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
package org.sonatype.nexus.bootstrap.edition;

import java.nio.file.Path;
import java.util.Optional;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;

import com.google.common.annotations.VisibleForTesting;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.nexus.common.app.FeatureFlags.*;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.DATADIR_SYS_PROP;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.getDataPath;

/**
 * !!!! DEPRECATED in favor of {@link org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusPropertiesVerifier}.
 * This class and another were merge and attempted to be streamlined on how each property is handled. This class
 * should be removed when the previous DI architecture is removed. Until then changes should primarily be done on
 * the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class NexusEditionPropertiesConfigurer
{
  static final String NEXUS_EDITION = "nexus-edition";

  static final String NEXUS_DB_FEATURE = "nexus-db-feature";

  private static final String NEXUS_EXCLUDE_FEATURES = "nexus-exclude-features";

  private static final String TRUE = Boolean.TRUE.toString();

  private static final String FALSE = Boolean.FALSE.toString();

  public void applyPropertiesFromConfiguration(final PropertyMap nexusProperties) {
    nexusProperties.putAll(System.getProperties());

    // Ensure required properties exist
    requireProperty(nexusProperties, BASEDIR_SYS_PROP);
    requireProperty(nexusProperties, DATADIR_SYS_PROP);

    Path workDirPath = getDataPath();

    NexusEditionFactory.selectActiveEdition(nexusProperties, workDirPath);

    selectDatastoreFeature(nexusProperties);
    selectAuthenticationFeature(nexusProperties);
    readEnvironmentVariables(nexusProperties);

    requireProperty(nexusProperties, NEXUS_EDITION);
    requireProperty(nexusProperties, NEXUS_DB_FEATURE);
    ensureHACIsDisabled();
  }

  private void readEnvironmentVariables(final PropertyMap properties) {
    properties.computeIfAbsent(
        CHANGE_REPO_BLOBSTORE_TASK_ENABLED,
        k -> Boolean.toString(parseBoolean(System.getenv("CHANGE_REPO_BLOBSTORE_TASK_ENABLED"))));
    properties.computeIfAbsent(
        FIREWALL_QUARANTINE_FIX_ENABLED,
        k -> Boolean.toString(parseBoolean(System.getenv("FIREWALL_QUARANTINE_FIX_ENABLED"))));
    properties.computeIfAbsent(
        ZERO_DOWNTIME_BASELINE_FAIL,
        k -> Boolean.toString(parseBoolean(System.getenv("NEXUS_ZDU_BASELINE_FAIL"))));
    properties.computeIfAbsent(
        ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED,
        k -> Boolean.toString(parseBoolean(System.getenv("NEXUS_ZDU_FUTURE_MIGRATION_ENABLED"))));
    // Env variable for secrets encryption
    Optional.ofNullable(System.getenv(SECRETS_FILE_ENV))
        .ifPresent(secretsFilePath -> properties.put(SECRETS_FILE, secretsFilePath));
  }

  private void selectDatastoreFeature(final PropertyMap properties) {
    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.get(
        DATASTORE_CLUSTERED_ENABLED,
        Optional.ofNullable(System.getenv("DATASTORE_CLUSTERED_ENABLED")).orElse(FALSE)))) {
      // As we read the ENV variable we need to enable feature flagged classes using in-memory properties hashtable
      properties.put(DATASTORE_CLUSTERED_ENABLED, TRUE);
      properties.put(DATASTORE_TABLE_SEARCH, TRUE);
      properties.put(ELASTIC_SEARCH_ENABLED, FALSE);
      properties.put(SQL_DISTRIBUTED_CACHE, TRUE);

      // JWT and Blobstore Metrics should also be enabled for clustered
      properties.put(JWT_ENABLED, TRUE);
      properties.put(DATASTORE_BLOBSTORE_METRICS, TRUE);

      // Enable zero downtime based on property and flag
      String zduEnabled = Optional.ofNullable(System.getenv(CLUSTERED_ZERO_DOWNTIME_ENABLED_ENV))
          .orElse(properties.get(CLUSTERED_ZERO_DOWNTIME_ENABLED, FALSE));
      properties.put(CLUSTERED_ZERO_DOWNTIME_ENABLED, zduEnabled);
    }
    else {
      // Set default of clustered upgrades off
      properties.put(CLUSTERED_ZERO_DOWNTIME_ENABLED, FALSE);
    }

    // datastore search mode enables datastore user mode
    // disables elastic search mode
    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.get(DATASTORE_TABLE_SEARCH, FALSE))) {
      properties.put(ELASTIC_SEARCH_ENABLED, FALSE);
    }

    // elastic search disables datastore search mode
    if (parseBoolean(properties.get(ELASTIC_SEARCH_ENABLED, FALSE))) {
      properties.put(DATASTORE_TABLE_SEARCH, FALSE);
    }

    // datastore mode, but not developer mode
    if (!parseBoolean(properties.get(DATASTORE_DEVELOPER, FALSE))) {
      // exclude unfinished format features
      properties.put(NEXUS_EXCLUDE_FEATURES, properties.get(NEXUS_EXCLUDE_FEATURES, ""));
    }

    selectDbFeature(properties);
  }

  private void selectDbFeature(final PropertyMap properties) {
    properties.put(NEXUS_DB_FEATURE, "nexus-datastore-mybatis");
    // enable change blobstore task for only for newdb
    properties.put(CHANGE_REPO_BLOBSTORE_TASK_ENABLED, TRUE);
    properties.put("nexus.quartz.jobstore.jdbc", TRUE);
  }

  private void selectAuthenticationFeature(final PropertyMap properties) {
    if (parseBoolean(properties.get(SESSION_ENABLED, TRUE))) {
      properties.put(SESSION_ENABLED, TRUE);
    }
    if (parseBoolean(properties.get(JWT_ENABLED, FALSE))) {
      properties.put(SESSION_ENABLED, FALSE);
    }
    else {
      // If JWT is not enabled, then disable OAuth2 as well
      properties.put(NEXUS_SECURITY_OAUTH2_ENABLED, FALSE);
    }
  }

  private void requireProperty(final PropertyMap properties, final String name) {
    if (!properties.containsKey(name)) {
      throw new IllegalStateException("Missing required property: " + name);
    }
  }

  @VisibleForTesting
  void ensureHACIsDisabled() {
    if (Boolean.getBoolean("nexus.clustered") || parseBoolean(System.getenv("NEXUS_CLUSTERED"))) {
      throw new IllegalStateException(
          "High Availability Clustering (HA-C) is a legacy feature and is no longer supported");
    }
  }
}
