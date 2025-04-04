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

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.edition.NexusEdition.NEXUS_FEATURES;
import static org.sonatype.nexus.bootstrap.edition.NexusEditionFeature.PRO_FEATURE;

/**
 * !!!! DEPRECATED in favor of com.sonatype.nexus.bootstrap.entrypoint.edition.pro.ProfessionalNexusEditionTest.
 * This test class still needs to be created, validating the updated logic in the new class. This class should be
 * removed when the previous DI architecture is removed. Until then changes should primarily be done on the newer
 * "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class ProNexusEditionTest
{
  @TempDir
  private File workDir;

  private JavaPrefs javaPrefs;

  private ProNexusEdition underTest;

  @BeforeEach
  public void setup() throws IOException {
    javaPrefs = mock(JavaPrefs.class);
    when(javaPrefs.isLicenseInstalled()).thenReturn(true);
    underTest = new ProNexusEdition(javaPrefs);
  }

  @Test
  public void testApplies_AllProChecks() throws IOException {
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    propertyMap.put("nexus.licenseFile", "/some/value");
    createMarkerFile();
    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(true));
  }

  @Test
  public void testApplies_OnlyPropertyCheck() {
    when(javaPrefs.isLicenseInstalled()).thenReturn(false);
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    propertyMap.put("nexus.licenseFile", "/some/value");
    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(true));
  }

  @Test
  public void testApplies_OnlyJavaPrefsCheck() {
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(true));
  }

  @Test
  public void testApplies_NoMatch() {
    when(javaPrefs.isLicenseInstalled()).thenReturn(false);
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(false));
  }

  @Test
  public void testApplies_loadAsOssWins() throws IOException {
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    propertyMap.put("nexus.licenseFile", "/some/value");
    propertyMap.put("nexus.loadAsOSS", "true");
    createMarkerFile();

    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(false));
  }

  @Test
  public void testApplies_loadAsCeWins() throws IOException {
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put(NEXUS_FEATURES, PRO_FEATURE.featureString);
    propertyMap.put("nexus.licenseFile", "/some/value");
    propertyMap.put("nexus.loadAsCE", "true");
    createMarkerFile();

    boolean result = underTest.applies(propertyMap, workDir.toPath());

    assertThat(result, is(false));
  }

  private void createMarkerFile() throws IOException {
    File markerFile = workDir.toPath().resolve("edition_pro").toFile();
    if (!markerFile.createNewFile()) {
      throw new IOException("Failed to create marker file");
    }
  }
}
