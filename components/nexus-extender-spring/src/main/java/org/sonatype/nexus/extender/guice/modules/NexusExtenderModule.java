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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.extender.sisu.modules.SisuAggregatedIndexModule;
import org.sonatype.nexus.spring.application.NexusProperties;
import org.sonatype.nexus.spring.application.classpath.components.MybatisDAOComponentList;
import org.sonatype.nexus.spring.application.classpath.components.SisuComponentMap;
import org.sonatype.nexus.spring.application.classpath.finder.FeatureFlagEnabledClassFinderFilter;
import org.sonatype.nexus.spring.application.classpath.finder.NexusMybatisDAOIndexClassFinder;
import org.sonatype.nexus.spring.application.classpath.finder.NexusSisuAggregatedIndexClassFinder;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * !!!! DEPRECATED in favor of spring @Configuration class. This class should no longer care about other modules,
 * just needs to expose the "(org|com)/sonatype.nexus.bootstrap" package for injection. Could also use a high
 * priority value to get processed last. This class should be removed when the previous DI architecture is removed.
 * Until then changes should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to
 * this class if necessary
 * -------------------------------------------------------
 * Old javadoc
 * The main entry module for sisu/guice configuration
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class NexusExtenderModule
    extends AbstractModule
{
  private final URLClassSpace space;

  private final ShiroAopModule shiroAopModule = new ShiroAopModule();

  private final SecurityFilterModule securityFilterModule;

  private final MetricsRegistryModule metricsRegistryModule;

  private final ValidationModule validationModule = new ValidationModule();

  private final RankingModule rankingModule = new RankingModule();

  private final NexusProperties nexusProperties;

  private final SisuComponentMap sisuComponentMap;

  private final MybatisDAOComponentList mybatisDAOComponentList;

  private final FeatureFlagEnabledClassFinderFilter featureFlagEnabledClassFinderFilter;

  private final NexusServletContextModule nexusServletContextModule;

  private static final Logger LOG = LoggerFactory.getLogger(NexusExtenderModule.class);

  public NexusExtenderModule(final ServletContext servletContext) throws IOException {
    this.space = new URLClassSpace(getClass().getClassLoader());
    this.nexusProperties = (NexusProperties) servletContext.getAttribute("nexusProperties");
    this.sisuComponentMap = (SisuComponentMap) servletContext.getAttribute("sisuComponentMap");
    this.mybatisDAOComponentList = (MybatisDAOComponentList) servletContext.getAttribute("mybatisDAOComponentList");
    this.featureFlagEnabledClassFinderFilter = (FeatureFlagEnabledClassFinderFilter) servletContext.getAttribute(
        "featureFlagEnabledClassFinderFilter");
    this.nexusServletContextModule = new NexusServletContextModule(servletContext, nexusProperties.get());
    this.metricsRegistryModule = new MetricsRegistryModule(this.nexusProperties.get());
    this.securityFilterModule = new SecurityFilterModule(this.nexusProperties.get());
  }

  @Override
  protected void configure() {
    List<Module> modules = new ArrayList<>();
    URL[] allUrls = space.getURLs();
    LOG.debug("all classpath urls: {}", Arrays.toString(allUrls));
    try {
      Map<String, String> properties = nexusProperties.get();
      if (!properties.containsKey(BeanScanning.class.getName())) {
        properties.put(BeanScanning.class.getName(), BeanScanning.GLOBAL_INDEX.name());
      }

      modules.add(new DataAccessModule(
          new NexusMybatisDAOIndexClassFinder(
              mybatisDAOComponentList,
              featureFlagEnabledClassFinderFilter),
          space));
      modules.add(new SisuAggregatedIndexModule(
          space,
          new NexusSisuAggregatedIndexClassFinder(
              sisuComponentMap,
              featureFlagEnabledClassFinderFilter)));
      modules.add(securityFilterModule);
      modules.add(nexusServletContextModule);
      modules.add(metricsRegistryModule);
      WebResourcesModule webResourcesModule = new WebResourcesModule(space);
      modules.add(webResourcesModule);
      modules.add(shiroAopModule);
      modules.add(validationModule);
      modules.add(rankingModule);

      bind(Logger.class).toInstance(LoggerFactory.getLogger(LogManager.class));

      new WireModule(modules).configure(binder());
    }
    catch (IOException e) {
      LOG.error("Failed to load nexusProperties", e);
    }
  }
}
