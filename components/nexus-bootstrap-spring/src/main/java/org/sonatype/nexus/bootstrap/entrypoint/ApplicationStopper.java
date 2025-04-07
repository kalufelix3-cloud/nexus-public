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

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;

@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
public class ApplicationStopper
{
  private static final Logger LOG = LoggerFactory.getLogger(ApplicationStopper.class);

  private final ApplicationLauncher applicationLauncher;

  public ApplicationStopper(final ApplicationLauncher applicationLauncher) {
    this.applicationLauncher = applicationLauncher;
  }

  @EventListener
  public void handleExit(final ExitCodeEvent exitCodeEvent) throws Exception {
    LOG.info("Received exit event with code {} from spring, stopping nexus", exitCodeEvent.getExitCode());
    applicationLauncher.stop();
    LOG.info("Nexus stopped");
  }
}
