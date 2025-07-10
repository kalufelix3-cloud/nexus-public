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
package org.sonatype.nexus.api.rest.selfhosted.security.apikey;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.api.rest.selfhosted.security.apikey.model.ApiKeysReEncryptionRequestApiXO;
import org.sonatype.nexus.crypto.apikey.ApiKeysReEncryptService;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class ApiKeysEncryptionApiResourceV1Test
    extends TestSupport
{
  @Mock
  private ApiKeysReEncryptService apiKeysReEncryptService;

  private ApiKeysEncryptionApiResourceV1 underTest;

  @Before
  public void setup() {
    underTest = new ApiKeysEncryptionApiResourceV1(apiKeysReEncryptService);
  }

  @Test
  public void testEncrypt() {
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO("password", "salt", "iv", "algorithm", null);
    Response response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

    request = new ApiKeysReEncryptionRequestApiXO("password", "salt", "iv", null, null);
    response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

    request = new ApiKeysReEncryptionRequestApiXO("password", "salt", null, null, null);
    response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

    request = new ApiKeysReEncryptionRequestApiXO("password", null, null, null, null);
    response = underTest.reEncrypt(request);
    assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
  }

  @Test
  public void testEncrypt_InvalidRequest() {
    ApiKeysReEncryptionRequestApiXO request = new ApiKeysReEncryptionRequestApiXO(null, null, null, null, null);

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class, () -> underTest.reEncrypt(request));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("Invalid request parameters");
  }

  @Test
  public void testEncrypt_ReEncryptionIsNotSupported() {
    doThrow(new ReEncryptionNotSupportedException("Re-encryption api key principals is not supported"))
        .when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any(), any(), any());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class,
            () -> underTest.reEncrypt(new ApiKeysReEncryptionRequestApiXO("password", null, null, null, null)));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("Re-encryption api key principals is not supported");
  }

  @Test
  public void testEncrypt_ReEncryptionIsRunning() {
    doThrow(new IllegalStateException("Re-encryption principals task is already running")).when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any(), any(), any());

    WebApplicationMessageException exception =
        assertThrows(WebApplicationMessageException.class,
            () -> underTest.reEncrypt(new ApiKeysReEncryptionRequestApiXO("password", null, null, null, null)));

    assertThat(exception.getResponse().getStatus()).isEqualTo(Status.CONFLICT.getStatusCode());
    ValidationErrorXO error = (ValidationErrorXO) exception.getResponse().getEntity();
    assertThat(error.getMessage()).isEqualTo("Re-encryption principals task is already running");
  }

  @Test
  public void testEncrypt_UnexpectedError() {
    doThrow(new RuntimeException("unexpected error")).when(apiKeysReEncryptService)
        .submitReEncryption(any(), any(), any(), any(), any());
    ApiKeysReEncryptionRequestApiXO request =
        new ApiKeysReEncryptionRequestApiXO("password", null, null, null, null);
    assertThrows(RuntimeException.class, () -> underTest.reEncrypt(request));
  }
}
