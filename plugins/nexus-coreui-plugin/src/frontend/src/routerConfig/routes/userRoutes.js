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
import UserAccount from '../../components/pages/admin/UserAccount/UserAccount';
import NuGetApiToken from '../../components/pages/user/NuGetApiToken/NuGetApiToken';
import {UIView} from '@uirouter/react';
import UserToken from '../../components/pages/user/UserToken/UserToken';
import { ROUTE_NAMES } from '../routeNames/routeNames';

const USER = ROUTE_NAMES.USER;

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const userRoutes = [
  {
    name: USER.DIRECTORY,
    url: 'user',
    component: UIView,
    data: {
      visibilityRequirements: {
        ignoreForMenuVisibilityCheck: true
      }
    }
  },

  {
    name: USER.ACCOUNT,
    url: '/account',
    component: UserAccount,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        requiresUser: true,
      }
    },
  },

  {
    name: USER.NUGETAPITOKEN,
    url: '/NuGetApiToken',
    component: NuGetApiToken,
    data: {
      visibilityRequirements: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        requiresUser: true,
      }
    },
  },

  {
    name: USER.USER_TOKEN,
    url: '/user-token',
    component: UserToken,
    data: {
      visibilityRequirements: {
        bundle: 'com.sonatype.nexus.plugins.nexus-usertoken-plugin',
        statesEnabled: [
          {
            key: 'usertoken',
            defaultValue: {enabled: false}
          }
        ],
        permissions: ['nexus:usertoken-current:read'],
        editions: ['PRO'],
      }
    },
  }
]
