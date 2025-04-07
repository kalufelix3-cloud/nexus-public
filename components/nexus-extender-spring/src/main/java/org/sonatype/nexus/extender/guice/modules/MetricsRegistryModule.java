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

import org.sonatype.nexus.extender.guice.listeners.CachedGaugeTypeListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.palominolabs.metrics.guice.DefaultMetricNamer;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import com.palominolabs.metrics.guice.annotation.MethodAnnotationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * !!!! DEPRECATED in favor of spring @Configuration class. This class should be removed when the previous DI
 * architecture is removed. Until then changes should primarily be done on the newer "nexus.spring.only=true" impl,
 * then only brought back to this class if necessary
 * -------------------------------------------------------
 * Old javadoc
 * Provides access to the shared metrics and healthcheck registries.
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class MetricsRegistryModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(MetricsRegistryModule.class);

  static final HealthCheckRegistry HEALTH_CHECK_REGISTRY = new HealthCheckRegistry();

  private static final String METRIC_REGISTRY_NAME = "nexus";

  private static final String USAGE_METRIC_REGISTRY_NAME = "usage";

  private final Map<?, ?> nexusProperties;

  public MetricsRegistryModule(Map<?, ?> nexusProperties) {
    this.nexusProperties = nexusProperties;
  }

  @Override
  protected void configure() {
    install(MetricsInstrumentationModule
        .builder()
        .withMetricRegistry(SharedMetricRegistries.getOrCreate(METRIC_REGISTRY_NAME))
        .build());
    bindListener(
        Matchers.any(),
        new CachedGaugeTypeListener(
            SharedMetricRegistries.getOrCreate(METRIC_REGISTRY_NAME),
            new DefaultMetricNamer(),
            new MethodAnnotationResolver(),
            nexusProperties));

    bind(MetricRegistry.class).toInstance(SharedMetricRegistries.getOrCreate(METRIC_REGISTRY_NAME));
    bind(MetricRegistry.class)
        .annotatedWith(Names.named(USAGE_METRIC_REGISTRY_NAME))
        .toInstance(SharedMetricRegistries.getOrCreate(USAGE_METRIC_REGISTRY_NAME));
    bind(HealthCheckRegistry.class).toInstance(HEALTH_CHECK_REGISTRY);
    log.info("MetricsRegistryModule configured");
  }
}
