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
import { Permissions } from '@sonatype/nexus-ui-plugin';
import IqServer from '../../components/pages/admin/IqServer/IqServer';
import RepositoriesExt from '../../components/pages/admin/Repositories/RepositoriesExt';
import DataStoreConfiguration from '../../components/pages/admin/DataStoreConfiguration/DataStoreConfiguration';
import ProprietaryRepositories from '../../components/pages/admin/ProprietaryRepositories/ProprietaryRepositories';
import CleanupPoliciesList from '../../components/pages/admin/CleanupPolicies/CleanupPoliciesList';
import CleanupPoliciesForm from '../../components/pages/admin/CleanupPolicies/CleanupPoliciesForm';
import EmailServer from '../../components/pages/admin/EmailServer/EmailServer';
import SslCertificatesList from '../../components/pages/admin/SslCertificates/SslCertificatesList';
import SslCertificatesAddForm from '../../components/pages/admin/SslCertificates/SslCertificatesAddForm';
import SslCertificatesDetailsForm from '../../components/pages/admin/SslCertificates/SslCertificatesDetailsForm';
import UsersExt from '../../components/pages/admin/Users/UsersExt';
import AnonymousSettings from '../../components/pages/admin/AnonymousSettings/AnonymousSettings';
import Realms from '../../components/pages/admin/Realms/Realms';
import UserTokens from '../../components/pages/admin/UserTokens/UserTokens';
import CrowdSettings from '../../components/pages/admin/CrowdSettings/CrowdSettings';
import SamlConfiguration from '../../components/pages/admin/SamlConfiguration/SamlConfiguration';
import SupportRequest from '../../components/pages/admin/SupportRequest/SupportRequest';
import SystemInformation from '../../components/pages/admin/SystemInformation/SystemInformation';
import MetricHealth from '../../components/pages/admin/MetricHealth/MetricHealth';
import SupportZip from '../../components/pages/admin/SupportZip/SupportZip';
import Logs from '../../components/pages/admin/Logs/Logs';
import Api from '../../components/pages/admin/Api/Api';
import HTTP from '../../components/pages/admin/Http/Http';
import Licensing from '../../components/pages/admin/Licensing/Licensing';
import Upgrade from '../../components/pages/admin/Upgrade/Upgrade';
import NodesExt from '../../components/pages/admin/Nodes/NodesExt';
import TasksExtJSWrapper from '../../components/pages/admin/Tasks/TasksExtJSWrapper';
import Capabilities from '../../components/pages/admin/Capabilities/Capabilities';
import BlobStoresList from '../../components/pages/admin/BlobStores/BlobStoresList';
import BlobStoresForm from '../../components/pages/admin/BlobStores/BlobStoresForm';
import { UIView } from '@uirouter/react';
import LdapServersExt from '../../components/pages/admin/LdapServers/LdapServersExt';
import AdminRepositoriesDirectoryPage from '../../components/pages/AdminRepositories/AdminRepositoriesDirectoryPage';
import AdminSecurityDirectoryPage from '../../components/pages/AdminSecurity/AdminSecurityDirectoryPage';
import { ROUTE_NAMES } from '../routeNames/routeNames';
import AdminSystemDirectoryPage from '../../components/pages/AdminSystem/AdminSystemDirectoryPage';
import AdminSupportDirectoryPage from '../../components/pages/AdminSupport/AdminSupportDirectoryPage';

import SettingsPageLayout from '../../components/LeftNavigationMenu/SettingsPageLayout/SettingsPageLayout';
import PrivilegesList from '../../components/pages/admin/Privileges/PrivilegesList';
import PrivilegesDetails from '../../components/pages/admin/Privileges/PrivilegesDetails';
import ContentSelectorsList from '../../components/pages/admin/ContentSelectors/ContentSelectorsList';
import ContentSelectorsDetails from '../../components/pages/admin/ContentSelectors/ContentSelectorsDetails';
import RolesList from '../../components/pages/admin/Roles/RolesList';
import RolesDetails from '../../components/pages/admin/Roles/RolesDetails';
import RoutingRulesForm from '../../components/pages/admin/RoutingRules/RoutingRulesForm';
import RoutingRulesList from '../../components/pages/admin/RoutingRules/RoutingRulesList';
import RoutingRulesGlobalPreview from '../../components/pages/admin/RoutingRules/RoutingRulesGlobalPreview.jsx';
import LoggingConfigurationList from '../../components/pages/admin/LoggingConfiguration/LoggingConfigurationList.jsx';
import LoggingConfigurationForm from '../../components/pages/admin/LoggingConfiguration/LoggingConfigurationForm.jsx';

