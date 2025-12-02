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
package org.sonatype.nexus.repository.content.browse.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorFilterBuilder;
import org.sonatype.nexus.selector.SelectorManager;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowseNodeQueryServiceImplTest
    extends TestSupport
{
  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  @Mock
  private SelectorFilterBuilder selectorFilterBuilder;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private BrowseFacet browseFacet;

  private BrowseNodeComparator defaultBrowseNodeComparator;

  private BrowseNodeQueryServiceImpl underTest;

  @Before
  public void setup() {
    defaultBrowseNodeComparator = mock(BrowseNodeComparator.class, DefaultBrowseNodeComparator.NAME);

    underTest = new BrowseNodeQueryServiceImpl(
        securityHelper,
        selectorManager,
        emptyList(),
        emptyList(),
        asList(defaultBrowseNodeComparator),
        contentAuthHelper,
        selectorFilterBuilder);

    when(repository.getName()).thenReturn("test-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));
  }

  @Test
  public void testNoAccessWhenNoValidFilteringAvailable() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), any(Integer.class), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    SelectorConfiguration unknownSelector = mock(SelectorConfiguration.class);
    when(unknownSelector.getType()).thenReturn("UNKNOWN_TYPE");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(unknownSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn(null);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(result, is(emptyIterable()));
  }

  @Test
  public void testAccessGrantedWhenUserHasFullBrowsePermission() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), any(Integer.class), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  public void testJexlFilteringAppliedWhenNoSeclSelectors() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), any(Integer.class), any(), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    SelectorConfiguration jexlSelector = mock(SelectorConfiguration.class);
    when(jexlSelector.getType()).thenReturn("jexl");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(jexlSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn(null);

    when(contentAuthHelper.checkPathPermissionsJexlOnly(anyString(), anyString(), anyString()))
        .thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }

  @Test
  public void testSeclFilteringAppliedWhenNoJexlSelectors() {
    BrowseNode mockNode = mock(BrowseNode.class);
    when(mockNode.getPath()).thenReturn("/test/path");

    when(browseFacet.getByDisplayPath(anyList(), any(Integer.class), eq("filter"), any()))
        .thenReturn(new ArrayList<>(asList(mockNode)));

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(false);

    SelectorConfiguration seclSelector = mock(SelectorConfiguration.class);
    when(seclSelector.getType()).thenReturn("csel");
    when(selectorManager.browseActive(anyList(), anyList())).thenReturn(asList(seclSelector));

    when(selectorFilterBuilder.buildFilter(anyString(), anyString(), anyList(), any()))
        .thenReturn("filter");

    when(contentAuthHelper.checkPathPermissions(anyString(), anyString(), anyString()))
        .thenReturn(true);

    Iterable<BrowseNode> result = underTest.getByPath(repository, asList("test"), 100);

    assertThat(((List<BrowseNode>) result).size(), is(1));
  }
}
