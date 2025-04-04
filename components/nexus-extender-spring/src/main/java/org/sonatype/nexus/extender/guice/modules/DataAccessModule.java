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

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.spring.application.classpath.finder.NexusMybatisDAOIndexClassFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import org.eclipse.sisu.space.ClassFinder;
import org.eclipse.sisu.space.ClassSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;

/**
 * !!!! DEPRECATED in favor of spring @Configuration class. This class would need to use the ClassFinder to find all
 * *DAO.class files, because this is using classloading and NOT injection, this doesn't have to wait for a specific
 * state of injection. This class should be removed when the previous DI architecture is removed. Until then changes
 * should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to this class if
 * necessary
 * -------------------------------------------------------
 * Old javadoc
 * Binds any DAO classes in the application, so they can be processed later by datastore implementations.
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class DataAccessModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(DataAccessModule.class);

  private final ClassSpace classSpace;

  private final ClassFinder classFinder;

  public DataAccessModule(
      final NexusMybatisDAOIndexClassFinder nexusMybatisDAOIndexClassFinder,
      final ClassSpace classSpace) throws IOException
  {
    this.classFinder = nexusMybatisDAOIndexClassFinder;
    this.classSpace = classSpace;
  }

  @Override
  protected void configure() {
    log.info("Binding DAO classes to mybatis");
    // locate all the DAO class files
    Enumeration<URL> classFileURLs = classFinder.findClasses(classSpace);
    while (classFileURLs.hasMoreElements()) {
      URL classFileURL = classFileURLs.nextElement();
      String classFileName = resolveClassName(classFileURL);
      try {
        Class<?> clazz = classSpace.loadClass(classFileName);
        if (clazz != null && DataAccess.class.isAssignableFrom(clazz)) {
          log.debug("located DataAccess class {}", clazz);
          bind(new DAOKey((Class<DataAccess>) clazz)).toInstance((Class<DataAccess>) clazz);
        }
      }
      catch (LinkageError | Exception e) {
        log.warn("Could not load {} for DataAccess binding", classFileName, e);
      }
    }
  }

  /**
   * Resolving a class name from the URL is dependent on how the application is started.
   * <p>
   * When running "java -jar " on the uber jar, the URL will include the nesting of jars. When running inside the IDE,
   * the URL will be to a classes directory in one of the maven modules of the project.
   *
   * @param classFileUrl
   * @return the fully qualified class name
   */
  String resolveClassName(URL classFileUrl) {
    String path = classFileUrl.getPath();
    String result = null;
    if (path.contains("jar")) {
      String[] fqdn = path.split("!");
      String classFileNameWithSlashes = fqdn[2].substring(1);
      result = classFileNameWithSlashes.replace("/", ".").replace(".class", "");
    }
    else if (path.contains("classes")) {
      String[] fqdn = path.split("classes");
      String classFileNameWithSlashes = fqdn[1].substring(1);
      result = classFileNameWithSlashes.replace("/", ".").replace(".class", "");
    }
    log.debug("resolved class name of {} from path {}", result, path);
    return result;
  }

  /**
   * Represents a unique key for the given {@link DataAccess} type.
   */
  private static class DAOKey
      extends Key<Class<DataAccess>>
  {
    DAOKey(final Class<DataAccess> clazz) {
      super(named(clazz.getName()));
    }
  }
}
