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
package org.sonatype.nexus.common.upgrade;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.guice.AbstractInterceptorModule;

import com.google.inject.matcher.Matchers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * !!!! DEPRECATED in favor of Spring's `@Configuration` annotation, see the spring dev doc
 * {@link{private/developer-documentation/architecture/spring.md} for more details, being removed as this was guice
 * specific handling. This class should be removed when the previous DI architecture is removed. Until then changes
 * should primarily be done on the newer "nexus.spring.only=true" impl, then only brought back to this class if
 * necessary
 * -------------------------------------------------------
 * Old javadoc
 * Guice module for configuring upgrade-related interceptors.
 * This module binds an interceptor to methods annotated with {@link AvailabilityVersion}.
 * The interceptor is an instance of {@link AvailabilityVersionCheckerInterceptor} which is provided
 * with a {@link DatabaseCheck} instance.
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "false", matchIfMissing = true)
public class UpgradeModule
    extends AbstractInterceptorModule
{
  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(AvailabilityVersion.class),
        new AvailabilityVersionCheckerInterceptor(getProvider(DatabaseCheck.class)));
  }
}
