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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.config.DefaultCleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.internal.preview.CsvCleanupPreviewContentWriter;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.rest.CleanupPolicyRequestValidator;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class CleanupPolicyResourceTest
    extends Test5Support
{
  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private List<Format> formats;

  private Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap;

  @Mock
  private Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EventManager eventManager;

  @Mock
  private CsvCleanupPreviewContentWriter csvCleanupPreviewContentWriter;

  @Mock
  private CleanupPolicyRequestValidator cleanupPolicyValidator;

  @Mock
  private Format mockFormat;

  private Collection<CleanupPolicyRequestValidator> cleanupPolicyValidators;

  private CleanupPolicyResource underTest;

  private final String repositoryName = "test-repo";

  private MockedStatic<QualifierUtil> mockedStatic;

  @BeforeEach
  void setUp() throws Exception {
    mockedStatic = mockStatic(QualifierUtil.class);
    cleanupFormatConfigurationMap =
        Map.of(DefaultCleanupPolicyConfiguration.NAME, mock(CleanupPolicyConfiguration.class));
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<CleanupPolicyConfiguration>>any()))
        .thenReturn(cleanupFormatConfigurationMap);
    Repository repository = mock(Repository.class);
    when(repositoryManager.get(repositoryName)).thenReturn(repository);
    when(repository.getName()).thenReturn(repositoryName);
    when(repository.getFormat()).thenReturn(mockFormat);
    when(mockFormat.getValue()).thenReturn("test-format");
    cleanupPolicyValidators = singleton(cleanupPolicyValidator);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void testPreviewContentCsv() {
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    String expectedPrefix = "attachment; filename=CleanupPreview-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));

    response = underTest.previewContentCsv("policy-name", repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    expectedPrefix = "attachment; filename=policy-name-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));
  }

  @Test
  void testPreviewContentCsvWithSpecialCharactersInRegex() {
    underTest =
        new CleanupPolicyResource(
            cleanupPolicyStorage,
            formats,
            List.of(),
            cleanupPreviewHelper,
            repositoryManager,
            eventManager,
            true,
            csvCleanupPreviewContentWriter,
            cleanupPolicyValidators);

    // Test regex with curly braces (quantifiers)
    Response response = underTest.previewContentCsv(
        "test-policy",
        repositoryName,
        null,
        null,
        null,
        "[a-zA-Z0-9]{40}",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=test-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with various special characters
    response = underTest.previewContentCsv(
        "special-chars-policy",
        repositoryName,
        null,
        null,
        null,
        ".*\\.(jar|war|zip)$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=special-chars-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));

    // Test regex with character classes and ranges
    response = underTest.previewContentCsv(
        "complex-regex-policy",
        repositoryName,
        null,
        null,
        null,
        "^[0-9]{3,5}\\.[a-z]+\\.(txt|log)$",
        null,
        null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    assertThat(contentDisposition, startsWith("attachment; filename=complex-regex-policy-" + repositoryName));
    assertThat(contentDisposition, endsWith(".csv"));
  }
}
