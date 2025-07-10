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
package org.sonatype.nexus.internal.security.apikey.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.PbeCipherFactory.PbeCipher;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import jakarta.inject.Inject;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_SECRETS_ALGORITHM_NAMED_VALUE;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.fromBase64;
import static org.sonatype.nexus.crypto.internal.EncryptionHelper.toBase64;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@TaskLogging(TaskLogType.NEXUS_LOG_ONLY)
public class ReEncryptPrincipalsTask
    extends TaskSupport
    implements Cancelable
{
  private static final int INTERVAL_IN_SECONDS = 60;

  private static final String SELECT = """
      SELECT principals, domain, username, access_key
      FROM api_key_v2
      ORDER BY username, principals, domain
      LIMIT ? OFFSET ?
      """;

  private static final String UPDATE = """
      UPDATE api_key_v2
      SET principals = ?
      WHERE domain = ? AND username = ? AND access_key = ?
      """;

  private final DataSessionSupplier sessionSupplier;

  private final PbeCipherFactory pbeCipherFactory;

  private final String password;

  private final String salt;

  private final String iv;

  private final String nexusSecretsAlgorithm;

  @Inject
  public ReEncryptPrincipalsTask(
      final DataSessionSupplier sessionSupplier,
      final PbeCipherFactory pbeCipherFactory,
      @Value("${nexus.mybatis.cipher.password:changeme}") final String password,
      @Value("${nexus.mybatis.cipher.salt:changeme}") final String salt,
      @Value("${nexus.mybatis.cipher.iv:0123456789ABCDEF}") final String iv,
      @Value(NEXUS_SECURITY_SECRETS_ALGORITHM_NAMED_VALUE) final String nexusSecretsAlgorithm)
  {
    this.sessionSupplier = checkNotNull(sessionSupplier);
    this.pbeCipherFactory = checkNotNull(pbeCipherFactory);
    this.password = password;
    this.salt = salt;
    this.iv = iv;
    this.nexusSecretsAlgorithm = nexusSecretsAlgorithm;
  }

  @Override
  public String getMessage() {
    return "Re-encrypting principals";
  }

  @Override
  protected Object execute() throws Exception {
    long start = System.currentTimeMillis();
    log.info("Started re-encryption principals");
    String keyPasswordOld = getValueFromTaskConfiguration("password", password);
    String saltOld = getValueFromTaskConfiguration("salt", salt);
    String ivOld = getValueFromTaskConfiguration("iv", iv);
    String algorithmOld = getValueFromTaskConfiguration("algorithm", nexusSecretsAlgorithm);

    if (!thereAreChanges(keyPasswordOld, saltOld, ivOld, algorithmOld)) {
      log.info("Re-encryption principals is not needed, there are not changes.");
      return 0;
    }

    SecretEncryptionKey secretKeyToDecrypt = new SecretEncryptionKey(null, keyPasswordOld);
    PbeCipher cipherToEncrypt = pbeCipherFactory.create(new SecretEncryptionKey(null, password), salt, iv);

    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, INTERVAL_IN_SECONDS);
        Connection conn = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement select = conn.prepareStatement(SELECT);
        PreparedStatement update = conn.prepareStatement(UPDATE)) {
      int offset = 0;
      int processedCount = 0;
      int failedCount = 0;
      while (true) {
        select.setInt(1, Continuations.BROWSE_LIMIT);
        select.setInt(2, offset);
        try (ResultSet results = select.executeQuery()) {
          if (!results.isBeforeFirst()) {
            break;
          }

          while (results.next()) {
            String domain = results.getString("domain");
            String username = results.getString("username");
            String accessKey = results.getString("access_key");
            byte[] principals = results.getBytes("principals");
            try {
              byte[] decrypted = decrypt(secretKeyToDecrypt, algorithmOld, saltOld, ivOld, principals);
              byte[] encrypted = reEncrypt(cipherToEncrypt, decrypted);
              updatePrincipal(update, domain, username, accessKey, encrypted);
              processedCount++;
            }
            catch (Exception e) {
              failedCount++;
              log.error("An error occurred trying to re-encrypt principals of username: {}.", username, e);
            }
          }

          progressLogger.info("Re-encrypted principals {}, Failed principals {}.", processedCount, failedCount);
          offset += Continuations.BROWSE_LIMIT;
        }
      }

      long finish = System.currentTimeMillis();
      long timeElapsed = finish - start;
      log.info(
          "The re-encryption principals has been completed. Re-encrypted principals {}, Failed principals {}. It took {} milliseconds.",
          processedCount, failedCount, timeElapsed);
      return processedCount;
    }
  }

  private byte[] decrypt(
      final SecretEncryptionKey secretKeyToDecrypt,
      final String algorithmOld,
      final String saltOld,
      final String ivOld,
      final byte[] principalsBytes)
  {
    EncryptedSecret encryptedSecret = new EncryptedSecret(algorithmOld, null,
        toBase64(saltOld.getBytes()),
        toBase64(principalsBytes),
        Map.of("iv", Hex.toHexString(ivOld.getBytes())));
    PbeCipher cipherToDecrypt = pbeCipherFactory.create(secretKeyToDecrypt, encryptedSecret.toPhcString());
    return cipherToDecrypt.decrypt();
  }

  private static byte[] reEncrypt(
      final PbeCipher cipherToEncrypt,
      final byte[] decrypted)
  {
    EncryptedSecret encryptedSecret = cipherToEncrypt.encrypt(decrypted);
    String base64Value = encryptedSecret.getValue();
    return fromBase64(base64Value);
  }

  private static void updatePrincipal(
      final PreparedStatement update,
      final String domain,
      final String username,
      final String accessKey,
      final byte[] encrypted) throws SQLException
  {
    update.setBytes(1, encrypted);
    update.setString(2, domain);
    update.setString(3, username);
    update.setString(4, accessKey);
    update.executeUpdate();
  }

  private boolean thereAreChanges(
      final String keyPasswordOld,
      final String saltOld,
      final String ivOld,
      final String algorithmOld)
  {
    return !keyPasswordOld.equals(password) ||
        !saltOld.equals(salt) ||
        !ivOld.equals(iv) ||
        !algorithmOld.equals(nexusSecretsAlgorithm);
  }

  private String getValueFromTaskConfiguration(final String configurationKey, final String defaultValue) {
    String value = taskConfiguration().getString(configurationKey);
    if (value == null || value.isBlank()) {
      value = defaultValue;
    }

    return value;
  }
}
