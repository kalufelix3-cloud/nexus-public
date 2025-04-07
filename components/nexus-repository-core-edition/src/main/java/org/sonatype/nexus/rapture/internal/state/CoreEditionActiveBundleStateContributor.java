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
package org.sonatype.nexus.rapture.internal.state;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * !!!! DEPRECATED in favor of {@link org.sonatype.nexus.rapture.internal.BundleStateContributor} in nexus-rapture
 * module, now that our DI framework has solidified around spring, we can go back to a dynamic module listing. This
 * class should be removed when the previous DI architecture is removed. Until then changes should primarily be done
 * on the newer "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "false", matchIfMissing = true)
public class CoreEditionActiveBundleStateContributor
    implements StateContributor
{
  public static final String STATE_ID = "activeBundles";

  private final Set<String> activeBundles = new ConcurrentSkipListSet<>();

  @Inject
  public CoreEditionActiveBundleStateContributor() {
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-audit-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-blobstore-s3");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-blobstore-tasks");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-coreui-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-default-role-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-onboarding-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-repository-httpbridge");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-script-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-ssl-plugin");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-task-log-cleanup");

    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-repository-apt");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-repository-maven");
    this.activeBundles.add("org.sonatype.nexus.plugins.nexus-repository-raw");
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(STATE_ID, activeBundles);
  }
}
