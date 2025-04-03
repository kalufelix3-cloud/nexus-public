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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public abstract class AbstractClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(AbstractClasspathVisitor.class);

  @Override
  public boolean visit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    if (applies(path)) {
      doVisit(path, applicationJarPath, applicationJarInputStream);
      return true;
    }
    return false;
  }

  protected List<String> toSimpleStringList(final InputStream applicationJarInputStream) {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(applicationJarInputStream));
    return bufferedReader.lines().collect(toList());
  }

  protected abstract boolean applies(final String path);

  protected abstract void doVisit(
      final String path,
      final String applitcationJarPath,
      final InputStream applicationJarInputStream);
}
