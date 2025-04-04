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
package org.sonatype.nexus.extender.guice.modules;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.spring.application.classpath.finder.JettyConfigurationIndexClassFinder;

import com.google.inject.AbstractModule;
import org.eclipse.sisu.space.ClassFinder;
import org.eclipse.sisu.space.ClassSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Streams.stream;

/**
 * !!!! DEPRECATED in favor of spring @Configuration class. This class would need to use the ClassFinder to find all
 * *ConnectorConfiguration.class files, because this is using classloading and NOT injection, this doesn't have to wait
 * for a specific state of injection. This class should be removed when the previous DI architecture is removed.
 * Until then changes should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to
 * this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class JettyConfigurationModule
    extends AbstractModule
{
  private static final Logger LOG = LoggerFactory.getLogger(JettyConfigurationModule.class);

  private final ClassSpace classSpace;

  private final ClassFinder classFinder;

  public JettyConfigurationModule(
      final JettyConfigurationIndexClassFinder jettyConfigurationIndexClassFinder,
      final ClassSpace classSpace) throws IOException
  {
    this.classFinder = jettyConfigurationIndexClassFinder;
    this.classSpace = classSpace;
  }

  @Override
  protected void configure() {
    LOG.info("Binding ConnectorConfiguration classes to jetty");
    // locate all the DAO class files
    Enumeration<URL> classFileURLs = classFinder.findClasses(classSpace);
    stream(forEnumeration(classFileURLs))
        .map(URL::getPath)
        .map(path -> path.substring(1, path.indexOf(".class")).replace('/', '.'))
        .map(classFileName -> {
          try {
            LOG.debug("Looking up class {}", classFileName);
            Class<?> clazz = classSpace.loadClass(classFileName);
            LOG.debug("Found class {}", clazz);
            return (Class<DataAccess>) clazz;
          }
          catch (LinkageError | Exception e) {
            LOG.warn("Could not load {} for DataAccess binding", classFileName, e);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .forEach(clazz -> {
          // TODO: add configuration to the jetty server object
          // bind(new DAOKey(clazz)).toInstance(clazz);
        });
  }
}
