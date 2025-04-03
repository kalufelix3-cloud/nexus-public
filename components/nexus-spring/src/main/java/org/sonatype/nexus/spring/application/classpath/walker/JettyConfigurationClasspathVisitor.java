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
package org.sonatype.nexus.spring.application.classpath.walker;

import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.spring.application.classpath.components.JettyConfigurationComponentList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class JettyConfigurationClasspathVisitor
    extends AbstractClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(JettyConfigurationClasspathVisitor.class);

  private final JettyConfigurationComponentList jettyConfigurationComponentSet;

  @Inject
  public JettyConfigurationClasspathVisitor(final JettyConfigurationComponentList jettyConfigurationComponentSet) {
    this.jettyConfigurationComponentSet = checkNotNull(jettyConfigurationComponentSet);
  }

  @Override
  public String name() {
    return "Jetty Configuration Classpath Visitor";
  }

  @Override
  protected void doVisit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    jettyConfigurationComponentSet.addComponent(path);
    LOG.debug("Adding Jetty configuration class to cache: {}", path);
  }

  @Override
  protected boolean applies(final String path) {
    return path.endsWith("ConnectorConfiguration.class") &&
        !path.endsWith("org/sonatype/nexus/bootstrap/jetty/ConnectorConfiguration.class");
  }
}
