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

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

@Named
@Singleton
public class NexusApplicationJarFilter
    extends ComponentSupport
    implements ApplicationJarFilter
{
  @Override
  public boolean allowed(final String path) {
    String filename = getModuleNameFromFile(path);
    return filename.startsWith("nexus-");
  }

  @Nullable
  private String getModuleNameFromFile(final String path) {
    try {
      Path fsPath = Paths.get(path);
      String fileName = fsPath.getFileName().toString();
      if (fileName.equals("test-classes") || fileName.equals("classes")) {
        return fsPath.getParent().getParent().getFileName().toString();
      }
    }
    catch (Exception e) {
      log.debug("Failed to compute {}", path, e);
    }
    return path.substring(path.lastIndexOf("/") + 1);
  }
}
