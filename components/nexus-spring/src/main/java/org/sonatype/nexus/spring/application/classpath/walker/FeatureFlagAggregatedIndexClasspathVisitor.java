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
package org.sonatype.nexus.spring.application.classpath.walker;

import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.spring.application.classpath.components.FeatureFlagComponentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * !!!! DEPRECATED no longer a needed process with everything injected into spring now. This class should be
 * removed when the previous DI architecture is removed. Until then changes should primarily be done on the newer
 * "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "false", matchIfMissing = true)
@Named
@Singleton
public class FeatureFlagAggregatedIndexClasspathVisitor
    extends AbstractClasspathVisitor
    implements ClasspathVisitor
{
  private static final Logger LOG = LoggerFactory.getLogger(SisuAggregatedIndexClasspathVisitor.class);

  private final FeatureFlagComponentMap featureFlagComponentSet;

  @Inject
  public FeatureFlagAggregatedIndexClasspathVisitor(final FeatureFlagComponentMap featureFlagComponentSet) {
    this.featureFlagComponentSet = checkNotNull(featureFlagComponentSet);
  }

  @Override
  public String name() {
    return "Feature Flag Aggregated Index Classpath Visitor";
  }

  @Override
  protected void doVisit(
      final String path,
      final String applicationJarPath,
      final InputStream applicationJarInputStream)
  {
    List<String> components = toSimpleStringList(applicationJarInputStream);
    LOG.debug("Found feature flagged components: {}", components);
    featureFlagComponentSet.addComponents(applicationJarPath, components);
  }

  @Override
  protected boolean applies(final String path) {
    return path.endsWith("sonatype/org.sonatype.nexus.common.app.FeatureFlag");
  }
}
