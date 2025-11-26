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
package org.sonatype.nexus.testdb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.type.TypeHandler;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSessionConfiguration
{
  Class<? extends DataAccess>[] daos() default {};

  Class<? extends TypeHandler<?>>[] typeHandlers() default {};

  /**
   * Can be used to prevent the H2 database from being used for all tests. Individual tests may disable H2 via the
   * {@link DatabaseTest} annotation.
   */
  boolean h2() default true;

  /**
   * Can be used to prevent the PostgreSQL database from being used for all tests. Individual tests may disable
   * PostgreSQL via the {@link DatabaseTest} annotation.
   */
  boolean postgresql() default true;
}
