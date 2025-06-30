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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.memory.MemoryCUser;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class AuthenticatingRealmImplTest
    extends TestSupport
{
  private static final String TEST_USERNAME = "testUser";

  private static final String TEST_PASSWORD = "admin123";

  private static final String LEGACY_PASSWORD_HASH = "f865b53623b121fd34ee5426c792e5c33af8c227";

  private static final String SHIRO_PASSWORD_ALGORITHM = "shiro1";

  private static final String SHA256_PASSWORD_ALGORITHM = "pbkdf2-sha256";

  @Mock
  private SecurityConfigurationManager configuration;

  private CUser testUser = new MemoryCUser();

  @Before
  public void setUp() throws Exception {

    testUser.setId(TEST_USERNAME);
    testUser.setStatus(CUser.STATUS_ACTIVE);
    testUser.setPassword(LEGACY_PASSWORD_HASH);

    // read must pass back detached copy to avoid side-effects in test
    when(configuration.readUser(TEST_USERNAME)).thenAnswer((inv) -> testUser.clone());

    // can't use argument captor because argument is mutated after call
    doAnswer((inv) -> {
      // capture password from the updated user at the time of the call
      testUser.setPassword(((CUser) inv.getArguments()[0]).getPassword());
      return null;
    }).when(configuration).updateUser(any());
  }

  @Test
  public void testLegacyPasswordIsReHashedUsingShiroOnOrient() {
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient = new AuthenticatingRealmImpl(configuration,
        new DefaultSecurityPasswordService(new LegacyNexusPasswordService(), SHIRO_PASSWORD_ALGORITHM,
            new CryptoHelperImpl(false)),
        true, SHIRO_PASSWORD_ALGORITHM);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$shiro1$SHA-512$"));
  }

  @Test
  public void testLegacyPasswordIsReHashedUsingShiroOnNewDB() {
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient = new AuthenticatingRealmImpl(configuration,
        new DefaultSecurityPasswordService(new LegacyNexusPasswordService(), SHIRO_PASSWORD_ALGORITHM,
            new CryptoHelperImpl(false)),
        false, SHIRO_PASSWORD_ALGORITHM);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$shiro1$SHA-512$"));
  }

  @Test
  public void testLegacyPasswordIsReHashedToSha256OnOrient() {
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient = new AuthenticatingRealmImpl(configuration,
        new DefaultSecurityPasswordService(new LegacyNexusPasswordService(), SHA256_PASSWORD_ALGORITHM,
            new CryptoHelperImpl(false)),
        true, SHA256_PASSWORD_ALGORITHM);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$pbkdf2-sha256$i"));
  }

  @Test
  public void testLegacyPasswordIsReHashedToSha256OnNewDB() {
    assertThat(testUser.getPassword(), is(LEGACY_PASSWORD_HASH));
    AuthenticatingRealmImpl underTestOrient = new AuthenticatingRealmImpl(configuration,
        new DefaultSecurityPasswordService(new LegacyNexusPasswordService(), SHA256_PASSWORD_ALGORITHM,
            new CryptoHelperImpl(false)),
        false, SHA256_PASSWORD_ALGORITHM);
    underTestOrient.getAuthenticationInfo(new UsernamePasswordToken(TEST_USERNAME, TEST_PASSWORD));
    assertThat(testUser.getPassword(), startsWith("$pbkdf2-sha256$i"));
  }
}
