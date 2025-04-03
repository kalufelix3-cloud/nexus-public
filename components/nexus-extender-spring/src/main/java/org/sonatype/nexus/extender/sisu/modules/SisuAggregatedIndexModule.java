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
package org.sonatype.nexus.extender.sisu.modules;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sonatype.nexus.spring.application.classpath.finder.NexusSisuAggregatedIndexClassFinder;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.MembersInjectorLookup;
import com.google.inject.spi.PrivateElements;
import com.google.inject.spi.ProviderLookup;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.QualifiedTypeBinder;
import org.eclipse.sisu.space.QualifiedTypeVisitor;
import org.eclipse.sisu.space.SpaceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A replica of the SpaceModule that utilizes an aggregated index file, consisting of many typical sisu index files
 */
public class SisuAggregatedIndexModule
    implements Module
{
  private static final Logger log = LoggerFactory.getLogger(SisuAggregatedIndexModule.class);

  private final ClassSpace space;

  private final NexusSisuAggregatedIndexClassFinder finder;

  private final ConcurrentMap<String, List<Element>> cache = new ConcurrentHashMap<String, List<Element>>();

  public SisuAggregatedIndexModule(final ClassSpace space, final NexusSisuAggregatedIndexClassFinder finder) {
    this.space = space;
    this.finder = finder;
  }

  public void configure(final Binder binder) {
    log.info("Binding sisu aggregated index classes");
    binder.bind(ClassSpace.class).toInstance(space);

    recordAndReplayElements(binder);
  }

  private void recordAndReplayElements(final Binder binder) {
    final String key = space.toString();
    List<Element> elements = cache.get(key);
    if (null == elements) {
      // record results of scanning plus any custom module bindings
      final List<Element> recording = Elements.getElements(new Module()
      {
        public void configure(final Binder recorder) {
          scanForElements(recorder);
        }
      });
      elements = cache.putIfAbsent(key, recording);
      if (null == elements) {
        // shortcut, no need to reset state first time round
        Elements.getModule(recording).configure(binder);
        return;
      }
    }

    replayRecordedElements(binder, elements);
  }

  private void scanForElements(final Binder binder) {
    new SpaceScanner(space, finder).accept(new QualifiedTypeVisitor(new QualifiedTypeBinder(binder)));
  }

  private void replayRecordedElements(final Binder binder, final List<Element> elements) {
    log.info("enter replayRecordedElements, elements size {}", elements.size());
    for (final Element e : elements) {
      // lookups have state so we replace them with duplicates when replaying...
      if (e instanceof ProviderLookup<?>) {
        binder.getProvider(((ProviderLookup<?>) e).getKey());
      }
      else if (e instanceof MembersInjectorLookup<?>) {
        binder.getMembersInjector(((MembersInjectorLookup<?>) e).getType());
      }
      else if (e instanceof PrivateElements) {
        // Follows example set by Guice Modules when applying private elements:
        final PrivateElements privateElements = (PrivateElements) e;

        // 1. create new private binder, using the elements source token
        final PrivateBinder privateBinder = binder.withSource(e.getSource()).newPrivateBinder();

        // 2. for all elements, apply each element to the private binder
        replayRecordedElements(privateBinder, privateElements.getElements());

        // 3. re-expose any exposed keys using their exposed source token
        for (final Key<?> k : privateElements.getExposedKeys()) {
          privateBinder.withSource(privateElements.getExposedSource(k)).expose(k);
        }
      }
      else {
        e.applyTo(binder);
      }
    }
  }
}
