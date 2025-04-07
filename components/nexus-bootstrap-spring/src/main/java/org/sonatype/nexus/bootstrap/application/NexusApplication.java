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
package org.sonatype.nexus.bootstrap.application;

import javax.annotation.PreDestroy;

import org.sonatype.nexus.bootstrap.jetty.JettyServer;

import org.slf4j.MDC;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static org.sonatype.nexus.bootstrap.application.Launcher.SYSTEM_USERID;

/**
 * !!!! DEPRECATED in favor of org.sonatype.nexus.bootstrap.entrypoint.NexusApplication, no need for the static
 * reference to jetty server any longer. This class should be removed when the previous DI architecture is removed.
 * Until then changes should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to
 * this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public abstract class NexusApplication
{
  /**
   * See class level javadoc
   */
  @Deprecated
  private static JettyServer STATIC_SERVER_REFERENCE;

  @Deprecated
  private final Launcher launcher;

  public NexusApplication(final Launcher launcher) {
    this.launcher = launcher;
  }

  // don't start jetty until spring context is fully initialized
  @EventListener(ContextRefreshedEvent.class)
  public void onContextRefreshed() {
    try {
      configureApplication();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure application", e);
    }
  }

  private void configureApplication() throws Exception {
    MDC.put("userId", SYSTEM_USERID);

    // spring continues launch via PostBootstrapInjector when springOnly is true
    // otherwise, we drive forward with the old spring/sisu/guice rute
    if (launcher != null) {
      launcher.startAsync(() -> {
      });
      STATIC_SERVER_REFERENCE = launcher.getServer(); // NOSONAR
    }
  }

  @PreDestroy
  public void stop() throws Exception {
    if (launcher != null) {
      launcher.stop();
    }
  }

  /**
   * See class level javadoc
   */
  @Deprecated
  public static JettyServer getServerReference() {
    return STATIC_SERVER_REFERENCE;
  }
}
