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
package org.sonatype.nexus.elasticsearch.internal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.elasticsearch.internal.repository.index.ElasticSearchIndexService;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.search.elasticsearch.DefaultSearchDocumentProducer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SearchFacetImplTest
    extends Test5Support
{
  @Mock
  FluentComponents fluentComponents;

  @Mock
  ContentFacet content;

  @Mock
  FluentComponent component;

  @Mock(answer = Answers.RETURNS_MOCKS)
  Format format;

  @Mock
  Repository repository;

  @Mock
  ElasticSearchIndexService elasticSearchIndexService;

  @Captor
  ArgumentCaptor<Iterable<FluentComponent>> captor;

  SearchFacetImpl underTest;

  @BeforeEach
  void setup() throws Exception {
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("maven-central");
    lenient().when(repository.facet(ContentFacet.class)).thenReturn(content);
    lenient().when(content.components()).thenReturn(fluentComponents);

    underTest = new SearchFacetImpl(elasticSearchIndexService, List.of(new DefaultSearchDocumentProducer(Set.of())),
        1000, true);
    underTest.installDependencies(mock(EventManager.class));
    underTest.attach(repository);
    underTest.init();
    underTest.start();
  }

  @Test
  void testIndex() {
    when(fluentComponents.find(any())).thenReturn(Optional.of(component), Optional.empty());

    List<FluentComponent> indexed = new ArrayList<>();
    List<String> deleted = new ArrayList<>();

    when(elasticSearchIndexService.bulkPut(any(), any(), any(), any())).thenAnswer(i -> {
      i.getArgument(1, Iterable.class).forEach(component -> indexed.add((FluentComponent) component));
      return null;
    });

    doAnswer(i -> {
      i.getArgument(1, Iterable.class).forEach(component -> deleted.add((String) component));
      return null;
    }).when(elasticSearchIndexService).bulkDelete(any(), any());

    underTest.index(List.of(mockEntityId(0), mockEntityId(1)));

    // startup
    verify(elasticSearchIndexService).createIndex(repository);
    // known component should be indexed and saved
    verify(elasticSearchIndexService).bulkPut(any(), any(), any(), any());
    assertThat(indexed, contains(component));
    // unknown component should be deleted
    verify(elasticSearchIndexService).bulkDelete(any(), any());
    assertThat(deleted, contains("1"));
    verifyNoMoreInteractions(elasticSearchIndexService);
  }

  private static EntityId mockEntityId(final int id) {
    EntityId entityId = mock(EntityId.class);
    lenient().when(entityId.getValue()).thenReturn(String.valueOf(id));
    return entityId;
  }
}
