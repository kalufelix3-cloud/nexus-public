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

package org.sonatype.nexus.repository.content.maintenance.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.test.util.Whitebox;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MaintenanceServiceImplTest
    extends TestSupport
{
  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private DeleteFolderService deleteFolderService;

  @Mock
  private ExecutorService executorService;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private ContentFacetStores contentFacetStores;

  @Mock
  private AssetStore assetStore;

  @Mock
  private ContentMaintenanceFacet contentMaintenanceFacet;

  private MaintenanceServiceImpl underTest;

  @Before
  public void setUp() {
    underTest = new MaintenanceServiceImpl(contentPermissionChecker, variableResolverAdapterManager,
        repositoryPermissionChecker, deleteFolderService, executorService, databaseCheck);
  }

  @Test
  public void test_MaintenanceServiceImpl_deleteAssets() {
    underTest = spy(underTest);
    doReturn(Set.of("asset1", "asset2")).when(underTest).deleteAsset(any(), any());

    AssetData asset1 = mock(AssetData.class);
    asset1.setAssetId(1);
    asset1.setPath("asset1");
    AssetData asset2 = mock(AssetData.class);
    asset2.setAssetId(2);
    asset2.setPath("asset2");

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacetSupport);
    when(contentFacetSupport.stores()).thenReturn(contentFacetStores);
    Whitebox.setInternalState(contentFacetStores, "assetStore", assetStore);
    when(assetStore.readAsset(anyInt()))
        .thenReturn(Optional.of(asset1))
        .thenReturn(Optional.of(asset2));

    Set<String> result = underTest.deleteAssets(repository, List.of(1, 2));

    assertEquals(2, result.size());
  }

  @Test
  public void test_MaintenanceServiceImpl_deleteAssets_EmptyAssets() {
    underTest = spy(underTest);
    doReturn(Set.of()).when(underTest).deleteAsset(any(), any());

    Set<String> result = underTest.deleteAssets(repository, List.of());

    assertEquals(0, result.size());
  }

  @Test
  public void testDeleteComponent() {
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    setupCommonMocks(component, format, asset, fluentComponent, fluentComponents, contentFacet,
        variableResolverAdapter);

    when(databaseCheck.isPostgresql()).thenReturn(true);
    doNothing().when(underTestSpy).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));

    Set<String> result = underTestSpy.deleteComponent(repository, component);

    verify(underTestSpy).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));
    assertEquals(Set.of("asset1", "asset2"), result);
  }

  @Test
  public void testDeleteComponentNotPostgres() {
    MaintenanceServiceImpl underTestSpy = spy(underTest);
    Component component = mock(Component.class);
    Format format = mock(Format.class);
    FluentAsset asset = mock(FluentAsset.class);
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);

    setupCommonMocks(component, format, asset, fluentComponent, fluentComponents, contentFacet,
        variableResolverAdapter);

    when(databaseCheck.isPostgresql()).thenReturn(false);

    Set<String> result = underTestSpy.deleteComponent(repository, component);

    verify(underTestSpy, never()).deleteBrowseNode(any(Repository.class), any(FluentAsset.class));
    assertEquals(Set.of("asset1", "asset2"), result);
  }

  private void setupCommonMocks(
      Component component,
      Format format,
      FluentAsset asset,
      FluentComponent fluentComponent,
      FluentComponents fluentComponents,
      ContentFacet contentFacet,
      VariableResolverAdapter variableResolverAdapter)
  {
    when(format.getValue()).thenReturn("raw");
    when(asset.path()).thenReturn("/foo/bar");
    when(fluentComponent.assets()).thenReturn(Set.of(asset));
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(variableResolverAdapter.fromPath(anyString(), anyString())).thenReturn(mock(VariableSource.class));
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getName()).thenReturn("raw-repo");
    when(contentPermissionChecker.isPermitted(anyString(), anyString(), anyString(),
        any(VariableSource.class))).thenReturn(true);
    when(contentMaintenanceFacet.deleteComponent(any(Component.class))).thenReturn(Set.of("asset1", "asset2"));
    when(variableResolverAdapterManager.get(any())).thenReturn(variableResolverAdapter);
  }
}
