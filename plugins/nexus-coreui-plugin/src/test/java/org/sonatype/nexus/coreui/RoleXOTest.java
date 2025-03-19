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
package org.sonatype.nexus.coreui;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.sonatype.goodies.testsupport.inject.InjectedTestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.WebSecurityModule;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.inject.Binder;
import org.apache.shiro.authc.credential.PasswordService;
import org.eclipse.sisu.space.BeanScanning;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleXOTest
    extends InjectedTestSupport
{

  @Inject
  Validator validator;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.install(new ValidationModule());
    binder.install(new WebSecurityModule(mock(ServletContext.class)));
    binder.bind(EventManager.class).toInstance(mock(EventManager.class));
    binder.bind(AnonymousManager.class).toInstance(mock(AnonymousManager.class));
    SecurityConfigurationSource securityConfigurationSource = mock(SecurityConfigurationSource.class);
    when(securityConfigurationSource.loadConfiguration()).thenReturn(getMemorySecurityConfiguration());
    binder.bind(SecurityConfigurationSource.class).toInstance(securityConfigurationSource);

    binder.bind(PasswordService.class).toInstance(mock(PasswordService.class));

    ApplicationDirectories directories = mock(ApplicationDirectories.class);
    binder.bind(ApplicationDirectories.class).toInstance(directories);
  }

  @Override
  public BeanScanning scanning() {
    return BeanScanning.INDEX;
  }

  @Test
  public void testValidation_success() {
    RoleXO roleXO = new RoleXO();
    roleXO.setId("test");
    roleXO.setName("test");
    roleXO.setRoles(Collections.singleton("test2"));

    Set<ConstraintViolation<Object>> errors = validator.validate(roleXO);

    assertThat(errors.size(), is(0));
  }

  @Test
  public void testValidation_failure_includesSelf() {
    RoleXO roleXO = new RoleXO();
    roleXO.setId("test");
    roleXO.setName("test");
    roleXO.setRoles(Collections.singleton("test"));

    Set<ConstraintViolation<Object>> errors = validator.validate(roleXO);

    assertThat(errors.size(), is(1));
    assertThat(errors.iterator().next().getMessage(),
        is("A role cannot contain itself directly or indirectly through other roles."));
  }

  private static MemorySecurityConfiguration getMemorySecurityConfiguration() {
    return new MemorySecurityConfiguration().withUsers(
        new MemoryCUser()
            .withId("admin")
            .withPassword("encryptedPassword")
            .withFirstName("Administrator")
            .withLastName("User")
            .withStatus(CUser.STATUS_ACTIVE)
            .withEmail("admin@example.org"),
        new MemoryCUser()
            .withId("anonymous")
            // password="anonymous"
            .withPassword(
                "$shiro1$SHA-512$1024$CPJm1XWdYNg5eCAYp4L4HA==$HIGwnJhC07ZpgeVblZcFRD1F6KH+xPG8t7mIcEMbfycC+n5Ljudyoj9dzdinrLmChTrmKMCw2/z29F7HeLbTbQ==")
            .withFirstName("Anonymous")
            .withLastName("User")
            .withStatus(CUser.STATUS_ACTIVE)
            .withEmail("anonymous@example.org"))
        .withUserRoleMappings(
            new MemoryCUserRoleMapping()
                .withUserId("admin")
                .withSource("default")
                .withRoles(Roles.ADMIN_ROLE_ID),
            new MemoryCUserRoleMapping()
                .withUserId("anonymous")
                .withSource("default")
                .withRoles(Roles.ANONYMOUS_ROLE_ID));
  }
}
