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
import ContentSelectors from '../../components/pages/admin/ContentSelectors/ContentSelectors';
import CleanupPolicies from '../../components/pages/admin/CleanupPolicies/CleanupPolicies';
import RoutingRules from '../../components/pages/admin/RoutingRules/RoutingRules';
import Privileges from '../../components/pages/admin/Privileges/Privileges';
import EmailServer from '../../components/pages/admin/EmailServer/EmailServer';
import Roles from '../../components/pages/admin/Roles/Roles';
import SslCertificates from '../../components/pages/admin/SslCertificates/SslCertificates';
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
import BlobStoresList from "../../components/pages/admin/BlobStores/BlobStoresList";
import { BlobStoresFormRouterAwareContainer } from "../../components/pages/admin/BlobStores/BlobStoresForm";
import { UIView } from "@uirouter/react";
import LdapServersExt from "../../components/pages/admin/LdapServers/LdapServersExt";
import AdminRepositoriesDirectoryPage from '../../components/pages/AdminRepositories/AdminRepositoriesDirectoryPage';
import AdminSecurityDirectoryPage from '../../components/pages/AdminSecurity/AdminSecurityDirectoryPage';
import { ROUTE_NAMES } from '../routeNames/routeNames';
import AdminSystemDirectoryPage from '../../components/pages/AdminSystem/AdminSystemDirectoryPage';
import AdminSupportDirectoryPage from '../../components/pages/AdminSupport/AdminSupportDirectoryPage';

