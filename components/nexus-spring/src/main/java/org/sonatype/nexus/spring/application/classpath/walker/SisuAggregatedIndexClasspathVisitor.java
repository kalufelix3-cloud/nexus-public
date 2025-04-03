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
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.spring.application.classpath.components.SisuComponentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class SisuAggregatedIndexClasspathVisitor
    extends AbstractClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(SisuAggregatedIndexClasspathVisitor.class);

  private final SisuComponentMap sisuComponentMap;

  @Inject
  public SisuAggregatedIndexClasspathVisitor(final SisuComponentMap sisuComponentMap) {
    this.sisuComponentMap = checkNotNull(sisuComponentMap);
  }

  @Override
  public String name() {
    return "Sisu Aggregated Index Classpath Visitor";
  }

  @Override
  public void doVisit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    List<String> components = toSimpleStringList(applicationJarInputStream);
    LOG.debug("Found indexed components: {}", components);
    sisuComponentMap.addComponents(applicationJarPath, components);
  }

  @Override
  protected boolean applies(final String path) {
    return path.endsWith("META-INF/sisu/javax.inject.Named");
  }
}
