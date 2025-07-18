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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;
import static org.sonatype.nexus.common.app.FeatureFlags.*;

@ExtendWith({SystemStubsExtension.class})
public class NexusPropertiesVerifierTest
    extends Test5Support
{
  private static final String NEXUS_ANALYTICS = "nexus.analytics.enabled";

  private static final String TRUE = Boolean.TRUE.toString();

  private static final String FALSE = Boolean.FALSE.toString();

  private final NexusPropertiesVerifier nexusPropertiesVerifier = new NexusPropertiesVerifier();

  private final Map<String, String> properties = new HashMap<>();

  @Mock
  private NexusProperties nexusProperties;

  @SystemStub
  private EnvironmentVariables environmentVariables;

  @BeforeEach
  public void setUp() {
    // Treat the nexusProperties like a simple map
    lenient().doAnswer(invocation -> properties.get(invocation.getArgument(0)))
        .when(nexusProperties)
        .getProperty(anyString());
    lenient().doAnswer(invocation -> properties.put(invocation.getArgument(0), invocation.getArgument(1)))
        .when(nexusProperties)
        .put(anyString(), anyString());
  }

  @AfterEach
  public void tearDown() {
    properties.clear();
  }

  @Test
  public void verifiesMissingRequiredProperties() {
    assertThrows(IllegalStateException.class, () -> nexusPropertiesVerifier.verify(nexusProperties));
  }

  @Test
  public void testVerifyRequiresProperties() {
    mockRequiredProperties();

    nexusPropertiesVerifier.verify(nexusProperties);
  }

  @Test
  public void testVerifyEnsuresAnalyticsEnabledForCommunityEdition() {
    mockRequiredProperties();
    nexusProperties.put(NexusEditionSelector.PROPERTY_KEY, NexusPropertiesVerifier.COMMUNITY);
    nexusProperties.put(NEXUS_ANALYTICS, FALSE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(NEXUS_ANALYTICS), is(TRUE));
  }

  @Test
  public void testVerifyLeavesAnalyticsDisabledForProEdition() {
    mockRequiredProperties();
    nexusProperties.put(NexusEditionSelector.PROPERTY_KEY, "PRO");
    nexusProperties.put(NEXUS_ANALYTICS, FALSE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(NEXUS_ANALYTICS), is(FALSE));
  }

  @Test
  public void testSelectDatastoreFeature_WhenClusteredEnabledOnEnvironment_SetsRelatedFeatures() {
    mockRequiredProperties();

    environmentVariables.set("DATASTORE_CLUSTERED_ENABLED", "true");

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(DATASTORE_CLUSTERED_ENABLED), is(TRUE));

    // datastore features
    assertThat(nexusProperties.getProperty(DATASTORE_TABLE_SEARCH), is(TRUE));
    assertThat(nexusProperties.getProperty(SQL_DISTRIBUTED_CACHE), is(TRUE));
    assertThat(nexusProperties.getProperty(DATASTORE_BLOBSTORE_METRICS), is(TRUE));
    assertThat(nexusProperties.getProperty(ELASTIC_SEARCH_ENABLED), is(FALSE));

    // authentication features
    assertThat(nexusProperties.getProperty(JWT_ENABLED), is(TRUE));
    assertThat(nexusProperties.getProperty(SESSION_ENABLED), is(FALSE));
  }

  @Test
  public void testSelectDatastoreFeature_WhenClusteredDisabledOnEnvironment_SetsRelatedFeatures() {
    mockRequiredProperties();
    environmentVariables.set("DATASTORE_CLUSTERED_ENABLED", FALSE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(CLUSTERED_ZERO_DOWNTIME_ENABLED), is(FALSE));
    assertThat(nexusProperties.getProperty(DATASTORE_CLUSTERED_ENABLED), is(FALSE));

    // set because DATASTORE_CLUSTERED_ENABLED is false and both SESSION_ENABLED and JWT_ENABLED are unset
    assertThat(nexusProperties.getProperty(SESSION_ENABLED), is(TRUE));
    assertThat(nexusProperties.getProperty(JWT_ENABLED), is(FALSE));

    // left unset
    assertThat(nexusProperties.getProperty(DATASTORE_TABLE_SEARCH), nullValue());
    assertThat(nexusProperties.getProperty(SQL_DISTRIBUTED_CACHE), nullValue());
    assertThat(nexusProperties.getProperty(DATASTORE_BLOBSTORE_METRICS), nullValue());
    assertThat(nexusProperties.getProperty(ELASTIC_SEARCH_ENABLED), nullValue());
  }

  @Test
  public void testSelectDatastoreFeature_WhenTableSearchEnabled_DisablesElasticSearch() {
    mockRequiredProperties();

    nexusProperties.put(DATASTORE_TABLE_SEARCH, TRUE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(ELASTIC_SEARCH_ENABLED), is(FALSE));
  }

  @Test
  public void testSelectDatastoreFeature_WhenElasticSearchEnabled_DisablesTableSearch() {
    mockRequiredProperties();
    nexusProperties.put(ELASTIC_SEARCH_ENABLED, TRUE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(DATASTORE_TABLE_SEARCH), is(FALSE));
  }

  @Test
  public void testSelectDatastoreFeature_CommunityEdition_EnforcesAnalyticsEnabled() {
    mockRequiredProperties();
    nexusProperties.put(NexusEditionSelector.PROPERTY_KEY, NexusPropertiesVerifier.COMMUNITY);
    nexusProperties.put(NEXUS_ANALYTICS, FALSE);

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.getProperty(NEXUS_ANALYTICS), is(TRUE));
  }

  @Test
  public void testSelectDatastoreFeature_AlwaysSetsDbFeatureAndDefaults() {
    mockRequiredProperties();

    when(nexusProperties.get(CHANGE_REPO_BLOBSTORE_TASK_ENABLED, "true")).thenReturn(TRUE);

    nexusPropertiesVerifier.verify(nexusProperties);

    verify(nexusProperties).get(CHANGE_REPO_BLOBSTORE_TASK_ENABLED, "true");

    assertThat(nexusProperties.getProperty(NexusPropertiesVerifier.DB_FEATURE_PROPERTY_KEY),
        is("nexus-datastore-mybatis"));
    assertThat(nexusProperties.getProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED), is(TRUE));
    assertThat(nexusProperties.getProperty("nexus.quartz.jobstore.jdbc"), is(TRUE));
  }

  public void mockRequiredProperties() {
    nexusProperties.put(BASEDIR_SYS_PROP, "/tmp/nexus");
    nexusProperties.put(NexusDirectoryConfiguration.DATADIR_SYS_PROP, "/tmp/nexus/data");
    nexusProperties.put(NexusPropertiesVerifier.DB_FEATURE_PROPERTY_KEY, "nexus-datastore-mybatis");
  }
}
