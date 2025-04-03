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
package org.sonatype.nexus.spring.application.classpath.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public abstract class AbstractComponentMap<T>
    implements ComponentMap<T>
{
  private final Map<String, List<T>> componentMap = new LinkedHashMap<>();

  @Override
  public void addComponent(final String module, final T component) {
    componentMap.computeIfAbsent(module, k -> new ArrayList<>()).add(component);
  }

  @Override
  public void addComponents(final String module, final List<T> components) {
    componentMap.computeIfAbsent(module, k -> new ArrayList<>()).addAll(components);
  }

  @Override
  public List<T> getComponents(final String module) {
    return componentMap.get(module);
  }

  @Override
  public List<T> getComponents() {
    return componentMap.values().stream().flatMap(List::stream).collect(toList());
  }
}
