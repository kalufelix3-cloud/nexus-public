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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.LocalDate.now;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Default {@link IntegrityCheckStrategy} which checks name and SHA1 checksum
 *
 * @since 3.29
 */
@Component
@Qualifier(DefaultIntegrityCheckStrategy.DEFAULT_NAME)
@Singleton
public class DefaultIntegrityCheckStrategy
    extends ComponentSupport
    implements IntegrityCheckStrategy
{
  public static final String NAME_MISMATCH = "Name does not match on asset! Metadata name: '{}', Blob name: '{}'";

  public static final String DEFAULT_NAME = "default";

  static final String BLOB_PROPERTIES_MISSING_FOR_ASSET = "Blob properties missing for asset '{}'.";

  static final String BLOB_DATA_MISSING_FOR_ASSET = "Blob data missing for asset '{}'.";

  static final String BLOB_PROPERTIES_MARKED_AS_DELETED =
      "Blob properties marked as deleted for asset '{}'. Will be removed on next compact.";

  static final String SHA1_MISMATCH = "SHA1 does not match on asset '{}'! Metadata SHA1: '{}', Blob SHA1: '{}'";

  static final String ASSET_SHA1_MISSING = "Asset is missing SHA1 hash code";

  static final String BLOB_NAME_MISSING = "Blob properties is missing name";

  static final String ERROR_ACCESSING_BLOB = "Error accessing blob for asset '{}'";

  static final String ERROR_ACCESSING_BLOB_ATTRIBUTES = "Error accessing blob attributes for asset '{}' {} - {}";

  static final String ASSET_NAME_MISSING = "Asset is missing name";

  static final String ERROR_PROCESSING_ASSET = "Error processing asset '{}'";

  static final String ERROR_PROCESSING_ASSET_WITH_EX = ERROR_PROCESSING_ASSET + ". {} Exception: {}";

  static final String CANCEL_WARNING = "Cancelling blob integrity check";

  static final String BLOB_METRICS_MISSING_SHA1 = "Blob metrics are missing SHA1 hash code";

  static final String ASSET_INTEGRITY_CHECK_FAILED = "Asset integrity check failed for {}";

  public static final String ERROR_CHECKING_SHA_1_CHECKSUM_FOR = "Error checking sha1 checksum for '{}'";

  private final int browseBatchSize;

  @Inject
  public DefaultIntegrityCheckStrategy(
      @Value("${nexus.blobstore.restore.integrityCheck.batchSize:1000}") final int browseBatchSize)
  {
    checkArgument(browseBatchSize > 0);
    this.browseBatchSize = browseBatchSize;
  }

  @Override
  public void check(
      final Repository repository,
      final BlobStore blobStore,
      final BooleanSupplier isCancelled,
      final int sinceDays,
      @Nullable final Consumer<Asset> integrityCheckFailedHandler)
  {
    log.info("Checking integrity of assets in repository '{}' with blob store '{}'", repository.getName(),
        blobStore.getBlobStoreConfiguration().getName());

    long processed = 0;
    long failures = 0;

    LocalDate sinceDate = null;
    if (sinceDays > 0) {
      sinceDate = now().minusDays(sinceDays);
    }

    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      FluentAssets fluentAssets = repository.facet(ContentFacet.class).assets();
      Continuation<FluentAsset> assets = fluentAssets.browse(browseBatchSize, null);
      while (!assets.isEmpty()) {
        for (FluentAsset asset : assets) {
          if (isCancelled.getAsBoolean()) {
            log.warn(CANCEL_WARNING);
            return;
          }

          if (sinceDate != null) {
            LocalDate assetDate = asset.blob().get().blobCreated().toLocalDate();
            if (sinceDate.isAfter(assetDate)) {
              continue;
            }
          }

          log.debug("Checking asset {}", asset.path());
          boolean failed = checkAsset(asset, blobStore);

          if (failed) {
            failures++;
            if (integrityCheckFailedHandler != null) {
              integrityCheckFailedHandler.accept(asset);
            }
          }

          progressLogger
              .info("Elapsed time: {}, processed: {}, failed integrity check: {}", progressLogger.getElapsed(),
                  ++processed, failures);
        }

        assets = fluentAssets.browse(browseBatchSize, assets.nextContinuationToken());
      }
    }
  }

  private boolean checkAsset(final Asset asset, final BlobStore blobStore) {
    try {
      Optional<BlobId> blobId = asset.blob()
          .map(AssetBlob::blobRef)
          .map(BlobRef::getBlobId);

      if (blobId.isEmpty()) {
        log.error(ERROR_ACCESSING_BLOB, asset.path());
        return true;
      }
      else if (!blobAttributesExists(asset, blobStore, blobId.get())) {
        // If blob attributes exist but integrity check fails, we log and return true
        return true;
      }
      else if (!blobDataExists(blobStore, blobId.get())) {
        log.error(BLOB_DATA_MISSING_FOR_ASSET, asset.path());
        return true;
      }
      else {
        return false;
      }
    }
    catch (IllegalArgumentException e) {
      // thrown by checkAsset inner methods
      log.error(ERROR_PROCESSING_ASSET_WITH_EX, asset.toString(), e.getMessage(), log.isDebugEnabled() ? e : null);
      return true;
    }
    catch (Exception e) {
      log.error(ERROR_PROCESSING_ASSET, asset.toString(), e);
      return true;
    }
  }

  /**
   * Check the asset for integrity. By default checks name and SHA1.
   *
   * @param blobAttributes the {@link BlobAttributes} from the {@link Blob}
   * @param asset the {@link Asset}
   * @return true if asset integrity is intact, false otherwise
   */
  protected boolean checkAssetIntegrity(final BlobAttributes blobAttributes, final Asset asset) {
    checkArgument(blobAttributes.getProperties() != null, "Blob attributes are missing properties");

    return checkSha1(blobAttributes, asset) && checkName(blobAttributes, asset);
  }

  /**
   * returns true if the checksum matches, false otherwise
   */
  private boolean checkSha1(final BlobAttributes blobAttributes, final Asset asset) {
    try {
      String assetSha1 = getAssetSha1(asset);
      String blobSha1 = getBlobSha1(blobAttributes);

      if (!Objects.equals(assetSha1, blobSha1)) {
        log.error(SHA1_MISMATCH, asset.path(), assetSha1, blobSha1);
        return false;
      }
    }
    catch (IllegalArgumentException e) {
      log.error(ERROR_CHECKING_SHA_1_CHECKSUM_FOR, asset.path());
      return false;
    }
    catch (Exception e) {
      log.error(ERROR_PROCESSING_ASSET_WITH_EX, asset.path(), e.getMessage(), log.isDebugEnabled() ? e : null);
      return true;
    }

    return true;
  }

  /**
   * Get the SHA1 from the {@link BlobAttributes}
   */
  protected String getBlobSha1(final BlobAttributes blobAttributes) {
    BlobMetrics metrics = blobAttributes.getMetrics();
    checkArgument(metrics != null, "Blob attributes are missing metrics");
    String blobSha1 = metrics.getSha1Hash();
    checkArgument(blobSha1 != null, BLOB_METRICS_MISSING_SHA1);
    return blobSha1;
  }

  /**
   * Get the SHA1 from the {@link Asset}
   */
  protected String getAssetSha1(final Asset asset) {
    return asset.blob()
        .map(assetBlob -> assetBlob.checksums().get(SHA1.name()))
        .orElseThrow(() -> new IllegalArgumentException(ASSET_SHA1_MISSING));
  }

  /**
   * returns true if the name matches, false otherwise
   */
  private boolean checkName(final BlobAttributes blobAttributes, final Asset asset) {
    try {
      String blobName = getBlobName(blobAttributes, asset);
      String assetName = getAssetName(asset);

      checkArgument(blobName != null, BLOB_NAME_MISSING);
      checkArgument(assetName != null, ASSET_NAME_MISSING);

      if (assetName.startsWith("/") && !blobName.startsWith("/")) {
        assetName = assetName.substring(1);
      }

      if (!StringUtils.equals(assetName, blobName)) {
        log.error(NAME_MISMATCH, blobName, assetName);
        return false;
      }
    }
    catch (IllegalArgumentException e) {
      log.error(BLOB_NAME_MISSING);
      return false;
    }
    catch (Exception e) {
      log.error("Error checking blob name for asset '{}'", asset.path(), e);
      return true;
    }

    return true;
  }

  /**
   * returns true if the blobs data is accessible, false otherwise
   */
  protected boolean blobDataExists(final BlobStore blobStore, final BlobId blobId) {
    try {
      return blobStore.bytesExists(blobId);
    }
    catch (Exception e) {
      // Distinguish between infrastructure issues and legitimate blob non-existence
      if (isInfrastructureException(e)) {
        log.error("Infrastructure failure while checking blob existence for {}: {} - {}",
            blobId, e.getClass().getSimpleName(), e.getMessage());
        return true; // Treat as blob exists
      }
      else {
        log.error("Failed to check blob existence for {} cause {} - {}", blobId, e.getClass().getSimpleName(),
            log.isDebugEnabled() ? e : null);
        return true;
      }
    }
  }

  /**
   * returns true if the blobs data is accessible, false otherwise
   */
  protected boolean blobAttributesExists(final Asset asset, final BlobStore blobStore, final BlobId blobId) {
    try {
      BlobAttributes blobAttributes = blobStore.getBlobAttributesWithException(blobId);

      if (blobAttributes == null) {
        log.error(BLOB_PROPERTIES_MISSING_FOR_ASSET, asset.path());
        return false;
      }
      else if (blobAttributes.isDeleted()) {
        log.warn(BLOB_PROPERTIES_MARKED_AS_DELETED, asset.path());
        return false;
      }
      else if (!checkAssetIntegrity(blobAttributes, asset)) {
        log.error(ASSET_INTEGRITY_CHECK_FAILED, asset.path());
        return false;
      }
    }
    catch (Exception e) {
      log.warn(ERROR_ACCESSING_BLOB_ATTRIBUTES, asset.path(), e.getMessage(), log.isDebugEnabled() ? e : null);
      // Distinguish between infrastructure issues and legitimate blob non-existence
      if (isInfrastructureException(e)) {
        log.error("Infrastructure failure while checking attributes existence for {}: {} - {}",
            blobId, e.getClass().getSimpleName(), e.getMessage());
        return true; // Treat as blob exists
      }
      else {
        log.error("Failed to check attributes existence for {} cause {} - {}", blobId, e.getClass().getSimpleName(),
            log.isDebugEnabled() ? e : null);
        return true;
      }
    }

    return true;
  }

  /**
   * Determines if an exception indicates an infrastructure/connectivity issue rather than a legitimate "blob doesn't
   * exist" scenario
   */
  private boolean isInfrastructureException(Exception e) {
    String className = e.getClass().getName();

    // Network connectivity issues
    if (e instanceof ConnectException ||
        e instanceof SocketTimeoutException ||
        e instanceof UnknownHostException ||
        e instanceof NoRouteToHostException) {
      return true;
    }

    // AWS S3 specific infrastructure issues
    if (className.contains("AmazonS3Exception") || className.contains("SdkClientException")) {
      return true;
    }

    // Azure specific infrastructure issues
    if (className.contains("StorageException") && className.contains("azure")) {
      return true;
    }

    // Google Cloud specific infrastructure issues
    if (className.contains("StorageException") && className.contains("google")) {
      return true;
    }

    // Generic timeout and I/O issues - specific subtypes only
    return e instanceof TimeoutException ||
        e instanceof SocketException ||
        e instanceof InterruptedIOException ||
        e instanceof ClosedChannelException ||
        e instanceof ProtocolException;
  }

  /**
   * Get the name from the {@link Asset}
   */
  protected String getAssetName(final Asset asset) {
    return asset.path();
  }

  /**
   * Get the name from the {@link BlobAttributes}
   */
  protected String getBlobName(final BlobAttributes blobAttributes, final Asset asset) {
    return blobAttributes.getProperties().getProperty(HEADER_PREFIX + BLOB_NAME_HEADER);
  }
}
