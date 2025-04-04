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
import java.nio.file.Path;
import java.util.Properties;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.bootstrap.edition.NexusEditionType.PRO;

/**
 * !!!! DEPRECATED in favor of {@link org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition}. The modules
 * containing the edition modules need to be in the bootstrap.entrypoint package so they are loaded in first round of
 * injection. This class should be removed when the previous DI architecture is removed. Until then changes should
 * primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public abstract class NexusEdition
{
  static final String NEXUS_EDITION = "nexus-edition";

  public static final String NEXUS_FEATURES = "nexus-features";

  private static final Logger log = LoggerFactory.getLogger(NexusEdition.class);

  public static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";

  public static final String NEXUS_LOAD_AS_CE_PROP_NAME = "nexus.loadAsCE";

  private static final String EDITION_PRO_PATH = "edition_pro";

  private final JavaPrefs javaPrefs;

  public abstract NexusEditionType getEdition();

  public abstract NexusEditionFeature getEditionFeature();

  protected NexusEdition(final JavaPrefs javaPrefs) {
    this.javaPrefs = javaPrefs;
  }

  /**
   * Determine whether or not we should be booting to the corresponding edition or not, based on the presence of a
   * edition marker file, license, or a System property that can be used to override the behaviour.
   */
  protected abstract boolean doesApply(final PropertyMap properties, final Path workDirPath);

  protected abstract void doApply(final PropertyMap properties, final Path workDirPath);

  public boolean applies(final PropertyMap properties, final Path workDirPath) {
    return doesApply(properties, workDirPath);
  }

  public void apply(final PropertyMap properties, final Path workDirPath) {
    doApply(properties, workDirPath);
  }

  protected boolean hasNexusLoadAs(final PropertyMap properties, final String nexusProperty) {
    return null != properties.get(nexusProperty);
  }

  public boolean isNexusLoadAs(final PropertyMap properties, final String nexusProperty) {
    return Boolean.parseBoolean(properties.get(nexusProperty));
  }

  public boolean hasFeature(final Properties properties, final String feature) {
    return properties.getProperty(NEXUS_FEATURES, "")
        .contains(feature);
  }

  protected boolean isNullNexusLicenseFile(final PropertyMap properties) {
    return properties.get("nexus.licenseFile") == null && System.getenv("NEXUS_LICENSE_FILE") == null;
  }

  protected boolean isNullJavaPrefLicensePath() {
    return !javaPrefs.isLicenseInstalled();
  }

  protected File getEditionMarker(final Path workDirPath, NexusEditionType edition) {
    if (PRO.equals(edition)) {
      return workDirPath.resolve(EDITION_PRO_PATH).toFile();
    }
    throw new IllegalStateException("Marker for Core edition not supported!");
  }

  protected void createEditionMarker(final Path workDirPath, NexusEditionType edition) {
    File editionMarker = getEditionMarker(workDirPath, edition);
    try {
      if (editionMarker.createNewFile()) {
        log.debug("Created {} edition marker file: {}", edition.name(), editionMarker);
      }
    }
    catch (IOException e) {
      log.error("Failed to create {}} edition marker file: {}", edition.name(), editionMarker, e);
    }
  }
}