const ADMIN = ROUTE_NAMES.ADMIN;
import SettingsPageLayout from '../../components/LeftNavigationMenu/SettingsPageLayout/SettingsPageLayout';
import LoggingConfiguration from '../../components/pages/admin/LoggingConfiguration/LoggingConfiguration';

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const adminRoutes = [
  {
    name: ADMIN.DIRECTORY,
    url: 'admin',
    component: SettingsPageLayout,
    abstract: true,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true
      },
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
        ignoreForMenuVisibilityCheck: true
      }
    }
  },

  {
    name: ADMIN.REPOSITORY.REPOSITORIES,
    url: '/repositories:itemId',
    component: RepositoriesExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:repository-admin:*:*:read'],
      },
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.DIRECTORY,
    component: UIView,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      }
    }
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.LIST,
    url: '/blobstores',
    component: BlobStoresList,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.READ]
      }
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.DETAILS,
    url: '/blobstores/:itemId',
    component: BlobStoresFormRouterAwareContainer,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.READ]
      }
    },
  },

  {
    name: ADMIN.REPOSITORY.DATASTORE,
    url: '/datastore',
    component: DataStoreConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'com.sonatype.nexus.plugins.nexus-pro-datastore-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO', 'COMMUNITY']
      }
    },
  },

  {
    name: ADMIN.REPOSITORY.PROPRIETARY,
    url: '/proprietary',
    component: ProprietaryRepositories,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ]
      }
    },
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS,
    url: '/selectors:itemId',
    component: ContentSelectors,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SELECTORS.READ],
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES,
    url: '/cleanuppolicies:itemId',
    component: CleanupPolicies,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ADMIN]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES,
    url: '/routingrules:itemId',
    component: RoutingRules,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ADMIN]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  // === admin/security ===
  {
    name: ADMIN.SECURITY.DIRECTORY,
    url: '/security',
    component: AdminSecurityDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES,
    url: '/privileges:itemId',
    component: Privileges,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.PRIVILEGES.READ]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.ROLES,
    url: '/roles:itemId',
    component: Roles,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:roles:read', 'nexus:privileges:read']
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES,
    url: '/sslcertificates:itemId',
    component: SslCertificates,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SSL_TRUSTSTORE.READ]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.LDAP,
    url: '/ldap:itemId',
    component: LdapServersExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.LDAP.READ]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.USERS,
    url: '/users:itemId',
    component: UsersExt,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.USERS.READ, Permissions.ROLES.READ]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SECURITY.ANONYMOUS,
    url: '/anonymous',
    component: AnonymousSettings,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SETTINGS.READ]
      }
    }
  },

  {
    name: ADMIN.SECURITY.REALMS,
    url: '/realms',
    component: Realms,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SETTINGS.READ]
      }
    }
  },

  {
    name: ADMIN.SECURITY.USERTOKEN,
    url: '/usertoken',
    component: UserTokens,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.USER_TOKENS_SETTINGS.READ],
        editions: ['PRO'],
        licenseValid: [
          {
            key: 'usertoken',
            defaultValue: false
          }
        ],
      }
    }
  },

  {
    name: ADMIN.SECURITY.ATLASSIANCROWD,
    url: '/atlassiancrowd',
    component: CrowdSettings,
    data: {
      visibilityRequirements: {
        bundle: 'com.sonatype.nexus.plugins.nexus-crowd-plugin',
        licenseValid: [
          {
            key: 'crowd',
            defaultValue: false
          }
        ],
        permissions: ['nexus:crowd:read']
      }
    }
  },

  {
    name: ADMIN.SECURITY.SAML,
    url: '/saml',
    component: SamlConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'com.sonatype.nexus.plugins.nexus-saml-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO']
      }
    }
  },

  // === admin/support ===
  {
    name: ADMIN.SUPPORT.DIRECTORY,
    url: '/support',
    component: AdminSupportDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true
      }
    }
  },

  {
    name: ADMIN.SUPPORT.SUPPORTREQUEST,
    url: '/supportrequest',
    component: SupportRequest,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ATLAS.CREATE],
        editions: ['PRO']
      }
    }
  },

  {
    name: ADMIN.SUPPORT.SYSTEMINFORMATION,
    url: '/systeminformation',
    component: SystemInformation,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ATLAS.READ]
      }
    }
  },

  {
    name: ADMIN.SUPPORT.STATUS,
    url: '/status:itemId',
    component: MetricHealth,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.METRICS.READ]
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SUPPORT.SUPPORTZIP,
    url: '/supportzip',
    component: SupportZip,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ATLAS.READ]
      }
    }
  },

  {
    name: ADMIN.SUPPORT.LOGS,
    url: '/logs:itemId',
    component: Logs,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.LOGGING.READ],
        notClustered: true
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  {
    name: ADMIN.SUPPORT.LOGGING,
    url: '/logging:itemId',
    component: LoggingConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.LOGGING.READ],
      }
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true
      }
    }
  },

  // === admin/system ===
  {
    name: ADMIN.SYSTEM.DIRECTORY,
    url: '/system',
    component: AdminSystemDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true
      }
    }
  },

  {
    name: ADMIN.SYSTEM.TASKS,
    url: '/tasks:taskId',
    component: TasksExtJSWrapper,
    params: {
      taskId: {
        value: null,
        raw: true,
        dynamic: true
      }
    },
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.TASKS.READ]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.API,
    url: '/api',
    component: Api,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES,
    url: '/capabilities:id',
    component: Capabilities,
    params: {
      id: {
        value: null,
        raw: true,
        dynamic: true
      }
    },
    data: {
      visibilityRequirements: {
        permissions: ['nexus:capabilities:read']
      }
    }
  },

  {
    name: ADMIN.SYSTEM.EMAILSERVER,
    url: '/emailserver',
    component: EmailServer,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SETTINGS.READ]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.HTTP,
    url: '/http',
    component: HTTP,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.LICENSING,
    url: '/licensing',
    component: Licensing,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LICENSING.READ]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.NODES,
    url: '/nodes',
    component: NodesExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN]
      }
    }
  },

  {
    name: ADMIN.SYSTEM.UPGRADE,
    url: '/upgrade',
    component: Upgrade,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.MIGRATION.READ],
        capability: 'migration'

      }
    }
  },

  // === iq ===
  {
    name: ADMIN.IQ,
    url: '/iq',
    component: IqServer,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
    },
  },
];
