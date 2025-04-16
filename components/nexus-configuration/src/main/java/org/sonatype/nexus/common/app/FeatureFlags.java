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
package org.sonatype.nexus.common.app;

/**
 * List of available feature flags
 * You can change it's values by editing ${data-dir}/nexus.properties configuration file.
 *
 * @since 3.20
 */
public class FeatureFlags
{
  /* Sisu/Guice completely removed */
  public static final String FEATURE_SPRING_ONLY = "nexus.spring.only";

  /* Go (hosted) repository is experimental. Available values: true, false. Default value: false */
  public static final String FEATURE_GOLANG_HOSTED = "nexus.golang.hosted";

  /* Cargo format is temporarily hidden behind the feature flag. Default value: false */
  public static final String CARGO_FORMAT_ENABLED = "nexus.format.cargo.enabled";

  /* Hugging Face format is temporarily hidden behind the feature flag. Default value: false */
  public static final String HUGGING_FACE_FORMAT_ENABLED = "nexus.format.huggingface.enabled";

  /* Composer format is temporarily hidden behind the feature flag. Default value: false */
  public static final String COMPOSER_FORMAT_ENABLED = "nexus.format.composer.enabled";

  /* Docker GC Custom task enabled. Available values: true, false. Default value: false */
  public static final String DOCKER_GC_CUSTOM_TASK_ENABLED = "nexus.docker.gc.custom.enabled";

  /* Database externalization developers only. Available values: true, false. Default value: false */
  public static final String DATASTORE_DEVELOPER = "nexus.datastore.developer";

  public static final String DATASTORE_DEVELOPER_NAMED = "${nexus.datastore.developer:-false}";

  /* Distributed event service. Available values: true, false. Default value: false */
  public static final String DATASTORE_CLUSTERED_ENABLED = "nexus.datastore.clustered.enabled";

  public static final String DATASTORE_CLUSTERED_ENABLED_NAMED = "${nexus.datastore.clustered.enabled:-false}";

  /* Zero downtime upgrades while clustered. Available values: true, false. Default value: false */
  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED = "nexus.zero.downtime.enabled";

  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED_NAMED = "${nexus.zero.downtime.enabled:-false}";

  public static final String CLUSTERED_ZERO_DOWNTIME_ENABLED_ENV = "NEXUS_ZERO_DOWNTIME_ENABLED";

  /* Feature flag to indicate if current db is postgresql */
  public static final String DATASTORE_IS_POSTGRESQL = "datastore.isPostgresql";

  // Enable elastic search
  public static final String ELASTIC_SEARCH_ENABLED = "nexus.elasticsearch.enabled";

  public static final String ELASTIC_SEARCH_ENABLED_NAMED = "${nexus.elasticsearch.enabled:-false}";

  /* JWT externalization. Available values: true, false. Default value: false */
  public static final String JWT_ENABLED = "nexus.jwt.enabled";

  /* Session flag for marking content that is only for session, and should be disabled when jwt is enabled */
  public static final String SESSION_ENABLED = "nexus.session.enabled";

  /* HTTP Replication. Available values: true, false. Default value: true */
  public static final String REPLICATION_HTTP_ENABLED = "nexus.replication.http.enabled";

  /*
   * flag for skipping blob store with soft-quota violation (for Round Robin group policy)
   * Available values: true, false. Default value: false
   */
  public static final String BLOBSTORE_SKIP_ON_SOFTQUOTA_VIOLATION = "nexus.blobstore.skipOnSoftQuotaViolation";

  /*  */
  public static final String DATASTORE_BLOBSTORE_METRICS = "nexus.datastore.blobstore.metrics.enabled";

  /**
   * Enable searching components via aggregated search table.
   * Mutual exclusive with:
   * - ELASTIC_SEARCH_ENABLED;
   */
  public static final String DATASTORE_TABLE_SEARCH = "nexus.datastore.table.search.enabled";

