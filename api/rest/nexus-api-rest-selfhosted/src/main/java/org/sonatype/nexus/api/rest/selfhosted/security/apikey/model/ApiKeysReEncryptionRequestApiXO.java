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
package org.sonatype.nexus.api.rest.selfhosted.security.apikey.model;

import javax.annotation.Nullable;

import io.swagger.annotations.ApiModelProperty;

public class ApiKeysReEncryptionRequestApiXO
{
  @ApiModelProperty("Optional - The current password used to encrypt the principals")
  @Nullable
  private String password;

  @ApiModelProperty("Optional - The current password used to encrypt the principals")
  @Nullable
  private String salt;

  @ApiModelProperty("Optional - The current IV used to encrypt the principals")
  @Nullable
  private String iv;

  @ApiModelProperty("Optional - The current algorithm used to encrypt the principals")
  @Nullable
  private String algorithm;

  @ApiModelProperty("Optional - Email to notify when task finishes")
  @Nullable
  private String notifyEmail;

  public ApiKeysReEncryptionRequestApiXO() {
    // serialization
  }

  public ApiKeysReEncryptionRequestApiXO(
      @Nullable final String password,
      @Nullable final String salt,
      @Nullable final String iv,
      @Nullable final String algorithm,
      @Nullable final String notifyEmail)
  {
    this.password = password;
    this.salt = salt;
    this.iv = iv;
    this.algorithm = algorithm;
    this.notifyEmail = notifyEmail;
  }

  @Nullable
  public String getPassword() {
    return password;
  }

  @Nullable
  public String getSalt() {
    return salt;
  }

  @Nullable
  public String getIv() {
    return iv;
  }

  @Nullable
  public String getAlgorithm() {
    return algorithm;
  }

  @Nullable
  public String getNotifyEmail() {
    return notifyEmail;
  }
}
