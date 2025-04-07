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

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.bootstrap.entrypoint.jetty.JettyServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.MapPropertySource;

@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
public class ApplicationLauncher
{
  public static final String SYSTEM_USERID = "*SYSTEM";

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationLauncher.class);

  private final NexusEditionSelector nexusEditionSelector;

  private final ConfigurableApplicationContext context;

  private final JettyServer jettyServer;

  @Inject
  public ApplicationLauncher(
      final NexusEditionSelector nexusEditionSelector,
      final JettyServer jettyServer,
      final ConfigurableApplicationContext context)
  {
    this.nexusEditionSelector = nexusEditionSelector;
    this.jettyServer = jettyServer;
    this.context = context;

    initialize();
  }

  public void start() throws Exception {
    MDC.put("userId", SYSTEM_USERID);
    jettyServer.start(false, () -> {
    });
  }

  public void stop() throws Exception {
    MDC.remove("userId");
    jettyServer.stop();
  }

  private void initialize() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    NexusEdition nexusEdition = nexusEditionSelector.getCurrent();

    LOG.info("Starting nexus with edition {}", nexusEdition.getShortName());

    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(new MapPropertySource(
            "application-launcher",
            Map.of("nexus.edition", nexusEdition)));
  }

  // don't start jetty until spring context is fully initialized
  @EventListener(ContextRefreshedEvent.class)
  public void onContextRefreshed(final ContextRefreshedEvent event) {
    try {
      start();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure application", e);
    }
  }
}
