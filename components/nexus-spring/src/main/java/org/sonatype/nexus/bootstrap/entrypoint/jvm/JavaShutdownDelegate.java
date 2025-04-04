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
package org.sonatype.nexus.bootstrap.entrypoint.jvm;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.entrypoint.jvm.JavaShutdownDelegate.AnotherPropertyCondition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
@Conditional(AnotherPropertyCondition.class)
public class JavaShutdownDelegate
    implements ShutdownDelegate
{
  @Override
  public void exit(final int code) {
    // expected use of System.exit()
    System.exit(code);
  }

  @Override
  public void halt(final int code) {
    // expected use of Runtime.halt()
    Runtime.getRuntime().halt(code);
  }

  public static class AnotherPropertyCondition
      implements Condition
  {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String nexusNoopShutdownDelegate = context.getEnvironment().getProperty("nexus.noop.shutdown.delegate");

      return nexusNoopShutdownDelegate == null || "false".equals(nexusNoopShutdownDelegate);
    }
  }
}
