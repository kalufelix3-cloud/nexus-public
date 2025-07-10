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
package org.sonatype.nexus.crypto.internal;

import java.security.SecureRandom;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl.PbeCipherImpl;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PbeCipherFactoryImplTest
{
  private SecretEncryptionKey encryptionKey;

  private PbeCipherFactoryImpl factory;

  @BeforeEach
  void setUp() {
    CryptoHelper cryptoHelper = mock(CryptoHelper.class);
    when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
    encryptionKey = mock(SecretEncryptionKey.class);
    when(encryptionKey.getKey()).thenReturn("test-secret-key");
    HashingHandlerFactory hashingHandlerFactory = mock(HashingHandlerFactory.class);
    when(hashingHandlerFactory.create("PBKDF2WithSHA1")).thenReturn(mock(HashingHandler.class));

    factory = new PbeCipherFactoryImpl(cryptoHelper, hashingHandlerFactory, "PBKDF2WithSHA1");
  }

  @Test
  void testCreate_DefaultAlgorithm_Pbkdf2Sha1() {
    PbeCipher cipher = factory.create(encryptionKey);
    assertNotNull(cipher);
    assertInstanceOf(PbeCipherImpl.class, cipher);
    assertInstanceOf(cipher.getClass(), cipher);
    assertTrue(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_when_Sha256Algorithm_input_creates_Pbkdf2Sha256() {
    String encoded =
        "$pbkdf2-sha256$iv=a6f7e545dc07ae8b0c5ff522d58b5994,key_iteration=10000,key_len=256$9+gAr77ZJtvlpDZm7Av1bg==$Z7yqZ3ok5JyAFYjoJ9lo+p2G1GZFUX9kqnDEJFHeErg=";

    PbeCipher cipher = factory.create(encryptionKey, encoded);
    assertNotNull(cipher);
    assertFalse(cipher.isDefaultCipher());
  }

  @Test
  void testCreate_UnsupportedAlgorithm_ShouldThrow() {
    String encoded = "$unsupportedabcdef0123456789$16salt";
    assertThrows(IllegalArgumentException.class, () -> factory.create(encryptionKey, encoded));
  }
}
