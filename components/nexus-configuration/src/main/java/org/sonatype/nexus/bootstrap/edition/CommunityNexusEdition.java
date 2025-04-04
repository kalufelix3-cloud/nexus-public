
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

import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * !!!! DEPRECATED in favor of com.sonatype.nexus.bootstrap.entrypoint.edition.community.CommunityNexusEdition.
 * The modules containing the edition modules need to be in the bootstrap.entrypoint package so they are loaded in
 * first round of injection. This class should be removed when the previous DI architecture is removed. Until then
 * changes should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to this class
 * if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "false", matchIfMissing = true)
public class CommunityNexusEdition
    extends NexusEdition
{
  private static final Logger log = LoggerFactory.getLogger(CommunityNexusEdition.class);

  @Inject
  public CommunityNexusEdition(final JavaPrefs javaPrefs) {
    super(javaPrefs);
  }

  @Override
  public NexusEditionType getEdition() {
    return NexusEditionType.COMMUNITY;
  }

  @Override
  public NexusEditionFeature getEditionFeature() {
    return NexusEditionFeature.COMMUNITY_FEATURE;
  }

  @Override
  protected boolean doesApply(final PropertyMap properties, final Path workDirPath) {
    // only matches if instance is either PRO or COMMUNITY configured. Or one of the loadAs properties is set. Note that
    // at this point the doesApply check on ProNexusEdition has failed, or we wouldn't be here, so no need to check
    // license again
    return (properties.get(NEXUS_FEATURES, "").contains(NexusEditionFeature.PRO_FEATURE.featureString) ||
        properties.get(NEXUS_FEATURES, "").contains(NexusEditionFeature.COMMUNITY_FEATURE.featureString)) ||
        isNexusLoadAs(properties, NEXUS_LOAD_AS_CE_PROP_NAME) ||
        isNexusLoadAs(properties, NEXUS_LOAD_AS_OSS_PROP_NAME);
  }

  @Override
  protected void doApply(final PropertyMap properties, final Path workDirPath) {
    log.info("Loading Community Edition");
    properties.put(NEXUS_EDITION, NexusEditionType.COMMUNITY.editionString);
    String updatedNexusFeaturesProps = properties.get(NEXUS_FEATURES)
        .replace(
            NexusEditionFeature.PRO_FEATURE.featureString,
            getEditionFeature().featureString);
    properties.put(NEXUS_FEATURES, updatedNexusFeaturesProps);
  }
}
