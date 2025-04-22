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

package org.sonatype.nexus.extender.guice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.webresources.UrlWebResource;

import com.google.common.collect.Lists;
import org.eclipse.sisu.space.ClassSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.enumeration;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

public class StaticWebResourceTest
    extends TestSupport
{
  @Mock
  private MimeSupport mimeSupport;

  @Mock
  UrlWebResource urlWebResource;

  @Mock
  private ClassSpace space;

  @Before
  public void setUp() throws Exception {
    when(urlWebResource.getPath()).thenReturn("mock/path");
    when(urlWebResource.getContentType()).thenReturn("mock/type");
    when(urlWebResource.getSize()).thenReturn(123L);
  }

  private static final String BASE_JAR_PATH = "jar:file:/path-to/SomeJar.jar!";

  @Test
  public void getResources_shouldIncludeResourcesFromDesignatedResourcePaths() throws IOException {
    final Enumeration<URL> givenResourcesInStaticPath = enumeration(Lists.newArrayList(
        mockURL("%s/static/img.png".formatted(BASE_JAR_PATH)),
        mockURL("%s/static/some-file.js".formatted(BASE_JAR_PATH)),
        mockURL("%s/static/file.css".formatted(BASE_JAR_PATH))));
    when(space.findEntries("static", "*", true))
        .thenReturn(givenResourcesInStaticPath);

    final Enumeration<URL> givenResourcesInAssetsPath = enumeration(Lists.newArrayList(
        mockURL("%s/assets/font.woff".formatted(BASE_JAR_PATH)),
        mockURL("%s/assets/thing.jpeg".formatted(BASE_JAR_PATH))));
    when(space.findEntries("assets", "*", true))
        .thenReturn(givenResourcesInAssetsPath);

    when(mimeSupport.guessMimeTypeFromPath(anyString())).thenReturn("some/mime");
    final var staticWebResource = new StaticWebResource(space, mimeSupport);
    final var results = staticWebResource.getResources();

    assertThat(results).hasSize(5);
    assertThat(results.get(0).getPath()).isEqualTo("/static/img.png");
    assertThat(results.get(1).getPath()).isEqualTo("/static/some-file.js");
    assertThat(results.get(2).getPath()).isEqualTo("/static/file.css");
    assertThat(results.get(3).getPath()).isEqualTo("/assets/font.woff");
    assertThat(results.get(4).getPath()).isEqualTo("/assets/thing.jpeg");
  }

  // it's necessary to mock this so that the UrlWebResource constructor doesn't throw
  // an exception on openConnection because the file is not present
  private URL mockURL(String path) throws IOException {
    URL mockUrl = mock(URL.class);
    URLConnection mockConnection = mock(URLConnection.class);

    // Mock the behavior of URLConnection
    when(mockUrl.openConnection()).thenReturn(mockConnection);
    when(mockUrl.getPath()).thenReturn(path);
    when(mockUrl.toExternalForm()).thenReturn(path);
    when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(mockConnection.getContentType()).thenReturn("text/plain");
    when(mockConnection.getContentLengthLong()).thenReturn(123L);
    when(mockConnection.getLastModified()).thenReturn(456L);

    return mockUrl;
  }
}
