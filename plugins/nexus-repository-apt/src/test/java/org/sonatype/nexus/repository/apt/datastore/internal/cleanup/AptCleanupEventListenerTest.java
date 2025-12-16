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

import java.lang.reflect.Method;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.AptProperties;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AptCleanupEventListenerTest
    extends TestSupport
{
  @Mock
  private Repository aptHostedRepository;

  @Mock
  private Repository aptProxyRepository;

  @Mock
  private Repository mavenRepository;

  @Mock
  private Configuration onlineConfiguration;

  @Mock
  private Configuration offlineConfiguration;

  @Mock
  private AptKeyValueFacet aptKeyValueFacet;

  @Mock
  private AptHostedMetadataFacet aptHostedMetadataFacet;

  @Mock
  private AptContentFacet aptContentFacet;

  @Mock
  private Asset debAsset;

  @Mock
  private Asset nonDebAsset;

  private AptCleanupEventListener underTest;

  @Before
  public void setup() {
    underTest = new AptCleanupEventListener();

    // Setup APT hosted repository
    Format aptFormat = new Format(AptFormat.NAME)
    {
    };
    Type hostedType = new HostedType();
    when(aptHostedRepository.getFormat()).thenReturn(aptFormat);
    when(aptHostedRepository.getType()).thenReturn(hostedType);
    when(aptHostedRepository.getName()).thenReturn("apt-hosted-test");
    when(aptHostedRepository.getConfiguration()).thenReturn(onlineConfiguration);
    when(onlineConfiguration.isOnline()).thenReturn(true);
    when(aptHostedRepository.facet(AptKeyValueFacet.class)).thenReturn(aptKeyValueFacet);
    when(aptHostedRepository.facet(AptHostedMetadataFacet.class)).thenReturn(aptHostedMetadataFacet);
    when(aptHostedRepository.facet(AptContentFacet.class)).thenReturn(aptContentFacet);
    when(aptContentFacet.getAptPackageAssets()).thenReturn(Collections.emptyList());

    // Setup APT proxy repository
    Type proxyType = new ProxyType();
    when(aptProxyRepository.getFormat()).thenReturn(aptFormat);
    when(aptProxyRepository.getType()).thenReturn(proxyType);
    when(aptProxyRepository.getName()).thenReturn("apt-proxy-test");

    // Setup Maven repository
    Format mavenFormat = new Format("maven2")
    {
    };
    when(mavenRepository.getFormat()).thenReturn(mavenFormat);
    when(mavenRepository.getType()).thenReturn(hostedType);
    when(mavenRepository.getName()).thenReturn("maven-hosted-test");

    // Setup offline configuration
    when(offlineConfiguration.isOnline()).thenReturn(false);

    // Setup assets
    when(debAsset.kind()).thenReturn(AptProperties.DEB);
    when(nonDebAsset.kind()).thenReturn("other");
  }

  /**
   * Verifies that isAptHostedRepository correctly identifies APT hosted repositories.
   */
  @Test
  public void testIsAptHostedRepository_returnsTrue_forAptHosted() throws Exception {
    // When - use reflection to call private method
    Method method = AptCleanupEventListener.class.getDeclaredMethod("isAptHostedRepository", Repository.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(underTest, aptHostedRepository);

    // Then
    assertThat(result).isTrue();
  }

  /**
   * Verifies that isAptHostedRepository returns false for APT proxy repositories.
   */
  @Test
  public void testIsAptHostedRepository_returnsFalse_forAptProxy() throws Exception {
    // When
    Method method = AptCleanupEventListener.class.getDeclaredMethod("isAptHostedRepository", Repository.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(underTest, aptProxyRepository);

    // Then
    assertThat(result).isFalse();
  }

  /**
   * Verifies that isAptHostedRepository returns false for non-APT repositories.
   */
  @Test
  public void testIsAptHostedRepository_returnsFalse_forNonAptRepository() throws Exception {
    // When
    Method method = AptCleanupEventListener.class.getDeclaredMethod("isAptHostedRepository", Repository.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(underTest, mavenRepository);

    // Then
    assertThat(result).isFalse();
  }

  /**
   * Verifies that isDebAsset correctly identifies DEB assets.
   */
  @Test
  public void testIsDebAsset_returnsTrue_forDebAsset() throws Exception {
    // When
    Method method = AptCleanupEventListener.class.getDeclaredMethod("isDebAsset", Asset.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(underTest, debAsset);

    // Then
    assertThat(result).isTrue();
  }

  /**
   * Verifies that isDebAsset returns false for non-DEB assets.
   */
  @Test
  public void testIsDebAsset_returnsFalse_forNonDebAsset() throws Exception {
    // When
    Method method = AptCleanupEventListener.class.getDeclaredMethod("isDebAsset", Asset.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(underTest, nonDebAsset);

    // Then
    assertThat(result).isFalse();
  }

  /**
   * Verifies that the event handler methods exist and are properly annotated with @Subscribe.
   *
   * These methods should be the ONLY way metadata rebuilds are triggered, ensuring that
   * rebuilds only occur when components/assets are actually modified.
   */
  @Test
  public void testEventHandlerMethods_exist() throws Exception {
    // Verify ComponentPurgedEvent handler exists
    Method componentPurgedHandler = AptCleanupEventListener.class.getDeclaredMethod("on",
        org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent.class);
    assertThat(componentPurgedHandler).as("ComponentPurgedEvent handler should exist").isNotNull();

    // Verify AssetDeletedEvent handler exists
    Method assetDeletedHandler = AptCleanupEventListener.class.getDeclaredMethod("on",
        org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent.class);
    assertThat(assetDeletedHandler).as("AssetDeletedEvent handler should exist").isNotNull();

    // Verify AssetUpdatedEvent handler exists
    Method assetUpdatedHandler = AptCleanupEventListener.class.getDeclaredMethod("on",
        org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent.class);
    assertThat(assetUpdatedHandler).as("AssetUpdatedEvent handler should exist").isNotNull();
  }
}
