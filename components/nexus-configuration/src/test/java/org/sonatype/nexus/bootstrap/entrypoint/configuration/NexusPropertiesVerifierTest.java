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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;

public class NexusPropertiesVerifierTest
    extends TestSupport
{
  private static final String NEXUS_ANALYTICS = "nexus.analytics.enabled";

  private final NexusPropertiesVerifier nexusPropertiesVerifier = new NexusPropertiesVerifier();

  private final Map<String, String> properties = new HashMap<>();

  @Mock
  private NexusProperties nexusProperties;

  @Before
  public void setUp() {
    // Treat the nexusProperties like a simple map
    doAnswer(invocation -> properties.get((String) invocation.getArgument(0)))
        .when(nexusProperties)
        .get(anyString());
    doAnswer(invocation -> properties.put(invocation.getArgument(0), invocation.getArgument(1)))
        .when(nexusProperties)
        .put(anyString(), anyString());
  }

  @After
  public void tearDown() {
    properties.clear();
  }

  @Test(expected = IllegalStateException.class)
  public void verifiesMissingRequiredProperties() {
    nexusPropertiesVerifier.verify(nexusProperties);
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
    nexusProperties.put(NEXUS_ANALYTICS, Boolean.FALSE.toString());

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.get(NEXUS_ANALYTICS), is(Boolean.TRUE.toString()));
  }

  @Test
  public void testVerifyLeavesAnalyticsDisabledForProEdition() {
    mockRequiredProperties();
    nexusProperties.put(NexusEditionSelector.PROPERTY_KEY, "PRO");
    nexusProperties.put(NEXUS_ANALYTICS, Boolean.FALSE.toString());

    nexusPropertiesVerifier.verify(nexusProperties);

    assertThat(nexusProperties.get(NEXUS_ANALYTICS), is(Boolean.FALSE.toString()));
  }

  public void mockRequiredProperties() {
    nexusProperties.put(BASEDIR_SYS_PROP, "/tmp/nexus");
    nexusProperties.put(NexusDirectoryConfiguration.DATADIR_SYS_PROP, "/tmp/nexus/data");
    nexusProperties.put(NexusPropertiesVerifier.DB_FEATURE_PROPERTY_KEY, "nexus-datastore-mybatis");
  }
}
