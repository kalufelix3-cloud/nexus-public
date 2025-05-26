/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
export default {
  IQ: 'admin.iq',
  DIRECTORY: 'admin',
  REPOSITORY: {
    DIRECTORY: 'admin.repository',
    REPOSITORIES: 'admin.repository.repositories',
    BLOBSTORES: {
      ROOT: 'admin.repository.blobstores',
      LIST: 'admin.repository.blobstores.list',
      DETAILS: 'admin.repository.blobstores.details'
    },
    DATASTORE: 'admin.repository.datastore',
    PROPRIETARY: 'admin.repository.proprietary',
    SELECTORS: {
      ROOT: 'admin.repository.selectors',
      LIST: 'admin.repository.selectors.list',
      EDIT: 'admin.repository.selectors.edit',
      CREATE: 'admin.repository.selectors.create'
    },
    CLEANUPPOLICIES: {
      ROOT: 'admin.repository.cleanuppolicies',
      LIST: 'admin.repository.cleanuppolicies.list',
      CREATE: 'admin.repository.cleanuppolicies.create',
      EDIT: 'admin.repository.cleanuppolicies.details'
    },
    ROUTINGRULES: {
      ROOT: 'admin.repository.routingrules',
      LIST: 'admin.repository.routingrules.list',
      CREATE: 'admin.repository.routingrules.create',
      EDIT: 'admin.repository.routingrules.edit',
      PREVIEW: 'admin.repository.routingrules.preview',
    },
  },
  SECURITY: {
    DIRECTORY: 'admin.security',
    PRIVILEGES:{
      ROOT: 'admin.security.privileges',
      LIST: 'admin.security.privileges.list',
      EDIT: 'admin.security.privileges.edit',
      CREATE: 'admin.security.privileges.create',
    },
    ROLES: {
      ROOT: 'admin.security.roles',
      LIST: 'admin.security.roles.list',
      EDIT: 'admin.security.roles.edit',
      CREATE: 'admin.security.roles.create'
    },
    USERS: 'admin.security.users',
    ANONYMOUS: 'admin.security.anonymous',
    ATLASSIANCROWD: 'admin.security.atlassiancrowd',
    LDAP: 'admin.security.ldap',
    REALMS: 'admin.security.realms',
    SAML: 'admin.security.saml',
    SSLCERTIFICATES: {
      ROOT: 'admin.security.sslcertificates',
      LIST: 'admin.security.sslcertificates.list',
      EDIT: 'admin.security.sslcertificates.edit',
      CREATE: 'admin.security.sslcertificates.create',
    },
    USERTOKEN: 'admin.security.usertoken'
  },
  SYSTEM: {
    DIRECTORY: 'admin.system',
    TASKS: 'admin.system.tasks',
    API: 'admin.system.api',
    CAPABILITIES: 'admin.system.capabilities',
    EMAILSERVER: 'admin.system.emailserver',
    HTTP: 'admin.system.http',
    LICENSING: 'admin.system.licensing',
    NODES: 'admin.system.nodes',
    UPGRADE: 'admin.system.upgrade'
  },
  SUPPORT: {
    DIRECTORY: 'admin.support',
    SUPPORTREQUEST: 'admin.support.supportrequest',
    LOGS: 'admin.support.logs',
    LOGGING:{
      ROOT: 'admin.support.logging',
      LIST: 'admin.support.logging.list',
      CREATE: 'admin.support.logging.create',
      EDIT: 'admin.support.logging.edit',
    }, 
    STATUS: 'admin.support.status',
    SUPPORTZIP: 'admin.support.supportzip',
    SYSTEMINFORMATION: 'admin.support.systeminformation'
  }
};
