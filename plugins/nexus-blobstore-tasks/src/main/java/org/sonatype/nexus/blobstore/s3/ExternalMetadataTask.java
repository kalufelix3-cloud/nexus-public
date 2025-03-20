/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.blobstore.s3;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * Retrieves S3's etag and last-modified headers.
 */
@Named
@TaskLogging(TASK_LOG_ONLY)
public class ExternalMetadataTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  public static final String FORMAT_FIELD_ID = "external.metadata.repository.format";

  private final BlobStoreManager blobStoreManager;

  private final AtomicInteger processed = new AtomicInteger();

  private final int concurrencyLimit;

  private final int queueCapacity;

  /**
   * @param blobStoreManager the blob store manager
   * @param concurrencyLimit the number of concurrent threads processing the queue allowed
   * @param queueCapacity the number of queued jobs allowed
   */
  @Inject
  public ExternalMetadataTask(
      final BlobStoreManager blobStoreManager,
      @Named("${external.metadata.repository.concurrencyLimit:-5}") final int concurrencyLimit,
      @Named("${external.metadata.repository.queueCapacity:-15}") final int queueCapacity)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);

    checkArgument(concurrencyLimit > 0, "external.metadata.repository.concurrencyLimit must be positive");
    checkArgument(queueCapacity > 0, "external.metadata.repository.queueCapacity must be positive");

    this.concurrencyLimit = concurrencyLimit;
    this.queueCapacity = queueCapacity;
  }

  @Override
  public String getMessage() {
    return String.format("retrieving remote blob headers for repositories '%s'", getRepositoryField());
  }

  @Override
  protected void execute(final Repository repository) {
    log.info("Processing {}", repository.getName());

    BlobStore blobstore = getBlobStore(repository).orElse(null);
    if (blobstore == null) {
      log.error("Unable to obtain blobstore for {}", repository);
      return;
    }
    final String repositoryName = repository.getName();
    // reset progress counter
    processed.set(0);

    AssetBlobStore<?> assetBlobStore =
        ((ContentFacetSupport) repository.facet(ContentFacet.class)).stores().assetBlobStore();

    ThreadPoolExecutor executor = new ThreadPoolExecutor(0, concurrencyLimit, 500L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(queueCapacity),
        new NexusThreadFactory("task-external-metadata", "task-external-metadata"), new CallerRunsPolicy());

    try (ProgressLogIntervalHelper progress = new ProgressLogIntervalHelper(log, 60)) {
      Continuations.streamOf(repository.facet(ContentFacet.class).assets()::browse)
          .filter(FluentAsset::hasBlob)
          .map(FluentAsset::blob)
          .map(Optional::get)
          .filter(assetBlob -> assetBlob.externalMetadata() == null)
          .forEach(assetBlob -> {
            // check cancellation before scheduling job so the primary thread throws an exception and stops queuing jobs
            CancelableHelper.checkCancellation();
            executor.submit(createJob(assetBlobStore, blobstore, assetBlob));
            progress.info("Processed {} assets in repository {}", processed.get(), repositoryName);
          });

      while (!executor.isShutdown() && (executor.getActiveCount() > 0 || !executor.getQueue().isEmpty())) {
        Runnable runnable = executor.getQueue().poll();
        if (runnable != null) {
          runnable.run();
        }
        else {
          Thread.sleep(500L);
        }
        progress.info("Processed {} assets in repository {}", processed.get(), repositoryName);
        CancelableHelper.checkCancellation();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CancelableHelper.checkCancellation();
      throw new RuntimeException(e);
    }
    finally {
      executor.shutdownNow();
    }
  }

  private Runnable createJob(
      final AssetBlobStore<?> assetBlobStore,
      final BlobStore blobstore,
      final AssetBlob assetBlob)
  {
    return () -> {
      // check cancellation to quickly drain the queue
      CancelableHelper.checkCancellation();
      log.debug("Processing {}", assetBlob);

      blobstore.getExternalMetadata(assetBlob.blobRef())
          .ifPresent(remoteMetadata -> assetBlobStore.setExternalMetadata(assetBlob, remoteMetadata));

      processed.addAndGet(1);
    };
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    if (!formatMatcher().test(repository)) {
      return false;
    }

    Optional<BlobStore> blobstore = getBlobStore(repository);

    if (blobstore.isEmpty()) {
      log.error("Unable to identify blobstore type for repository {}", repository);
      return false;
    }

    if (supportsMetadata(blobstore.get())) {
      return true;
    }
    else if (!isGroup(blobstore.get())) {
      return false;
    }

    return getMembers(blobstore.get()).stream()
        .map(blobStoreManager::get)
        .anyMatch(ExternalMetadataTask::supportsMetadata);
  }

  private Optional<BlobStore> getBlobStore(final Repository repository) {
    return Optional.ofNullable(repository.getConfiguration())
        .map(Configuration::getAttributes)
        .map(attr -> attr.get(STORAGE))
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(storage -> storage.get(BLOB_STORE_NAME))
        .filter(Objects::nonNull)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(blobStoreManager::get);
  }

  protected Predicate<Repository> formatMatcher() {
    String format = Optional.ofNullable(getConfiguration().getString(FORMAT_FIELD_ID))
        .map(String::trim)
        .orElse(null);
    if (Strings2.isBlank(format) || format.equals("*")) {
      return Predicates.alwaysTrue();
    }
    return repository -> repository.getFormat().getValue().equals(format);
  }

  private static boolean supportsMetadata(final BlobStore blobStore) {
    return "S3".equals(getType(blobStore));
  }

  private static boolean isGroup(final BlobStore blobStore) {
    return "Group".equals(getType(blobStore));
  }

  private static String getType(final BlobStore blobStore) {
    return blobStore.getBlobStoreConfiguration().getType();
  }

  @SuppressWarnings("unchecked")
  private static List<String> getMembers(final BlobStore blobstore) {
    return blobstore.getBlobStoreConfiguration().attributes(CONFIG_KEY).get(MEMBERS_KEY, List.class);
  }
}
