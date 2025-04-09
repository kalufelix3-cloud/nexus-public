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
package org.sonatype.nexus.bootstrap.entrypoint.edition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static java.util.Locale.ENGLISH;
import static org.sonatype.nexus.common.app.FeatureFlags.FEATURE_SPRING_ONLY;

@Singleton
@Named
@ConditionalOnProperty(value = FEATURE_SPRING_ONLY, havingValue = "true")
public class NexusEditionMarkerFile
{
  private static final Logger LOG = LoggerFactory.getLogger(NexusEditionMarkerFile.class);

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public NexusEditionMarkerFile(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = applicationDirectories;
  }

  public boolean exists(final NexusEdition nexusEdition) {
    String filename = "edition_" + nexusEdition.getShortName().toLowerCase(ENGLISH).replace('-', '_');
    return applicationDirectories.getWorkDirectory().toPath().resolve(filename).toFile().exists();
  }

  public boolean write(final NexusEdition nexusEdition) {
    String filename = "edition_" + nexusEdition.getShortName().toLowerCase(ENGLISH).replace('-', '_');
    Path markerFile = applicationDirectories.getWorkDirectory().toPath().resolve(filename);
    try {
      Files.createFile(markerFile);
      return true;
    }
    catch (IOException e) {
      LOG.warn("Failed to create edition marker file {}", markerFile);
    }
    return false;
  }
}
