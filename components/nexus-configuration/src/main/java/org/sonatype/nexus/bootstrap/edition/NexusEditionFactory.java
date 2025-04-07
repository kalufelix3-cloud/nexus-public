
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
import java.util.List;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;

import com.google.common.annotations.VisibleForTesting;

/**
 * !!!! DEPRECATED in favor of {@link org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector}. The modules
 * containing the edition modules need to be in the bootstrap.entrypoint package so they are loaded in first round of
 * injection. This class should be removed when the previous DI architecture is removed. Until then changes should
 * primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class NexusEditionFactory
{
  private NexusEditionFactory() {
    throw new IllegalStateException("NexusEditionFactory is a Utility class");
  }

  private static final JavaPrefs javaPrefs = new JavaPrefs();

  private static final List<NexusEdition> editions =
      List.of(
          new ProNexusEdition(javaPrefs),
          new CommunityNexusEdition(javaPrefs),
          new CoreNexusEdition(javaPrefs));

  public static void selectActiveEdition(final PropertyMap properties, final Path workDirPath) {
    NexusEdition nexusEdition = findActiveEdition(editions, properties, workDirPath);
    nexusEdition.apply(properties, workDirPath);
  }

  @VisibleForTesting
  static NexusEdition findActiveEdition(
      final List<NexusEdition> editions,
      final PropertyMap properties,
      final Path workDirPath)
  {
    return editions.stream()
        .filter(edition -> edition.applies(properties, workDirPath))
        .findFirst()
        .orElse(new CoreNexusEdition(javaPrefs));
  }
}
