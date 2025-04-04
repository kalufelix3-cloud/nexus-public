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
package org.sonatype.nexus.bootstrap.entrypoint.edition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static java.util.Collections.reverse;

@Named
@Singleton
@ConditionalOnProperty(value = "nexus.spring.only", havingValue = "true")
public class NexusEditionSelector
{
  public static final String PROPERTY_KEY = "nexus-edition";

  private final List<NexusEdition> editions;

  @Inject
  public NexusEditionSelector(final List<NexusEdition> editions) {
    this.editions = new ArrayList<>(editions);
    this.editions.sort(Comparator.comparingInt(NexusEdition::getPriority));
    reverse(this.editions);
  }

  @Bean("nexus.edition")
  public NexusEdition getCurrent() {
    return editions
        .stream()
        .filter(NexusEdition::isActive)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No active edition found"));
  }
}