  public static final String DATASTORE_TABLE_SEARCH_NAMED = "${nexus.datastore.table.search.enabled:-false}";

  /**
   * The Key-Value DB storage which can be used as a distributed cache. Use it intelligently,
   * for example it makes sense to cache IQ results in a DB rather than request IQ Server each time.
   * At best should be replaced by Redis cache or Apache Ignite or other.
   */
  public static final String SQL_DISTRIBUTED_CACHE = "nexus.datastore.sql.cache.enabled";

  /**
   * Validates attribute from the node_heartbeat.node_info to determine if the deployment is valid.
   */
  public static final String DATASTORE_DEPLOYMENT_VALIDATOR = "nexus.datastore.deployment.validator.enabled";

  public static final String CHANGE_REPO_BLOBSTORE_TASK_ENABLED = "nexus.change.repo.blobstore.task.enabled";

  public static final String CHANGE_REPO_BLOBSTORE_TASK_ENABLED_NAMED =
      "${nexus.change.repo.blobstore.task.enabled:-false}";

  /**
   * Feature flag to enable/disable RecalculateBlobStoreSizeTask
   */
  public static final String RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED = "nexus.recalculate.blobstore.size.task.enabled";

  public static final String RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED_NAMED =
      "${" + RECALCULATE_BLOBSTORE_SIZE_TASK_ENABLED + ":-true}";

  public static final String FIREWALL_ONBOARDING_ENABLED = "nexus.firewall.onboarding.enabled";

  public static final String CLEANUP_PREVIEW_ENABLED = "nexus.cleanup.preview.enabled";

  public static final String CLEANUP_PREVIEW_ENABLED_NAMED = "${nexus.cleanup.preview.enabled:-true}";

  public static final String CLEANUP_MAVEN_RETAIN = "nexus.cleanup.mavenRetain";

  public static final String CLEANUP_DOCKER_RETAIN = "nexus.cleanup.dockerRetain";

  public static final String CLEANUP_USE_SQL = "nexus.cleanup.useSQL";

  public static final String FORMAT_RETAIN_PATTERN = "nexus.cleanup.{format}Retain";

  public static final String DISABLE_NORMALIZE_VERSION_TASK = "nexus.cleanup.disableNormalizeVersionTask";

  public static final String DISABLE_CREATING_COMPONENT_INDEXES_TASK = "nexus.component.index.task";

  public static final String FIREWALL_QUARANTINE_FIX_ENABLED = "nexus.firewall.quarantineFix.enabled";

  public static final String FIREWALL_QUARANTINE_FIX_ENABLED_NAMED = "${nexus.firewall.quarantineFix.enabled:-false}";

  public static final String REACT_PRIVILEGES = "nexus.react.privileges";

  public static final String REACT_PRIVILEGES_NAMED = "${nexus.react.privileges:-true}";

  public static final String REACT_PRIVILEGES_MODAL_ENABLED = "nexus.react.privileges.modal.enabled";

  public static final String REACT_PRIVILEGES_MODAL_NAMED = "${nexus.react.privileges.modal.enabled:-true}";

  /**
   * Feature flag to determine if we should include the repository sizes feature
   */
  public static final String REPOSITORY_SIZE_ENABLED = "nexus.repository.size";

  public static final String REPOSITORY_SIZE_ENABLED_NAMED = "${nexus.repository.size:-true}";

  public static final String CONTENT_USAGE_ENABLED_NAMED = "${nexus.contentUsageMetrics.enabled:-true}";

  public static final String REACT_ROLES_MODAL_ENABLED = "nexus.react.roles.modal.enabled";

  public static final String REACT_ROLES_MODAL_NAMED = "${nexus.react.roles.modal.enabled:-true}";

  public static final String BLOBSTORE_OWNERSHIP_CHECK_DISABLED_NAMED =
      "${nexus.blobstore.s3.ownership.check.disabled:-false}";

