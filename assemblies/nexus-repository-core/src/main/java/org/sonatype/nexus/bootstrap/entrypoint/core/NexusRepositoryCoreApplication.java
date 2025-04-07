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
package org.sonatype.nexus.bootstrap.entrypoint.core;

import java.util.Map;

import org.sonatype.nexus.bootstrap.entrypoint.NexusApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.springframework.boot.Banner.Mode.OFF;

// Minimalist list of root packages to scan, to start bootstrapping
@SpringBootApplication(scanBasePackages = {
    "org.sonatype.nexus.bootstrap.entrypoint"
})
public class NexusRepositoryCoreApplication
    extends NexusApplication
{
  public static void main(String[] args) {
    System.setProperty("spring.config.location", "etc/default-application.properties");
    // since logback is going to start on its own, we need to tell it where to find its configuration
    System.setProperty("logback.configurationFile", "etc/logback/logback.xml");

    new SpringApplicationBuilder(NexusRepositoryCoreApplication.class)
        .bannerMode(OFF)
        .properties(Map.of("nexus.spring.only", "true"))
        .run(args);
  }
}
