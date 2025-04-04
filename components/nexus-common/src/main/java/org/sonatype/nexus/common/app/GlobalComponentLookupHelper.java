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
package org.sonatype.nexus.common.app;

import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.inject.Key;

/**
 * !!!! DEPRECATED in favor of org.sonatype.nexus.bootstrap.entrypoint.ApplicationContextProvider in
 * `nexus-extender-spring` module, though this form of injection should ONLY be used when a class is instantiated
 * outside of spring's control. And this should only happen in ONE scenario, when jetty instantiates the
 * `NexusServletContextListener`.
 * THINK TWICE before using this class in ANY other way. This class should be removed when the previous DI
 * architecture is removed. Until then changes should primarily be done on the newer "nexus.spring.only=true" impl,
 * then only brought back to this class if necessary
 * -------------------------------------------------------
 * Old javadoc
 * Helper to lookup components in global context.
 * In a few places, components need to be looked up by class-name and need to use the uber class-loader to resolve
 * classes.
 * This helper contains this logic in one place for re-use.
 * 
 * @since 3.0
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public interface GlobalComponentLookupHelper
{
  /**
   * Lookup a component by class-name.
   *
   * @return Component reference, or {@code null} if the component was not found.
   */
  @Nullable
  Object lookup(String className);

  /**
   * Lookup a component by {@link Class}.
   *
   * @return Component reference, or {@code null} if the component was not found.
   * @since 3.6.1
   */
  <T> T lookup(Class<T> clazz);

  /**
   * Lookup a component by {@link Class} and @{@link Named} name.
   *
   * @return Component reference, or {@code null} if the component was not found.
   * @since 3.6.1
   */
  <T> T lookup(Class<T> clazz, String name);

  /**
   * Lookup a component by {@link Key}.
   *
   * @return Component reference, or {@code null} if the component was not found.
   * @since 3.6.1
   */
  Object lookup(Key key);

  /**
   * Lookup a type by class-name.
   *
   * @return Type reference, or {@code null} if the type was not found.
   */
  @Nullable
  Class<?> type(String className);
}
