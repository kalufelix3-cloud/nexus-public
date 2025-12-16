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
package org.sonatype.nexus.repository.apt.datastore.internal.cleanup;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.AptProperties;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.types.HostedType;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AptCleanupEventListener
    implements EventAware
{
  private static final Logger log = LoggerFactory.getLogger(AptCleanupEventListener.class);

  private static final int METADATA_UPDATE_DELAY_SECONDS = 2;

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

  private final ScheduledExecutorService executor;

  private final ConcurrentMap<String, Boolean> pendingUpdates;

  @Autowired
  public AptCleanupEventListener() {
    this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "apt-metadata-updater");
      t.setDaemon(true);
      return t;
    });
    this.pendingUpdates = new ConcurrentHashMap<>();
  }

  @PreDestroy
  public void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }
    catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentPurgedEvent event) {
    event.getRepository().ifPresent(repository -> {
      if (isAptHostedRepository(repository)) {
        scheduleMetadataUpdate(repository);
      }
    });
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    event.getRepository().ifPresent(repository -> {
      if (isAptHostedRepository(repository) && isDebAsset(event.getAsset())) {
        scheduleMetadataUpdate(repository);
      }
    });
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent event) {
    event.getRepository().ifPresent(repository -> {
      if (isAptHostedRepository(repository) && isDebAsset(event.getAsset())) {
        log.debug("AssetUpdatedEvent for APT repository {} - scheduling metadata rebuild", repository.getName());
        scheduleMetadataUpdate(repository);
      }
    });
  }

  private void scheduleMetadataUpdate(final Repository repository) {
    if (pendingUpdates.putIfAbsent(repository.getName(), Boolean.TRUE) == null) {
      executor.schedule(() -> {
        try {
          pendingUpdates.remove(repository.getName());
          updateAptMetadata(repository);
        }
        catch (Exception e) {
          log.error("Failed to update APT metadata for repository {}", repository.getName(), e);
        }
      }, METADATA_UPDATE_DELAY_SECONDS, TimeUnit.SECONDS);
    }
  }

  private void updateAptMetadata(final Repository repository) {
    if (!repository.getConfiguration().isOnline()) {
      return;
    }

    try {
      log.info("Rebuilding APT metadata for repository {}", repository.getName());

      AptKeyValueFacet dataFacet = repository.facet(AptKeyValueFacet.class);
      AptHostedMetadataFacet metadataFacet = repository.facet(AptHostedMetadataFacet.class);
      AptContentFacet contentFacet = repository.facet(AptContentFacet.class);

      dataFacet.removeAllPackageMetadata();

      for (FluentAsset asset : contentFacet.getAptPackageAssets()) {
        metadataFacet.addPackageMetadata(asset);
      }

      metadataFacet.removeInReleaseIndex();
      metadataFacet.rebuildMetadata(Collections.emptyList());
    }
    catch (Exception e) {
      log.error("Failed to rebuild APT metadata for repository {}", repository.getName(), e);
    }
  }

  private boolean isDebAsset(final Asset asset) {
    return AptProperties.DEB.equals(asset.kind());
  }

  private boolean isAptHostedRepository(final Repository repository) {
    return AptFormat.NAME.equals(repository.getFormat().getValue()) &&
        HostedType.NAME.equals(repository.getType().getValue());
  }
}
