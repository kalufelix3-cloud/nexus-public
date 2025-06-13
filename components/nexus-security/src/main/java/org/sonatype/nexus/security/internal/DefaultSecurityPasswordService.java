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
package org.sonatype.nexus.security.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.security.authc.AuthenticationFailureReason;
import org.sonatype.nexus.security.authc.NexusAuthenticationException;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.HashingPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PasswordService}.
 *
 * A PasswordService that provides a default password policy.
 *
 * The intent of the password service is to encapsulate all password handling
 * details, such as password comparisons, hashing algorithm, hash iterations, salting policy, etc.
 *
 * This class is just a wrapper around DefaultPasswordService to apply the default password policy,
 * and provide backward compatibility with legacy SHA1 and MD5 based passwords.
 */
@Named("default")
@Singleton
public class DefaultSecurityPasswordService
    implements HashingPasswordService
{

  private static final Logger log = LoggerFactory.getLogger(DefaultSecurityPasswordService.class);

  private static final String DEFAULT_HASH_ALGORITHM = "SHA-512";

  private static final int DEFAULT_HASH_ITERATIONS = 1024;

  private static final String FIPS_COMPLIANT_ALGORITHM = "$pbkdf2-sha256$";

  private static final String PBKDF2_SHA256 = "PBKDF2WithHmacSHA256";

  private static final int FIPS_ITERATIONS = 10000;

  private static final int SALT_LENGTH = 16;

  private static final int KEY_LENGTH = 256;

  /**
   * Provides the actual implementation of PasswordService.
   * We are just wrapping to apply default policy
   */
  private final DefaultPasswordService passwordService;

  /**
   * Provides password services for legacy passwords (e.g. pre-2.5 SHA-1/MD5-based hashes)
   */
  private final PasswordService legacyPasswordService;

  private final CryptoHelper crypto;

  @Inject
  public DefaultSecurityPasswordService(
      @Named("legacy") final PasswordService legacyPasswordService,
      final CryptoHelper crypto)
  {
    this.passwordService = new DefaultPasswordService();
    this.legacyPasswordService = checkNotNull(legacyPasswordService);

    // Create and set a hash service according to our hashing policies
    DefaultHashService hashService = new DefaultHashService();
    hashService.setHashAlgorithmName(DEFAULT_HASH_ALGORITHM);
    hashService.setHashIterations(DEFAULT_HASH_ITERATIONS);
    hashService.setGeneratePublicSalt(true);
    this.passwordService.setHashService(hashService);
    this.crypto = checkNotNull(crypto);
  }

  @Override
  public String encryptPassword(final Object plaintextPassword) {
    // Generate random salt
    byte[] salt = new byte[SALT_LENGTH];

    SecureRandom secureRandom = crypto.createSecureRandom();
    secureRandom.nextBytes(salt);

    // Create hash using PBKDF2
    PBEKeySpec spec = new PBEKeySpec(
        convertToCharArray(plaintextPassword),
        salt,
        FIPS_ITERATIONS,
        KEY_LENGTH);
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_SHA256);
      byte[] hash = factory.generateSecret(spec).getEncoded();
      spec.clearPassword();

      // Format: $algorithm$i=iterations$salt$hash
      return String.format("%si=%d$%s$%s",
          FIPS_COMPLIANT_ALGORITHM,
          FIPS_ITERATIONS,
          Base64.getEncoder().encodeToString(salt),
          Base64.getEncoder().encodeToString(hash));
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      log.error("Failed to encrypt password due to algorithm issue", e);
      throw new NexusAuthenticationException("Password is not strong enough",
          Set.of(AuthenticationFailureReason.UNKNOWN));
    }
    catch (FipsUnapprovedOperationError e) {
      log.error("Failed to encrypt password", e);
      throw new NexusAuthenticationException("Password is not strong enough",
          Set.of(AuthenticationFailureReason.INCORRECT_CREDENTIALS));
    }
    finally {
      spec.clearPassword();
    }
  }

  @Override
  public boolean passwordsMatch(final Object submittedPlaintextPassword, final String storedHash) {
    if (storedHash == null || submittedPlaintextPassword == null) {
      return false;
    }

    if (isFipsPassword(storedHash)) {
      try {
        FipsHash storedFipsHash = parseFipsHash(storedHash);
        char[] submittedPassword = convertToCharArray(submittedPlaintextPassword);
        boolean matches = verifyFipsHash(submittedPassword, storedFipsHash);
        // Clear sensitive data from memory
        Arrays.fill(submittedPassword, '\0');
        storedFipsHash.clear();
        return matches;
      }
      catch (Exception e) {
        log.warn("FIPS password verification failed", e);
        return false;
      }
    }

    return passwordService.passwordsMatch(submittedPlaintextPassword, storedHash) ||
        legacyPasswordService.passwordsMatch(submittedPlaintextPassword, storedHash);
  }

  @Override
  public Hash hashPassword(final Object plaintext) {
    return passwordService.hashPassword(plaintext);
  }

  @Override
  public boolean passwordsMatch(final Object plaintext, final Hash savedPasswordHash) {
    return passwordService.passwordsMatch(plaintext, savedPasswordHash);
  }

  private static char[] convertToCharArray(final Object plaintext) {
    if (plaintext == null) {
      return new char[0]; // Return empty array for null input
    }
    else if (plaintext instanceof char[]) {
      return (char[]) plaintext;
    }
    else if (plaintext instanceof String) {
      return ((String) plaintext).toCharArray();
    }
    else {
      return plaintext.toString().toCharArray();
    }
  }

  public static boolean isFipsPassword(final String password) {
    return password != null && password.startsWith(FIPS_COMPLIANT_ALGORITHM);
  }

  private static class FipsHash
  {
    String algorithm;

    int iterations;

    byte[] salt;

    byte[] hash;

    String fullHash;

    void clear() {
      if (salt != null)
        Arrays.fill(salt, (byte) 0);
      if (hash != null)
        Arrays.fill(hash, (byte) 0);
    }
  }

  private static FipsHash parseFipsHash(final String hash) throws IllegalArgumentException {
    String[] parts = hash.split("\\$");
    if (parts.length < 5) {
      throw new IllegalArgumentException("Invalid FIPS hash format");
    }

    FipsHash result = new FipsHash();
    result.algorithm = parts[1];

    try {
      result.iterations = Integer.parseInt(parts[2].substring(2));
      result.salt = Base64.getDecoder().decode(parts[3]);
      result.hash = Base64.getDecoder().decode(parts[4]);
      result.fullHash = hash;
      return result;
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse FIPS hash", e);
    }
  }

  private static boolean verifyFipsHash(final char[] password, final FipsHash storedFipsHash) {
    PBEKeySpec spec = new PBEKeySpec(
        password,
        storedFipsHash.salt,
        storedFipsHash.iterations,
        KEY_LENGTH);

    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_SHA256);
      byte[] hash = factory.generateSecret(spec).getEncoded();
      return MessageDigest.isEqual(hash, storedFipsHash.hash);
    }
    catch (Exception e) {
      log.warn("FIPS verification failed", e);
      return false;
    }
    finally {
      spec.clearPassword();
    }
  }
}
