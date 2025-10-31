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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {createRouter} from '@sonatype/nexus-ui-plugin';
import {ROUTE_NAMES} from './routeNames/routeNames';
import {browseRoutes} from './routes/browseRoutes';
import {adminRoutes} from './routes/adminRoutes';
import {userRoutes} from './routes/userRoutes';
import {loginRoutes} from "./routes/loginRoutes";
import {MissingRoutePage} from '../components/pages/MissingRoutePage/MissingRoutePage';

export function getRouter() {
  const initialRoute = getInitialRoute();

  const menuRoutes = [
    ...browseRoutes,
    ...adminRoutes,
    ...userRoutes,
    ...getLoginRoutes(),
  ];

  const missingRoute = {
    name: ROUTE_NAMES.MISSING_ROUTE,
    url: '404',
    component: MissingRoutePage,
    data: {
      visibilityRequirements: {}
    }
  };

  return createRouter({initialRoute, menuRoutes, missingRoute});
}

/**
 * Determines the initial route based on feature flags and configuration.
 */
function getInitialRoute() {
  let initialRoute = ROUTE_NAMES.BROWSE.WELCOME.ROOT;

  // Check if ExtJS is available (not available in test environment)
  if (!ExtJS?.state) {
    return initialRoute;
  }

  const isReactLoginEnabled = ExtJS.state().getValue('nexus.login.react.enabled', false);
  if (isReactLoginEnabled) {
    const isAnonymousAccessEnabled = !!ExtJS.state().getValue('anonymousUsername');
    if (!isAnonymousAccessEnabled) {
      initialRoute = ROUTE_NAMES.LOGIN;
    }
  }
  return initialRoute;
}

/**
 * Login routes configuration.
 * Routes are only registered if the React login feature flag is enabled.
 */
function getLoginRoutes() {
  // Check if ExtJS is available (not available in test environment)
  if (!ExtJS?.state) {
    return [];
  }

  const isReactLoginEnabled = ExtJS.state().getValue('nexus.login.react.enabled', false);
  if (isReactLoginEnabled) {
    return loginRoutes
  }

  return [];
}
