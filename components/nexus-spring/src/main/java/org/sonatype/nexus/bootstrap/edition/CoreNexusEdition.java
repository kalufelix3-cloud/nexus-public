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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.JavaPrefs;
import org.sonatype.nexus.spring.application.PropertyMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CoreNexusEdition
    extends NexusEdition
{
  private static final Logger log = LoggerFactory.getLogger(CoreNexusEdition.class);

  @Inject
  public CoreNexusEdition(final JavaPrefs javaPrefs) {
    super(javaPrefs);
  }

  @Override
  public NexusEditionType getEdition() {
    return NexusEditionType.CORE;
  }

  @Override
  public NexusEditionFeature getEditionFeature() {
    return NexusEditionFeature.CORE_FEATURE;
  }

  @Override
  protected boolean doesApply(final PropertyMap properties, final Path workDirPath) {
    // If this method is executed there is no need to validate anything, the other
    // nexus edition classes (Pro and Starter) already did all the checks needed.
    return true;
  }

  @Override
  protected void doApply(final PropertyMap properties, final Path workDirPath) {
    log.info("Loading Sonatype Nexus Repository Core Edition");
    properties.put(NEXUS_EDITION, NexusEditionType.CORE.editionString);
    String updatedNexusFeaturesProps = properties.get(NEXUS_FEATURES)
        .replace(NexusEditionFeature.PRO_FEATURE.featureString, getEditionFeature().featureString);

    properties.put(NEXUS_FEATURES, updatedNexusFeaturesProps);
  }
}
