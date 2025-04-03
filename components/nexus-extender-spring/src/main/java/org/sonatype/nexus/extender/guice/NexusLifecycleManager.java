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
package org.sonatype.nexus.extender.guice;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.reverse;
import static java.lang.Math.max;
import static org.sonatype.nexus.common.app.FeatureFlags.STARTUP_TASKS_DELAY_SECONDS;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.KERNEL;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.OFF;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Manages any {@link Lifecycle} components annotated with {@link ManagedLifecycle}.
 * <p>
 * Components are managed during their appropriate phase in order of their priority.
 */
@Singleton
@Named
public class NexusLifecycleManager
    extends ManagedLifecycleManager
{
  private static final Phase[] PHASES = Phase.values();

  private final Multimap<Phase, Lifecycle> components = HashMultimap.create();

  private final List<BeanEntry<Annotation, Lifecycle>> lifecycles = new ArrayList<>();

  private volatile Phase currentPhase = OFF;

  private final BeanLocator beanLocator;

  private final int timeToDelay;

  @Inject
  public NexusLifecycleManager(final BeanLocator beanLocator) {
    this(beanLocator, 0);
  }

  public NexusLifecycleManager(
      final BeanLocator beanLocator,
      @Named(STARTUP_TASKS_DELAY_SECONDS) final int timeToDelay)
  {
    this.beanLocator = checkNotNull(beanLocator);
    this.timeToDelay = timeToDelay;

    beanLocator.locate(Key.get(Lifecycle.class, Named.class)).forEach(lifecycles::add);
    // Make sure the lifecycles are processed in priority order
    lifecycles.sort((o1, o2) -> {
      int priority1 = getPriority(o1.getImplementationClass());
      int priority2 = getPriority(o2.getImplementationClass());
      return -Integer.compare(priority1, priority2);
    });
  }

  @Override
  public Phase getCurrentPhase() {
    return currentPhase;
  }

  @Override
  public void to(final Phase targetPhase) throws Exception { // NOSONAR
    if (targetPhase == OFF) {
      declareShutdown();
    }
    else if (isShuttingDown()) {
      return; // cannot go back once shutdown has begun
    }

    synchronized (beanLocator) {
      final int target = targetPhase.ordinal();
      int current = currentPhase.ordinal();

      log.debug("Getting lifecycle managed components up to phase {}", current < target ? targetPhase : currentPhase);
      ListMultimap<Phase, BeanEntry<Annotation, Lifecycle>> lifeCyclesInPhase =
          getLifecyclesInPhase(current < target ? targetPhase : currentPhase);

      // moving forwards to later phase, start components in priority order
      while (current < target) {
        Phase nextPhase = PHASES[++current];
        log.info("Start {}", nextPhase);
        boolean propagateNonTaskErrors = !TASKS.equals(nextPhase);
        for (BeanEntry<Annotation, Lifecycle> entry : lifeCyclesInPhase.get(nextPhase)) {
          if (nextPhase.equals(TASKS) && timeToDelay > 0) {
            delayStartUpTask(nextPhase, entry.getValue(), propagateNonTaskErrors);
          }
          else {
            startComponent(nextPhase, entry.getValue(), propagateNonTaskErrors);
          }
        }
        currentPhase = nextPhase;
      }

      // rolling back to earlier phase, stop components in reverse priority order
      while (current > target) {
        Phase prevPhase = PHASES[--current];
        log.info("Stop {}", currentPhase);
        for (BeanEntry<Annotation, Lifecycle> entry : reverse(lifeCyclesInPhase.get(currentPhase))) {
          stopComponent(currentPhase, entry.getValue(), false);
        }
        currentPhase = prevPhase;
      }
    }

    if (currentPhase == OFF) {
      log.info("Nexus is now OFF");
    }
  }

  @Override
  public void bounce(final Phase bouncePhase) throws Exception {
    Phase targetPhase = currentPhase;
    // re-run the given phase by moving to just before it before moving back
    if (bouncePhase.ordinal() <= targetPhase.ordinal()) {
      if (bouncePhase == KERNEL) {
        System.setProperty("karaf.restart", "true");
      }
      to(Phase.values()[max(0, bouncePhase.ordinal() - 1)]);
    }
    else {
      targetPhase = bouncePhase; // bounce phase is later, just move to it
    }
    to(targetPhase);
  }

  /**
   * Starts the given lifecycle component, propagating lifecycle errors only when requested.
   */
  private void startComponent(
      final Phase phase,
      final Lifecycle lifecycle,
      final boolean propagateErrors) throws Exception
  {
    try {
      if (components.put(phase, lifecycle)) {
        log.debug("Start {}: {}", phase, lifecycle);
        lifecycle.start();
      }
    }
    catch (Exception | LinkageError e) {
      if (propagateErrors) {
        throw e;
      }
      log.warn("Problem starting {}: {}", phase, lifecycle, e);
    }
  }

  /**
   * Stops the given lifecycle component, propagating lifecycle errors only when requested.
   */
  private void stopComponent(
      final Phase phase,
      final Lifecycle lifecycle,
      final boolean propagateErrors) throws Exception
  {
    try {
      if (components.remove(phase, lifecycle)) {
        log.debug("Stop {}: {}", phase, lifecycle);
        lifecycle.stop();
      }
    }
    catch (Exception | LinkageError e) {
      if (propagateErrors) {
        throw e;
      }
      log.warn("Problem stopping {}: {}", phase, lifecycle, e);
    }
  }

  private void delayStartUpTask(final Phase phase, final Lifecycle lifecycle, final boolean propagateErrors) {
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      try {
        startComponent(phase, lifecycle, propagateErrors);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, timeToDelay, TimeUnit.SECONDS);
  }

  /**
   * Creates a multilevel index containing all managed lifecycles up to and including the target phase.
   */
  private ListMultimap<Phase, BeanEntry<Annotation, Lifecycle>> getLifecyclesInPhase(final Phase targetPhase) {
    ListMultimap<Phase, BeanEntry<Annotation, Lifecycle>> lifecyclesInPhase = ArrayListMultimap.create();

    final int target = targetPhase.ordinal();

    log.info("Indexing lifecycle managed components up to phase {}", targetPhase);

    for (BeanEntry<Annotation, Lifecycle> entry : lifecycles) {
      ManagedLifecycle managedLifecycle = entry.getImplementationClass().getAnnotation(ManagedLifecycle.class);
      if (managedLifecycle != null && managedLifecycle.phase().ordinal() <= target) {
        lifecyclesInPhase.put(managedLifecycle.phase(), entry);
      }
    }

    return lifecyclesInPhase;
  }

  private int getPriority(final Class<?> clazz) {
    Priority priorityAnnotation = clazz.getAnnotation(Priority.class);
    // 0 is lowest priority, default value
    return priorityAnnotation != null ? priorityAnnotation.value() : 0;
  }
}
