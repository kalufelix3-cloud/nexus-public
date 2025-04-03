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
import org.sonatype.nexus.spring.application.ShutdownHelper;
import org.sonatype.nexus.spring.application.ShutdownHelper.ShutdownDelegate;

import org.slf4j.MDC;

import static org.sonatype.nexus.bootstrap.application.Launcher.SYSTEM_USERID;

public abstract class NexusApplication
    implements ShutdownDelegate
{
  private final Launcher launcher;

  private static JettyServer STATIC_SERVER_REFERENCE;

  public NexusApplication(final Launcher launcher) {
    this.launcher = launcher;
  }

  public void onContextRefreshed() {
    try {
      configureApplication();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure application", e);
    }
  }

  private void configureApplication() throws Exception {
    ShutdownHelper.setDelegate(this);

    MDC.put("userId", SYSTEM_USERID);
    launcher.startAsync(
        () -> {
        });
    STATIC_SERVER_REFERENCE = launcher.getServer(); // NOSONAR
  }

  /**
   * For this interim iteration of our migration away from OSGi and on to Spring, there is one single class instance
   * that needs to be instantiated by Spring, yet still accessible to Guice managed beans: the JettyServer.
   * At time of writing we do not have a way to take Spring managed beans and make them available to Guice.
   * This method is a temporary mechanism for Guice managed beans to get a reference to the JettyServer.
   * When Guice is removed, this method should be removed and the JettyServer will be injected directly into
   * downstream dependent classes.
   * This method will return null until {@link #onContextRefreshed()} has been called.
   *
   * @return a reference to the JettyServer under normal conditions
   */
  public static JettyServer getServerReference() {
    return STATIC_SERVER_REFERENCE;
  }

  @PreDestroy
  public void stop() throws Exception {
    launcher.stop();
  }

  @Override
  public void doExit(int code) {
    ShutdownHelper.setDelegate(ShutdownHelper.JAVA); // avoid recursion
    ShutdownHelper.exit(code);
  }

  @Override
  public void doHalt(int code) {
    ShutdownHelper.halt(code);
  }
}
