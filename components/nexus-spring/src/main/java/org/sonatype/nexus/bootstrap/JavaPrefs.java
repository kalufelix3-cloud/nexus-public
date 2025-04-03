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
package org.sonatype.nexus.bootstrap;

import javax.inject.Named;
import javax.inject.Singleton;

import static java.util.prefs.Preferences.userRoot;

@Singleton
@Named
public class JavaPrefs
{
  public boolean isLicenseInstalled() {
    Thread currentThread = Thread.currentThread();
    ClassLoader tccl = currentThread.getContextClassLoader();
    // Java prefs spawns a Timer-Task that inherits the current TCCL;
    // temporarily clear it so we can be GC'd if we bounce the KERNEL
    currentThread.setContextClassLoader(null);
    try {
      return userRoot().node("/com/sonatype/nexus/professional").get("license", null) != null;
    }
    finally {
      currentThread.setContextClassLoader(tccl);
    }
  }
}
