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
package org.sonatype.nexus.bootstrap.entrypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

public abstract class NexusApplication
{
  private static final Logger LOG = LoggerFactory.getLogger(NexusApplication.class);

  // don't start jetty until spring context is fully initialized
  @EventListener(ContextRefreshedEvent.class)
  public void onContextRefreshed() {
    try {
      LOG.info("Bootstrapping of entrypoint completed.");
      configureApplication();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure application", e);
    }
  }

  private void configureApplication() {
    // spring continues launch via PostBootstrapInjector
  }
}
