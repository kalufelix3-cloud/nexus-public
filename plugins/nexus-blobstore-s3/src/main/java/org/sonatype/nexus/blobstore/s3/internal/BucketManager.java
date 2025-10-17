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
package org.sonatype.nexus.blobstore.s3.internal;

import java.util.Collection;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.StorageLocationManager;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper.getConfiguredBucket;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.ACCESS_DENIED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.INVALID_ACCESS_KEY_ID_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.METHOD_NOT_ALLOWED_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.NO_SUCH_BUCKET_POLICY_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.SIGNATURE_DOES_NOT_MATCH_CODE;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.bucketOwnershipError;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.buildException;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.insufficientCreatePermissionsError;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.invalidIdentityError;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStoreException.unexpectedError;

/**
 * Creates and deletes buckets for the {@link S3BlobStore}.
 *
 * @since 3.16
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BucketManager
    extends ComponentSupport
    implements StorageLocationManager
{
  static final String OLD_LIFECYCLE_EXPIRATION_RULE_ID = "Expire soft-deleted blobstore objects";

  static final String LIFECYCLE_EXPIRATION_RULE_ID_PREFIX = "Expire soft-deleted objects in blobstore ";

  private EncryptingS3Client s3;

  private final BucketOwnershipCheckFeatureFlag ownershipCheckFeatureFlag;

  private final Collection<BucketOperations> bucketOperations;

  @Autowired
  public BucketManager(
      final BucketOwnershipCheckFeatureFlag featureFlag,
      final Collection<BucketOperations> bucketOperations)
  {
    this.ownershipCheckFeatureFlag = checkNotNull(featureFlag);
    this.bucketOperations = checkNotNull(bucketOperations);
  }

  public void setS3(final EncryptingS3Client s3) {
    this.s3 = s3;
  }

  @Override
  public void prepareStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    checkPermissions(getConfiguredBucket(blobStoreConfiguration));
    if (!s3.doesBucketExist(bucket)) {
      try {
        s3.createBucket(bucket);
      }
      catch (S3Exception e) {
        if (ACCESS_DENIED_CODE.equals(e.awsErrorDetails().errorCode())) {
          log.error("Error creating bucket {}", bucket, e);
          throw insufficientCreatePermissionsError();
        }
        log.info("Error creating bucket {}", bucket, e);
        throw unexpectedError("creating bucket");
      }
    }
  }

  @Override
  public void deleteStorageLocation(final BlobStoreConfiguration blobStoreConfiguration) {
    String bucket = getConfiguredBucket(blobStoreConfiguration);
    ListObjectsV2Response listing = s3.listObjectsV2(bucket, "");
    if (listing.contents().isEmpty()) {
      s3.deleteBucket(bucket);
    }
    else {
      log.info("Not removing S3 bucket {} because it is not empty", bucket);
      bucketOperations.forEach(operation -> {
        operation.delete(blobStoreConfiguration.getName(), bucket, s3);
      });
    }
  }

  private void checkPermissions(final String bucket) {
    checkCredentials(bucket);
    if (!ownershipCheckFeatureFlag.isDisabled() && s3.doesBucketExist(bucket)) {
      checkBucketOwner(bucket);
    }
  }

  private void checkCredentials(final String bucket) {
    try {
      s3.doesBucketExist(bucket);
    }
    catch (S3Exception e) {
      if (INVALID_ACCESS_KEY_ID_CODE.equals(e.awsErrorDetails().errorCode()) ||
          SIGNATURE_DOES_NOT_MATCH_CODE.equals(e.awsErrorDetails().errorCode())) {
        log.error("Exception thrown checking AWS credentials with error {}", e.getMessage(), e);
        throw buildException(e);
      }
      log.info("Exception thrown checking AWS credentials.", e);
      throw unexpectedError("checking credentials");
    }
  }

  private void checkBucketOwner(final String bucket) {
    try {
      s3.getBucketPolicy(bucket);
    }
    catch (S3Exception e) {
      String errorCode = e.awsErrorDetails().errorCode();
      String logMessage = String.format("Exception thrown checking ownership of \"%s\" bucket.", bucket);
      if (ACCESS_DENIED_CODE.equals(errorCode)) {
        log.debug(logMessage, e);
        throw bucketOwnershipError();
      }
      if (METHOD_NOT_ALLOWED_CODE.equals(errorCode)) {
        log.debug(logMessage, e);
        throw invalidIdentityError();
      }
      if (NO_SUCH_BUCKET_POLICY_CODE.equals(errorCode)) {
        // AWS SDK 2.x: NoSuchBucketPolicy means bucket has default permissions - this is normal and valid
        log.debug("Bucket \"{}\" has no bucket policy (using default permissions) - ownership check passed", bucket);
        return;
      }
      log.info(logMessage, log.isDebugEnabled() ? e : e.getMessage());
      throw unexpectedError("checking bucket ownership");
    }
  }
}
