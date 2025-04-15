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
import {UIRouterReact, hashLocationPlugin, servicesPlugin} from '@uirouter/react';

import { browseRoutes } from './routes/browseRoutes';
import { adminRoutes } from './routes/adminRoutes';
import { userRoutes } from './routes/userRoutes';
import isVisible from './isVisible';
import { ROUTE_NAMES } from './routeNames/routeNames';
import { MissingRoutePage } from '../components/pages/MissingRoutePage/MissingRoutePage';

const success = 'success';
const failure = 'failure';

/*
  This is where we register all of the client side routes. Routing is handled using
  UIRouter. For more info see https://github.com/ui-router/react.

  See also private/developer-documentation/frontend/client-side-routing.md
 */
export function getRouter() {
  const BROWSE = ROUTE_NAMES.BROWSE;

  const router = new UIRouterReact();
  router.plugin(servicesPlugin);
  router.plugin(hashLocationPlugin);
  router.urlService.rules.initial({state: BROWSE.WELCOME });

  // validate permissions and configuration on each route request
  router.transitionService.onBefore({}, async (transition) => {
    const redirectTo404 = () => {
      transition.abort();
      router.stateService.go(ROUTE_NAMES.MISSING_ROUTE);
    }

    const state = transition.to();

    console.debug(`transition from ${transition.from().name} to ${transition.to().name}`);
    if (!isVisible(state.data?.visibilityRequirements)) {
      if (!NX.Security.hasUser()) {
        const result =
            await offerUserTheChanceToLoginAndRevalidate(transition, state.data?.visibilityRequirements);

        if (result !== success) {
          console.warn('state is not visible for navigation after authentication prompt, aborting transition');
          redirectTo404();
        }
      } else {
        console.warn('state is not visible for navigation, aborting transition', state.name);
        redirectTo404();
      }
    }
  });

  router.stateRegistry.register({
    name: 'missingroute',
    url: '/missingroute',
    component: MissingRoutePage,
    visibilityRequirements: {}
  });

  browseRoutes.forEach((route) => router.stateRegistry.register(route));
  adminRoutes.forEach((route) => router.stateRegistry.register(route));
  userRoutes.forEach((route) => router.stateRegistry.register(route));

  // register 404 route
  router.stateRegistry.register({
    name: ROUTE_NAMES.MISSING_ROUTE,
    url: '404',
    component: MissingRoutePage,
    visibilityRequirements: {}
  });

  // send any unrecognized routes to the 404 page
  router.urlService.rules.otherwise({ state: ROUTE_NAMES.MISSING_ROUTE });

  console.info('States added to router:', router.stateRegistry.get());

  return router;
}

async function offerUserTheChanceToLoginAndRevalidate(transition, visibilityRequirements) {
  try {
    const loginResult = await promptUserToLogin();

    // abort if the user cancelled out of the login dialog
    if (loginResult === 'canceled') {
      console.debug('user canceled authentication');
      return failure;
    }

    // wait for extjs to update permissions
    await waitForNextPermissionChange();

    console.debug('rechecking visiblity requirements after authentication');
    if (!isVisible(visibilityRequirements)) {
      console.debug('user still does not have permissions after logging in')
      return failure;
    }
  } catch (ex) {
    console.warn('login unsuccessful: ', ex);
    return failure;
  }

  return success;
}

function promptUserToLogin() {
  return new Promise((resolve, reject) => {
    NX.Security.askToAuthenticate(null, {
      success: () => {
        resolve('success');
      },
      cancel: () => {
        resolve('canceled');
      },
      failure: (err) => {
        reject(err);
      }
    })
  });
}

function waitForNextPermissionChange() {
  return new Promise((resolve, reject) => {
    const handleChange = () => {
      console.debug('received permission changes');
      clearInterval(timeout);
      resolve();
    }

    console.debug('setting up event handler to wait for permission changes');
    const permissionsController = Ext.getApplication().getController('Permissions');
    const eventHandler = permissionsController.on({ changed: handleChange, single: true });

    const timeout = setTimeout(() => {
      console.debug('removing event handler, permission changes have timed out');
      permissionsController.removeHandler(eventHandler);
      reject(new Error('timed out waiting for permissions to update'));
    }, 1000);
  });
}
