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

import org.sonatype.nexus.security.JwtHelper;

import com.google.inject.AbstractModule;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * !!!! DEPRECATED in favor of a new spring @Configuration class. This class should be removed when the previous DI
 * architecture is removed. Until then changes should primarily be done on the newer "nexus.spring.only=true" impl,
 * then only brought back to this class if necessary
 * -------------------------------------------------------
 * Old javadoc
 * SecurityFilter support bindings.
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public class SecurityFilterModule
    extends AbstractModule
{
  private final Map<?, ?> nexusProperties;

  public SecurityFilterModule(Map<?, ?> nexusProperties) {
    this.nexusProperties = nexusProperties;
  }
  // handle some edge-cases for commonly used filter-based components which need a bit
  // more configuration so that sisu/guice can find the correct bindings inside of plugins

  @Override
  protected void configure() {
    requireBinding(FilterChainResolver.class);

    if (parseBoolean((String) nexusProperties.get(JWT_ENABLED))) {
      requireBinding(JwtHelper.class);
    }
  }
}
