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
package org.sonatype.nexus.extender;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.bootstrap.application.DelegatingFilter;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;
import org.sonatype.nexus.extender.guice.NexusLifecycleManager;
import org.sonatype.nexus.extender.guice.modules.NexusExtenderModule;
import org.sonatype.nexus.spring.application.NexusProperties;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.DynamicGuiceFilter;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.sisu.inject.InjectorBindings;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.text.Strings2.isEmpty;

/**
 * {@link ServletContextListener} that bootstraps the core Nexus application.
 */
public class NexusServletContextListener
    implements ServletContextListener
{
  private static final String NEXUS_LIFECYCLE_STARTUP_PHASE = "nexus.lifecycle.startupPhase";

  private static final Logger log = LoggerFactory.getLogger(NexusServletContextListener.class);

  private ServletContext servletContext;

  private Injector injector;

  private ManagedLifecycleManager lifecycleManager;

  private Phase startupPhase;

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    checkNotNull(event);

    SharedMetricRegistries.getOrCreate("nexus");

    servletContext = event.getServletContext();

    try {
      injector = Guice.createInjector(new NexusExtenderModule(servletContext));

      MutableBeanLocator locator = injector.getInstance(MutableBeanLocator.class);
      locator.add(new InjectorBindings(injector));

      lifecycleManager = new NexusLifecycleManager(locator);
      checkStartupPhase((NexusProperties) servletContext.getAttribute("nexusProperties"));

      // Push to the last phase in lifecycle, this will of course process each phase in between en route to TASKS phase
      moveToPhase(TASKS);

      GuiceFilter filter = injector.getInstance(DynamicGuiceFilter.class);
      DelegatingFilter.set(filter);
    }
    catch (final Exception e) {
      log.error("Failed to initialize context", e);
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    // log uptime before triggering activity which may run into problems
    long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    log.info(
        "Uptime: {}",
        PeriodFormat.getDefault().print(new Period(uptime)));
    // TODO: add edition back into log message

    try {
      moveToPhase(OFF);
    }
    catch (final Exception e) {
      log.error("Failed to stop nexus", e);
    }

    if (servletContext != null) {
      servletContext = null;
    }

    injector = null;

    SharedMetricRegistries.remove("nexus");
  }

  /**
   * Checks whether we should limit application startup to a particular lifecycle phase.
   */
  private void checkStartupPhase(final NexusProperties nexusProperties) throws IOException {
    String startupPhaseValue = nexusProperties.get().get(NEXUS_LIFECYCLE_STARTUP_PHASE);
    if (!isEmpty(startupPhaseValue)) {
      try {
        startupPhase = Phase.valueOf(startupPhaseValue);
        log.info("Running lifecycle phases {}", EnumSet.range(KERNEL, startupPhase));
      }
      catch (IllegalArgumentException e) {
        log.error("Unknown value for {}: {}", NEXUS_LIFECYCLE_STARTUP_PHASE, startupPhaseValue);
        throw e;
      }
    }
    else {
      log.info("Running lifecycle phases {}", EnumSet.complementOf(EnumSet.of(OFF)));
    }
  }

  /**
   * Moves the application lifecycle on to a new phase.
   * <p>
   * When {@link #startupPhase} is set startup will never go past that phase.
   */
  private void moveToPhase(final Phase phase) throws Exception {
    if (startupPhase != null && phase.ordinal() > startupPhase.ordinal()) {
      lifecycleManager.to(startupPhase); // this far, no further
    }
    else {
      lifecycleManager.to(phase);
    }
  }
}
