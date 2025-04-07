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

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;

/**
 * !!!! DEPRECATED no longer a needed process with everything injected into spring now. This class should be
 * removed when the previous DI architecture is removed. Until then changes should primarily be done on the newer
 * "nexus.spring.only=true" impl, then only brought back to this class if necessary
 */
@Deprecated(since = "4/1/2025", forRemoval = true)
public abstract class AbstractClasspathVisitorTest<T extends ClasspathVisitor>
{
  private static final String CACHE_FILE_BASE =
      "target/test-classes/org/sonatype/nexus/spring/application/classpath/walker/";

  private T underTest;

  private final ApplicationJarFilter applicationJarFilter = new AllowAllApplicationJarFilter();

  @Before
  public void setup() {
    underTest = newInstance();
  }

  @Test
  public void testCacheAggregatedIndex_allNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getAllNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar);

    assertAllNestedComponentsAggregatedIndex();
  }

  @Test
  public void testCacheAggregatedIndex_someNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getSomeNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar);
    assertSomeNestedComponentsAggregatedIndex();
  }

  @Test
  public void testCacheAggregatedIndex_noNestedComponentJars() throws Exception {
    File testJar = new File(CACHE_FILE_BASE + getNoNestedComponentsJarName());
    new ClasspathWalker(singletonList(underTest), applicationJarFilter).walk(testJar);

    assertNoNestedComponentsAggregatedIndex();
  }

  protected abstract T newInstance();

  protected abstract String getAllNestedComponentsJarName();

  protected abstract String getSomeNestedComponentsJarName();

  protected abstract String getNoNestedComponentsJarName();

  protected abstract void assertAllNestedComponentsAggregatedIndex();

  protected abstract void assertSomeNestedComponentsAggregatedIndex();

  protected abstract void assertNoNestedComponentsAggregatedIndex();
}
