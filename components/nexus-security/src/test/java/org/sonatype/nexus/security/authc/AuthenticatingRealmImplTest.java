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
package org.sonatype.nexus.security.authc;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.internal.DefaultSecurityPasswordService;
import org.sonatype.nexus.security.internal.SecurityConfigurationManagerImpl;

import com.google.common.hash.Hashing;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.realm.Realm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticatingRealmImplTest
    extends AbstractSecurityTest
{
  private AuthenticatingRealmImpl realm;

  private SecurityConfigurationManagerImpl configurationManager;

  private PasswordService passwordService;

  private CUser testUser;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    realm = (AuthenticatingRealmImpl) lookup(Realm.class, AuthenticatingRealmImpl.NAME);
    configurationManager = lookup(SecurityConfigurationManagerImpl.class);
    passwordService = lookup(PasswordService.class, "default");
  }

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(JwtHelper.class).toInstance(mock(JwtHelper.class));
        bind(JwtFilter.class).toInstance(mock(JwtFilter.class));
        CryptoHelper cryptoHelper = mock(CryptoHelper.class);
        try {
          when(cryptoHelper.createSecureRandom()).thenReturn(new SecureRandom());
          SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
          when(cryptoHelper.createSecretKeyFactory(anyString())).thenReturn(keyFactory);

          when(cryptoHelper.createCipher(anyString())).thenReturn(Cipher.getInstance("AES/CBC/PKCS5Padding"));
        }
        catch (Exception e) {
          throw new RuntimeException("Failed to setup crypto mocks", e);
        }
        bind(CryptoHelper.class).toInstance(cryptoHelper);
      }
    });
  }

  @Test
  public void testSuccessfulAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());
    assertThat(this.passwordService.passwordsMatch("password", password), is(true));
  }

  @Test
  public void testCreateWithPassowrd() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    String clearPassword = "default-password";
    String username = "testCreateWithPassowrdEmailUserId";

    CUser user = user("testCreateWithPassowrdEmail@somewhere", "testCreateWithPassowrdEmail",
        "testCreateWithPassowrdEmail", CUser.STATUS_ACTIVE, username, null);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(user, clearPassword, roles);

    UsernamePasswordToken upToken = new UsernamePasswordToken("testCreateWithPassowrdEmailUserId", clearPassword);
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());

    assertThat(passwordService.passwordsMatch(clearPassword, password), is(true));
  }

  @Test
  public void testFailedAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "badpassword");

    thrown.expect(IncorrectCredentialsException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testDisabledAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_DISABLED);
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");

    thrown.expect(DisabledAccountException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testGetAuthenticationInfo_userStatusChangePassword() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_CHANGE_PASSWORD);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());
    assertThat(this.passwordService.passwordsMatch("password", password), is(true));
  }

  @Test
  public void testDetectLegacyUser() throws Exception {
    String password = "password";
    String username = "username";
    buildLegacyTestAuthenticationConfig(password);

    UsernamePasswordToken upToken = new UsernamePasswordToken(username, password);
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    CUser updatedUser = this.configurationManager.readUser(username);
    String hash = new String((char[]) ai.getCredentials());

    assertThat(passwordService.passwordsMatch(password, hash), is(true));
    assertThat(passwordService.passwordsMatch(password, updatedUser.getPassword()), is(true));
  }

  @Test
  public void testNoneExistentUser() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);
    UsernamePasswordToken upToken = new UsernamePasswordToken("non-existent-user", "password");

    thrown.expect(UnknownAccountException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testEmptyPassword() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", (String) null);

    thrown.expect(CredentialsException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testDoGetAuthenticationInfo_DoesNotRehashFipsPassword() throws Exception {
    String password = "password";
    String username = "fipsUser";

    String fipsHash = passwordService.encryptPassword(password);
    assumeTrue(DefaultSecurityPasswordService.isFipsPassword(fipsHash));

    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, fipsHash, username);

    assertTrue(passwordService.passwordsMatch(password, fipsHash));

    UsernamePasswordToken upToken = new UsernamePasswordToken(username, password);
    realm.getAuthenticationInfo(upToken);

    String afterHash = configurationManager.readUser(username).getPassword();
    assertThat("FIPS password should not be rehashed", afterHash, equalTo(fipsHash));
  }

  @Test
  public void testDoGetAuthenticationInfo_DoesNotRehashPasswordWhenCredentialsInvalid() throws Exception {
    String password = "password";
    String username = "nonFipsUser";
    String initialHash = passwordService.encryptPassword(password);

    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, initialHash, username);

    UsernamePasswordToken upToken = new UsernamePasswordToken(username, "wrongpassword");
    thrown.expect(IncorrectCredentialsException.class);
    try {
      realm.getAuthenticationInfo(upToken);
    }
    finally {
      String afterHash = configurationManager.readUser(username).getPassword();
      assertThat("Password should not be rehashed for failed auth",
          afterHash, equalTo(initialHash));
    }
  }

  private void buildTestAuthenticationConfig(final String status, final String hash, String username) throws Exception {
    CPrivilege priv = new MemoryCPrivilege();
    priv.setId("priv-" + username);
    priv.setName("name");
    priv.setDescription("desc");
    priv.setType("method");
    priv.setProperty("method", "read");
    priv.setProperty("permission", "somevalue");

    configurationManager.createPrivilege(priv);

    CRole role = configurationManager.newRole();
    role.setName("name");
    role.setId("role-" + username);
    role.setDescription("desc");
    role.addPrivilege("priv-" + username);

    configurationManager.createRole(role);

    testUser = user("dummyemail@somewhere", "dummyFirstName", "dummyLastName", status, username, hash);

    Set<String> roles = new HashSet<String>();
    roles.add("role-" + username);

    configurationManager.createUser(testUser, roles);
  }

  private void buildTestAuthenticationConfig(final String status) throws Exception {
    buildTestAuthenticationConfig(status, this.hashPassword("password"));
  }

  private void buildTestAuthenticationConfig(final String status, final String hash) throws Exception {
    CPrivilege priv = new MemoryCPrivilege();
    priv.setId("priv");
    priv.setName("name");
    priv.setDescription("desc");
    priv.setType("method");
    priv.setProperty("method", "read");
    priv.setProperty("permission", "somevalue");

    configurationManager.createPrivilege(priv);

    CRole role = configurationManager.newRole();
    role.setName("name");
    role.setId("role");
    role.setDescription("desc");
    role.addPrivilege("priv");

    configurationManager.createRole(role);

    testUser = user("dummyemail@somewhere", "dummyFirstName", "dummyLastName", status, "username", hash);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(testUser, roles);
  }

  private void buildLegacyTestAuthenticationConfig(final String password, String username) throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, legacyHashPassword(password), username);
  }

  private String hashPassword(final String password) {
    return passwordService.encryptPassword(password);
  }

  @SuppressWarnings("deprecation")
  private String legacyHashPassword(final String password) {
    return Hashing.sha1().hashString(password, StandardCharsets.UTF_8).toString();
  }

  private void buildLegacyTestAuthenticationConfig(final String password) throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, legacyHashPassword(password));
  }

  private static CUser user(
      final String email,
      final String firstName,
      final String lastName,
      final String status,
      final String id,
      final String passwordHash)
  {
    CUser testUser = new MemoryCUser();
    testUser.setEmail(email);
    testUser.setFirstName(firstName);
    testUser.setLastName(lastName);
    testUser.setStatus(status);
    testUser.setId(id);
    testUser.setPassword(passwordHash);
    return testUser;
  }
}
