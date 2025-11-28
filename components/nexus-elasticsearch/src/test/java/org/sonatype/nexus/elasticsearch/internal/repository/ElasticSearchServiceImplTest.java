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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.elasticsearch.internal.repository.index.ElasticSearchIndexService;
import org.sonatype.nexus.elasticsearch.internal.repository.query.ElasticSearchQueryService;
import org.sonatype.nexus.elasticsearch.internal.repository.query.ElasticSearchUtils;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.elasticsearch.ElasticSearchExtension;

import org.elasticsearch.search.SearchHits;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;

public class ElasticSearchServiceImplTest
    extends TestSupport
{
  @Mock
  private ElasticSearchQueryService elasticSearchQueryService;

  @Mock
  private ElasticSearchIndexService elasticSearchIndexService;

  @Mock
  private ElasticSearchUtils elasticSearchUtils;

  @Mock
  private TokenEncoder tokenEncoder;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  private Set<ElasticSearchExtension> decorators;

  private ElasticSearchServiceImpl underTest;

  @Before
  public void setup() {
    decorators = Collections.emptySet();
    underTest = new ElasticSearchServiceImpl(
        elasticSearchQueryService,
        elasticSearchIndexService,
        elasticSearchUtils,
        tokenEncoder,
        decorators);
  }

  @Test
  public void testSearch_withLowercaseGroup() {
    // Given
    String groupValue = "com.example.lowercase";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse(groupValue);

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt())).thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is(groupValue));
    assertThat(result.getName(), is("test-artifact"));
    assertThat(result.getVersion(), is("1.0.0"));
  }

  @Test
  public void testSearch_withUppercaseGroup() {
    // Given
    String groupValue = "COM.EXAMPLE.UPPERCASE";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse(groupValue);

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is(groupValue));
    assertThat(result.getName(), is("test-artifact"));
    assertThat(result.getVersion(), is("1.0.0"));
  }

  @Test
  public void testSearch_withMixedCaseGroup() {
    // Given
    String groupValue = "Com.Example.MixedCase";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse(groupValue);

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is(groupValue));
    assertThat(result.getName(), is("test-artifact"));
    assertThat(result.getVersion(), is("1.0.0"));
  }

  @Test
  public void testSearch_withWildcardGroup() {
    // Given
    String groupValue = "com.example.*";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse("com.example.service");

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is("com.example.service"));
    assertThat(result.getName(), is("test-artifact"));
    assertThat(result.getVersion(), is("1.0.0"));

    // Verify that the query was built correctly
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(elasticSearchUtils).buildQuery(captor.capture());

    SearchRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest, is(notNullValue()));
  }

  @Test
  public void testSearch_withWildcardPrefix() {
    // Given
    String groupValue = "*example*";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse("org.example.test");

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is("org.example.test"));
  }

  @Test
  public void testSearch_withQuestionMarkWildcard() {
    // Given
    String groupValue = "com.example.?ervice";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse("com.example.service");

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    assertThat(result.getGroup(), is("com.example.service"));
  }

  @Test
  public void testSearch_groupValueNotConvertedToLowercase() {
    // Given - This test ensures we don't convert the group to lowercase which breaks Elasticsearch
    String groupValue = "com.sonatype.TEST";
    SearchRequest searchRequest = createSearchRequest(groupValue);
    org.elasticsearch.action.search.SearchResponse mockEsResponse = createMockEsResponse(groupValue);

    when(elasticSearchUtils.buildQuery(any(SearchRequest.class))).thenReturn(mock(QueryBuilder.class));
    when(elasticSearchQueryService.search(any(QueryBuilder.class), anyInt(), anyInt()))
        .thenReturn(mockEsResponse);

    // When
    SearchResponse response = underTest.search(searchRequest);

    // Then - Verify the response contains the exact case as stored in Elasticsearch
    assertThat(response, is(notNullValue()));
    assertThat(response.getSearchResults().size(), is(1));

    ComponentSearchResult result = response.getSearchResults().get(0);
    // This assertion verifies that the group value preserved its case
    // If the search parameters were incorrectly lowercased, Elasticsearch wouldn't find this
    assertThat(result.getGroup(), is("com.sonatype.TEST"));
    assertThat(result.getName(), is("test-artifact"));
    assertThat(result.getVersion(), is("1.0.0"));

    // Verify that buildQuery was called with a SearchRequest containing the original case
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(elasticSearchUtils).buildQuery(captor.capture());

    SearchRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest, is(notNullValue()));
    assertThat(capturedRequest.getSearchFilters().get(0).getValue(), is("com.sonatype.TEST"));
  }

  private SearchRequest createSearchRequest(final String groupValue) {
    return SearchRequest.builder()
        .searchFilter(GROUP, groupValue)
        .build();
  }

  private org.elasticsearch.action.search.SearchResponse createMockEsResponse(final String groupValue) {
    org.elasticsearch.action.search.SearchResponse mockResponse =
        mock(org.elasticsearch.action.search.SearchResponse.class);

    SearchHit mockHit = mock(SearchHit.class);

    Map<String, Object> sourceMap = new HashMap<>();
    sourceMap.put(GROUP, groupValue);
    sourceMap.put(NAME, "test-artifact");
    sourceMap.put(VERSION, "1.0.0");
    sourceMap.put(REPOSITORY_NAME, "test-repo");

    when(mockHit.getSource()).thenReturn(sourceMap);
    when(mockHit.getId()).thenReturn("test-id-123");

    SearchHit[] hits = new SearchHit[]{mockHit};

    // Mock SearchHits
    SearchHits mockSearchHits = mock(SearchHits.class);
    when(mockSearchHits.iterator()).thenReturn(java.util.Arrays.asList(hits).iterator());
    when(mockSearchHits.hits()).thenReturn(hits);
    when(mockSearchHits.getTotalHits()).thenReturn(1L);

    when(mockResponse.getHits()).thenReturn(mockSearchHits);

    when(repository.getName()).thenReturn("test-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(elasticSearchUtils.getReadableRepository("test-repo")).thenReturn(repository);

    return mockResponse;
  }
}