  public static final String STARTUP_TASKS_DELAY_SECONDS = "${nexus.startup.task.delay.seconds:-0}";

  /**
   * Feature flag to expose H2 export database to script task
   */
  public static final String H2_DATABASE_EXPORT_SCRIPT_TASK_ENABLED = "nexus.database.export.script.task.h2.enabled";

  public static final String H2_DATABASE_EXPORT_SCRIPT_TASK_ENABLED_NAMED =
      "${nexus.database.export.script.task.h2.enabled:-true}";

  /* When false skips the orient not supported error. Available values: true, false. Default value: true */
  public static final String ORIENT_WARNING = "nexus.orient.warning";

  public static final String ORIENT_WARNING_NAMED = "${nexus.orient.warning:-true}";

  /**
   * When true (default), the Secure attribute will be set on the NXSESSIONID Cookie when delivered over https.
   * In deployments with HTTP-only listeners, this setting will typically have no effect.
   * Setting false for this property in HTTPS only environments is not recommended.
   *
   * See https://owasp.org/www-community/controls/SecureCookieAttribute
   */
  public static final String NXSESSIONID_SECURE_COOKIE_NAMED = "${nexus.session.secureCookie:-true}";

  public static final String ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_NAMED =
      "${nexus.audit.attribute.changes.enabled:-true}";

  public static final String ZERO_DOWNTIME_MARKETING_MODAL_ENABLED = "zero.downtime.marketing.modal";

  public static final String ZERO_DOWNTIME_MARKETING_MODAL_ENABLED_NAMED = "${zero.downtime.marketing.modal:-false}";

  /* For testing purposes only */
  public static final String ZERO_DOWNTIME_BASELINE_FAIL = "nexus.zdu.baseline.fail";

  /* For testing purposes only */
  public static final String ZERO_DOWNTIME_FUTURE_MIGRATION_ENABLED = "nexus.zdu.future.enabled";

  public static final String MALWARE_RISK_ENABLED = "nexus.malware.risk.enabled";

  public static final String MALWARE_RISK_ENABLED_NAMED = "${nexus.malware.risk.enabled:-true}";

  public static final String MALWARE_RISK_ON_DISK_ENABLED = "nexus.malware.risk.on.disk.enabled";

  public static final String MALWARE_RISK_ON_DISK_ENABLED_NAMED = "${nexus.malware.risk.on.disk.enabled:-true}";

  public static final String MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED =
      "nexus.malware.risk.on.disk.nonadmin.override.enabled";

  public static final String MALWARE_RISK_ON_DISK_NONADMIN_OVERRIDE_ENABLED_NAMED =
      "${nexus.malware.risk.on.disk.nonadmin.override.enabled:-false}";

  public static final String MALWARE_REMEDIATOR_TASK_CHECK_REPOSITORY_IN_KNOWN_REGISTRIES_NAMED =
      "${nexus.malware.remediator.task.check.repository.in.known.registries:-true}";

  public static final String MALWARE_REMEDIATOR_TASK_IGNORE_QUARANTINE_STATE_NAMED =
      "${nexus.malware.remediator.task.ignore.quarantine.state:-true}";

  /* properties/env vars used by secrets service */
  public static final String SECRETS_FILE = "nexus.secrets.file";

  public static final String SECRETS_FILE_ENV = "NEXUS_SECRETS_KEY_FILE";

  public static final String RECONCILE_CLEANUP_DAYS_AGO = "${nexus.reconcile.cleanup.daysAgo:-7}";

  public static final String RECONCILE_EXECUTE_CREATED_HOURS_AGO = "${nexus.reconcile.execute.plans.created:-2h}";

  public static final String SECRETS_API_ENABLED = "nexus.secrets.api.enabled";

  public static final String NEXUS_SECURITY_OAUTH2_ENABLED = "nexus.security.oauth2.enabled";
}
