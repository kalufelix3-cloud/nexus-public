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

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.AbstractModule;
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.RankingFunction;

/**
 * !!!! DEPRECATED this functionality will not get replicatd in this fashion, no need to add new class. This class
 * should be removed when the previous DI architecture is removed. Until then changes should primarily be done on
 * the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class RankingModule
    extends AbstractModule
{
  private final AtomicInteger rank = new AtomicInteger(1);

  @Override
  protected void configure() {
    bind(RankingFunction.class).toInstance(new DefaultRankingFunction(rank.incrementAndGet()));
  }
}
