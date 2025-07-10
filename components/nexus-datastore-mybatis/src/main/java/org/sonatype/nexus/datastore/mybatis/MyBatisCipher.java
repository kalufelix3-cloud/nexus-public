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
package org.sonatype.nexus.datastore.mybatis;

import org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactoryImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;
import org.sonatype.nexus.security.PasswordHelper;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.sisu.Hidden;
import org.eclipse.sisu.Typed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.fromBase64;

/**
 * Database cipher shared between all MyBatis handlers that want to encrypt data at rest.
 * <p>
 * Note this is different from nested password attributes which should be encrypted with
 * {@link PasswordHelper}, this is about encrypting the entire content of a database cell.
 *
 * @since 3.21
 */
@Component
@Qualifier("mybatis")
@Singleton
@Typed(PbeCipher.class)
@Hidden // don't publish this to other modules
final class MyBatisCipher
    implements PbeCipher
{
  private final PbeCipherFactory.PbeCipher pbeCipher;

  @Inject
  MyBatisCipher(
      @Value("${nexus.mybatis.cipher.password:changeme}") final String password,
      @Value("${nexus.mybatis.cipher.salt:changeme}") final String salt,
      @Value("${nexus.mybatis.cipher.iv:0123456789ABCDEF}") final String iv,
      final PbeCipherFactory pbeCipherFactory)
  {
    SecretEncryptionKey secretKey = new SecretEncryptionKey(null, password);
    this.pbeCipher = checkNotNull(pbeCipherFactory).create(secretKey, salt, iv);
  }

  /**
   * Static configuration for testing purposes.
   */
  @VisibleForTesting
  MyBatisCipher() {
    this("changeme", "changeme", "0123456789ABCDEF",
        new PbeCipherFactoryImpl(new CryptoHelperImpl(false),
            new HashingHandlerFactoryImpl(new CryptoHelperImpl(false)), "PBKDF2WithHmacSHA1"));
  }

  @Override
  public byte[] encrypt(final byte[] bytes) {
    EncryptedSecret encryptedSecret = pbeCipher.encrypt(bytes);
    String base64Value = encryptedSecret.getValue();
    return fromBase64(base64Value);
  }

  @Override
  public byte[] decrypt(final byte[] bytes) {
    return pbeCipher.decrypt(bytes);
  }
}
