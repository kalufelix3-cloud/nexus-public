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
package org.sonatype.nexus.bootstrap.entrypoint.edition.core;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionMarkerFile;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSupport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Singleton
@Named
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
public class CoreNexusEdition
    extends NexusEditionSupport
{
  public static final String NAME = "nexus-core-edition";

  public static final String SHORT_NAME = "CORE";

  @Inject
  public CoreNexusEdition(final NexusEditionMarkerFile nexusEditionMarkerFile) {
    // we want core to be the last edition validated
    super(NAME, NAME, SHORT_NAME, Integer.MAX_VALUE, nexusEditionMarkerFile);
  }

  @Override
  public boolean isActive() {
    // no checks to do, if no other edition is enabled, core is enabled
    return true;
  }
}
