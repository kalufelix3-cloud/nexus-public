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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.sonatype.nexus.spring.application.PropertyMap;

import com.google.common.annotations.VisibleForTesting;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.nexus.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;
import static org.sonatype.nexus.NexusDirectoryConfiguration.DATADIR_SYS_PROP;
import static org.sonatype.nexus.NexusDirectoryConfiguration.getDataPath;
import static org.sonatype.nexus.common.app.FeatureFlags.*;

public class NexusEditionPropertiesConfigurer
{
  static final String NEXUS_EDITION = "nexus-edition";

  static final String NEXUS_DB_FEATURE = "nexus-db-feature";

  private static final String NEXUS_EXCLUDE_FEATURES = "nexus-exclude-features";

  private static final String TRUE = Boolean.TRUE.toString();

  private static final String FALSE = Boolean.FALSE.toString();

  public void applyPropertiesFromConfiguration(final PropertyMap nexusProperties) throws IOException {
    nexusProperties.putAll(System.getProperties());

    // Ensure required properties exist
    requireProperty(nexusProperties, BASEDIR_SYS_PROP);
    requireProperty(nexusProperties, DATADIR_SYS_PROP);

    Path workDirPath = getDataPath();
    // DirectoryHelper.mkdir(workDirPath);

    NexusEditionFactory.selectActiveEdition(nexusProperties, workDirPath);

    selectDatastoreFeature(nexusProperties);
    selectAuthenticationFeature(nexusProperties);
    readEnvironmentVariables(nexusProperties);

    requireProperty(nexusProperties, NEXUS_EDITION);
    requireProperty(nexusProperties, NEXUS_DB_FEATURE);
    ensureHACIsDisabled();
  }

  private void readEnvironmentVariables(final PropertyMap properties) {

    if (properties.get(CHANGE_REPO_BLOBSTORE_TASK_ENABLED) == null) {
      properties.put(
          CHANGE_REPO_BLOBSTORE_TASK_ENABLED,
          Boolean.toString(parseBoolean(System.getenv("CHANGE_REPO_BLOBSTORE_TASK_ENABLED"))));
    }

    if (properties.get(FIREWALL_QUARANTINE_FIX_ENABLED) == null) {
      properties.put(
          FIREWALL_QUARANTINE_FIX_ENABLED,
          Boolean.toString(parseBoolean(System.getenv("FIREWALL_QUARANTINE_FIX_ENABLED"))));
    }

    // Used by ZDU ITs to simulate migration failures
    if (properties.get(ZERO_DOWNTIME_BASELINE_FAIL) == null) {
      Optional.ofNullable(System.getenv("NEXUS_ZDU_BASELINE_FAIL"))
          .ifPresent(v -> properties.put(ZERO_DOWNTIME_BASELINE_FAIL, v));
    }

    // Used by ZDU ITs to simulate behavior when future migrations are available
    if (properties.get(ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED) == null) {
      Optional.ofNullable(System.getenv("NEXUS_ZDU_FUTURE_MIGRATION_ENABLED"))
          .ifPresent(v -> properties.put(ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED, v));
    }

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
