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

import java.util.Map;
import javax.servlet.ServletContext;

import org.sonatype.nexus.blobstore.metrics.BlobStoreModule;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.guice.TimeTypeConverter;
import org.sonatype.nexus.common.stateguard.StateGuardModule;
import org.sonatype.nexus.security.WebSecurityModule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.DynamicGuiceFilter;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.sisu.bean.BeanManager;
import org.eclipse.sisu.inject.DefaultBeanLocator;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.wire.ParameterKeys;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * !!!! DEPRECATED in favor of spring @Configuration class. As this class is just grouping other modules, we can
 * get by without a replacment, as i'd expect each of the other modules/configurations to be automatically dealt with.
 * This class should be removed when the previous DI architecture is removed. Until then changes should primarily be
 * done on the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class NexusServletContextModule
    extends AbstractModule
{
  private final ServletContext servletContext;

  private final Map<?, ?> nexusProperties;

  public NexusServletContextModule(final ServletContext servletContext, final Map<?, ?> nexusProperties) {
    this.servletContext = checkNotNull(servletContext);
    this.nexusProperties = checkNotNull(nexusProperties);
  }

  @Override
  protected void configure() {

    // we will look these up later...
    requireBinding(GuiceFilter.class);
    requireBinding(BeanManager.class);
    requireBinding(ApplicationVersion.class);

    bind(ServletContext.class).toInstance(servletContext);
    bind(ParameterKeys.PROPERTIES).toInstance(nexusProperties);

    install(new StateGuardModule());
    install(new TransactionModule());
    install(new BlobStoreModule());
    install(new TimeTypeConverter());
    install(new WebSecurityModule(servletContext));

    MutableBeanLocator locator = new DefaultBeanLocator();
    bind(MutableBeanLocator.class).toInstance(locator);

    DynamicGuiceFilter.avoidLogSpam();
  }
}
