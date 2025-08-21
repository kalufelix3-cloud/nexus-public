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
package org.sonatype.nexus.scheduling;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.thread.NexusThreadFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Support class for tasks which uses an executor to parallelize parts of the work
 */
public abstract class ParallelTaskSupport
    extends TaskSupport
{
  private final int concurrencyLimit;

  private final int queueCapacity;

  /**
   * @param concurrencyLimit the number of concurrent threads processing the queue allowed
   * @param queueCapacity the number of queued jobs allowed
   */
  protected ParallelTaskSupport(final int concurrencyLimit, final int queueCapacity) {
    validate(concurrencyLimit, queueCapacity);
    this.concurrencyLimit = concurrencyLimit;
    this.queueCapacity = queueCapacity;
  }

  /**
   * @param concurrencyLimit the number of concurrent threads processing the queue allowed
   * @param queueCapacity the number of queued jobs allowed
   */
  protected ParallelTaskSupport(final boolean taskLoggingEnabled, final int concurrencyLimit, final int queueCapacity) {
    super(taskLoggingEnabled);
    validate(concurrencyLimit, queueCapacity);
    this.concurrencyLimit = concurrencyLimit;
    this.queueCapacity = queueCapacity;
  }

  @Override
  protected final Object execute() {
    String name = getClass().getSimpleName();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(0, concurrencyLimit, 500L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(queueCapacity), new NexusThreadFactory(name, name), new CallerRunsPolicy());

    try (ProgressLogIntervalHelper progress = new ProgressLogIntervalHelper(log, 60)) {
      jobStream(progress).forEach(runnable -> {
        // check cancellation before scheduling job so the primary thread throws an exception and stops queuing jobs
        CancelableHelper.checkCancellation();
        executor.submit(runnable);
      });

      while (!executor.isShutdown() && (executor.getActiveCount() > 0 || !executor.getQueue().isEmpty())) {
        Runnable runnable = executor.getQueue().poll();
        if (runnable != null) {
          runnable.run();
        }
        else {
          Thread.sleep(500L);
        }
        CancelableHelper.checkCancellation();
      }
      return result();
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      CancelableHelper.checkCancellation();
      throw new RuntimeException(e);
    }
    finally {
      executor.shutdownNow();
    }
  }

  protected abstract Object result();

  protected abstract Stream<Runnable> jobStream(ProgressLogIntervalHelper progress);

  private static void validate(final int concurrencyLimit, final int queueCapacity) {
    checkArgument(concurrencyLimit > 0, "concurrencyLimit must be larger than 0");
    checkArgument(queueCapacity > 0, "queueCapacity must be larger than 0");
  }
}
