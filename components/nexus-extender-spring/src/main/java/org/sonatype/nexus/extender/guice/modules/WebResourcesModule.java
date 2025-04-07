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

import org.sonatype.nexus.extender.guice.StaticWebResource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.webresources.WebResourceBundle;

import com.google.inject.AbstractModule;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.eclipse.sisu.space.ClassSpace;

/**
 * !!!! DEPRECATED in favor of a new spring @Configuration class. This class should be removed when the previous DI
 * architecture is removed. Until then changes should primarily be done on the newer "nexus.spring.only=true" impl,
 * then only brought back to this class if necessary
 * -------------------------------------------------------
 * Old javadoc
 * Provides common {@link WebResourceBundle} bindings.
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class WebResourcesModule
    extends AbstractModule
{
  private static final Named STATIC = Names.named("static");

  private final ClassSpace classSpace;

  public WebResourcesModule(final ClassSpace classSpace) {
    this.classSpace = classSpace;
  }

  @Override
  protected void configure() {
    bind(ClassSpace.class).toInstance(classSpace);
    bind(MimeSupport.class).to(DefaultMimeSupport.class);
    bind(WebResourceBundle.class).annotatedWith(STATIC).to(StaticWebResource.class).asEagerSingleton();
  }
}