const ADMIN = ROUTE_NAMES.ADMIN;

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const adminRoutes = [
  {
    name: ADMIN.DIRECTORY,
    url: 'admin',
    component: SettingsPageLayout,
    abstract: true,
    data: {
      visibilityRequirements: {},
    },
  },

  // === admin/repository ===
  {
    name: ADMIN.REPOSITORY.DIRECTORY,
    url: '/repository',
    component: AdminRepositoriesDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.REPOSITORY.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.REPOSITORIES.ROOT,
    url: '/repositories:itemId',
    component: RepositoriesExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:repository-admin:*:*:read'],
      },
      title: ADMIN.REPOSITORY.REPOSITORIES.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.READ],
      },
      title: ADMIN.REPOSITORY.BLOBSTORES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.LIST,
    url: '/blobstores',
    component: BlobStoresList,
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.CREATE,
    url: '/blobstores/create',
    component: BlobStoresForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.CREATE],
      },
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.EDIT,
    url: '/blobstores/edit/{type:.+}/{name:.+}',
    component: BlobStoresForm,
  },

  {
    name: ADMIN.REPOSITORY.DATASTORE.ROOT,
    url: '/datastore',
    component: DataStoreConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-pro-datastore-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO', 'COMMUNITY'],
      },
      title: ADMIN.REPOSITORY.DATASTORE.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.PROPRIETARY.ROOT,
    url: '/proprietary',
    component: ProprietaryRepositories,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.REPOSITORY.PROPRIETARY.TITLE,
    },
  },

  // abstract parent selectors state
  {
    name: ADMIN.REPOSITORY.SELECTORS.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SELECTORS.READ],
      },
      title: ADMIN.REPOSITORY.SELECTORS.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.LIST,
    url: '/selectors',
    component: ContentSelectorsList,
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.EDIT,
    url: '/selectors/edit/{itemId:.+}',
    component: ContentSelectorsDetails,
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.CREATE,
    url: '/selectors/create',
    component: ContentSelectorsDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SELECTORS.CREATE],
      },
      title: ADMIN.REPOSITORY.SELECTORS.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.REPOSITORY.CLEANUPPOLICIES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.LIST,
    url: '/cleanuppolicies',
    component: CleanupPoliciesList,
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.CREATE,
    url: '/cleanuppolicies/create',
    component: CleanupPoliciesForm,
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.EDIT,
    url: '/cleanuppolicies/edit/{itemId:.+}',
    component: CleanupPoliciesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.REPOSITORY.ROUTINGRULES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.LIST,
    url: '/routingrules',
    component: RoutingRulesList,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.CREATE,
    url: '/routingrules/create',
    component: RoutingRulesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.EDIT,
    url: '/routingrules/edit/{itemId:.+}',
    component: RoutingRulesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.PREVIEW,
    url: '/routingrules/preview',
    component: RoutingRulesGlobalPreview,
  },

  // === admin/security ===
  {
    name: ADMIN.SECURITY.DIRECTORY,
    url: '/security',
    component: AdminSecurityDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SECURITY.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.PRIVILEGES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.LIST,
    url: '/privileges',
    component: PrivilegesList,
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.EDIT,
    url: '/privileges/edit/{itemId:.+}',
    component: PrivilegesDetails,
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.CREATE,
    url: '/privileges/create',
    component: PrivilegesDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.PRIVILEGES.CREATE],
      },
      title: ADMIN.SECURITY.PRIVILEGES.TITLE,
    },
  },

  // abstract parent roles state
  {
    name: ADMIN.SECURITY.ROLES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ROLES.READ, Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.ROLES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.ROLES.LIST,
    url: '/roles',
    component: RolesList,
  },

  {
    name: ADMIN.SECURITY.ROLES.EDIT,
    url: '/roles/edit/{itemId:.+}',
    component: RolesDetails,
  },

  {
    name: ADMIN.SECURITY.ROLES.CREATE,
    url: '/roles/create',
    component: RolesDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ROLES.READ, Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.ROLES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SSL_TRUSTSTORE.READ],
      },
      title: ADMIN.SECURITY.SSLCERTIFICATES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.LIST,
    url: '/sslcertificates',
    component: SslCertificatesList,
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.CREATE,
    url: '/sslcertificates/create',
    component: SslCertificatesAddForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SSL_TRUSTSTORE.CREATE],
      },
      title: ADMIN.SECURITY.SSLCERTIFICATES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.EDIT,
    url: '/sslcertificates/edit/{itemId:.+}',
    component: SslCertificatesDetailsForm,
  },

  {
    name: ADMIN.SECURITY.LDAP.ROOT,
    url: '/ldap:itemId',
    component: LdapServersExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LDAP.READ],
      },
      title: ADMIN.SECURITY.LDAP.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SECURITY.USERS.ROOT,
    url: '/users:itemId',
    component: UsersExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.USERS.READ, Permissions.ROLES.READ],
      },
      title: ADMIN.SECURITY.USERS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SECURITY.ANONYMOUS.ROOT,
    url: '/anonymous',
    component: AnonymousSettings,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SECURITY.ANONYMOUS.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.REALMS.ROOT,
    url: '/realms',
    component: Realms,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SECURITY.REALMS.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.USERTOKEN.ROOT,
    url: '/usertoken',
    component: UserTokens,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.USER_TOKENS_SETTINGS.READ],
        editions: ['PRO'],
        licenseValid: [
          {
            key: 'usertoken',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.SECURITY.USERTOKEN.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.ATLASSIANCROWD.ROOT,
    url: '/atlassiancrowd',
    component: CrowdSettings,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-crowd-plugin',
        licenseValid: [
          {
            key: 'crowd',
            defaultValue: false,
          },
        ],
        permissions: ['nexus:crowd:read'],
      },
      title: ADMIN.SECURITY.ATLASSIANCROWD.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SAML.ROOT,
    url: '/saml',
    component: SamlConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-saml-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO'],
      },
      title: ADMIN.SECURITY.SAML.TITLE,
    },
  },

  // === admin/support ===
  {
    name: ADMIN.SUPPORT.DIRECTORY,
    url: '/support',
    component: AdminSupportDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SUPPORT.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.SUPPORTREQUEST.ROOT,
    url: '/supportrequest',
    component: SupportRequest,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.CREATE],
        editions: ['PRO'],
      },
      title: ADMIN.SUPPORT.SUPPORTREQUEST.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.SYSTEMINFORMATION.ROOT,
    url: '/systeminformation',
    component: SystemInformation,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.READ],
      },
      title: ADMIN.SUPPORT.SYSTEMINFORMATION.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.STATUS.ROOT,
    url: '/status:itemId',
    component: MetricHealth,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.METRICS.READ],
      },
      title: ADMIN.SUPPORT.STATUS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SUPPORT.SUPPORTZIP.ROOT,
    url: '/supportzip',
    component: SupportZip,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.READ],
      },
      title: ADMIN.SUPPORT.SUPPORTZIP.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.LOGS.ROOT,
    url: '/logs:itemId',
    component: Logs,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.READ],
        notClustered: true,
      },
      title: ADMIN.SUPPORT.LOGS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SUPPORT.LOGGING.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.READ],
      },
      title: ADMIN.SUPPORT.LOGGING.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.LOGGING.LIST,
    url: '/logging',
    component: LoggingConfigurationList,
  },

  {
    name: ADMIN.SUPPORT.LOGGING.EDIT,
    url: '/logging/edit/{itemId:.+}',
    component: LoggingConfigurationForm,
  },

  {
    name: ADMIN.SUPPORT.LOGGING.CREATE,
    url: '/logging/create',
    component: LoggingConfigurationForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.UPDATE],
      },
      title: ADMIN.SUPPORT.LOGGING.TITLE,
    },
  },

  // === admin/system ===
  {
    name: ADMIN.SYSTEM.DIRECTORY,
    url: '/system',
    component: AdminSystemDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SYSTEM.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.TASKS.ROOT,
    url: '/tasks:taskId',
    component: TasksExtJSWrapper,
    params: {
      taskId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: [Permissions.TASKS.READ],
      },
      title: ADMIN.SYSTEM.TASKS.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.API.ROOT,
    url: '/api',
    component: Api,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.API.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES.ROOT,
    url: '/capabilities:id',
    component: Capabilities,
    params: {
      id: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: ['nexus:capabilities:read'],
      },
      title: ADMIN.SYSTEM.CAPABILITIES.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.EMAILSERVER.ROOT,
    url: '/emailserver',
    component: EmailServer,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.EMAILSERVER.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.HTTP.ROOT,
    url: '/http',
    component: HTTP,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.HTTP.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.LICENSING.ROOT,
    url: '/licensing',
    component: Licensing,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LICENSING.READ],
      },
      title: ADMIN.SYSTEM.LICENSING.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.NODES.ROOT,
    url: '/nodes',
    component: NodesExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.SYSTEM.NODES.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.UPGRADE.ROOT,
    url: '/upgrade',
    component: Upgrade,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.MIGRATION.READ],
        capability: 'migration',
      },
      title: ADMIN.SYSTEM.UPGRADE.TITLE,
    },
  },

  // === iq ===
  {
    name: ADMIN.IQ.ROOT,
    url: '/iq',
    component: IqServer,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.IQ.TITLE,
    },
  },
];
