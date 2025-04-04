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

import javax.inject.Inject;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import static org.springframework.boot.Banner.Mode.OFF;

/**
 * !!!! DEPRECATED in favor of {@link org.sonatype.nexus.bootstrap.entrypoint.core.NexusRepositoryCoreApplication}.
 * This class should be removed when the previous DI architecture is removed. Until then changes should primarily be
 * done on the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@SpringBootApplication(scanBasePackages = {
    "org.sonatype.nexus.bootstrap",
    "org.sonatype.nexus.spring"
})
public class NexusRepositoryCoreApplication
    extends NexusApplication
{
  @Inject
  public NexusRepositoryCoreApplication(final Launcher launcher) {
    super(launcher);
  }

  public static void main(String[] args) {
    System.setProperty("spring.config.location", "etc/default-application.properties");
    // since logback is going to start on its own, we need to tell it where to find its configuration
    System.setProperty("logback.configurationFile", "etc/logback/logback.xml");

    // Ensure karaf.data is set as logback needs it early
    NexusDirectoryConfiguration.load();

    // we don't want Spring to own our logging configuration right now
    // at this phase in the migration, Guice still bears the responsibility for configuring logging
    // these two lines tell Spring that there is no logging, preventing any undesirable output to stdout/err
    // later on during the startup process, Guice will pick up our slf4j/logback configuration and logging will work
    System.setProperty(LoggingSystem.class.getName(), LoggingSystem.NONE);
    LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel("ROOT", LogLevel.OFF);

    new SpringApplicationBuilder(NexusRepositoryCoreApplication.class)
        .bannerMode(OFF)
        .run(args);
  }
}
