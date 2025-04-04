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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.bootstrap.entrypoint.ApplicationContextProvider;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.extender.NexusLifecycleManager;

import com.codahale.metrics.SharedMetricRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.common.time.DateHelper.toDurationString;

/**
 * This is the only class that's not currently wired into spring, and is rather created by jetty
 */
public class NexusServletContextListener
    implements ServletContextListener
{
  private static final Logger LOG = LoggerFactory.getLogger(NexusServletContextListener.class);

  private static final String NEXUS_LIFECYCLE_STARTUP_PHASE = "nexus.lifecycle.startupPhase";

  private Phase startupPhase;

  private NexusLifecycleManager nexusLifecycleManager;

  private NexusEditionSelector nexusEditionSelector;

  private void maybeInject() {
    nexusLifecycleManager = ApplicationContextProvider.getBean(NexusLifecycleManager.class);
    nexusEditionSelector = ApplicationContextProvider.getBean(NexusEditionSelector.class);
    NexusProperties nexusProperties = ApplicationContextProvider.getBean(NexusProperties.class);
    String startupPhaseValue = nexusProperties.get(NEXUS_LIFECYCLE_STARTUP_PHASE);
    if (startupPhaseValue != null) {
      startupPhase = Phase.valueOf(startupPhaseValue);
    }
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    maybeInject();

    SharedMetricRegistries.getOrCreate("nexus");

    try {
      checkStartupPhase();
      // Push to the last phase in lifecycle, this will of course process each phase in between en route to TASKS phase
      moveToPhase(TASKS);
    }
    catch (final Exception e) {
      LOG.error("Failed to initialize context", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    maybeInject();
    // log uptime before triggering activity which may run into problems
    long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    LOG.info("Uptime: {} ({})", toDurationString(uptime), nexusEditionSelector.getCurrent());

    try {
      moveToPhase(OFF);
    }
    catch (final Exception e) {
      LOG.error("Failed to stop nexus", e);
    }

    SharedMetricRegistries.remove("nexus");
  }

  enum Period
  {
    SECONDS(60L, "second", null),
    MINUTES(60L, "minute", SECONDS),
    HOURS(24L, "hour", MINUTES),
    DAYS(30L, "day", HOURS),
    MONTHS(12L, "month", DAYS),
    YEARS(0L, "year", MONTHS);

    private long toNextMultiplier;

    private String periodName;

    private Period previousPeriod;

    Period(final long toNextMultiplier, final String periodName, final Period previousPeriod) {
      this.toNextMultiplier = toNextMultiplier;
      this.periodName = periodName;
      this.previousPeriod = previousPeriod;
    }
  }

  /**
   * Checks whether we should limit application startup to a particular lifecycle phase.
   */
  private void checkStartupPhase() throws IOException {
    if (startupPhase != null) {
      LOG.info("Running lifecycle phases {}", EnumSet.range(KERNEL, startupPhase));
    }
    else {
      LOG.info("Running lifecycle phases {}", EnumSet.complementOf(EnumSet.of(OFF)));
    }
  }

  /**
   * Moves the application lifecycle on to a new phase.
   * <p>
   * When {@link #startupPhase} is set startup will never go past that phase.
   */
  private void moveToPhase(final Phase phase) throws Exception {
    if (startupPhase != null && phase.ordinal() > startupPhase.ordinal()) {
      nexusLifecycleManager.to(startupPhase); // this far, no further
    }
    else {
      nexusLifecycleManager.to(phase);
    }
  }
}
