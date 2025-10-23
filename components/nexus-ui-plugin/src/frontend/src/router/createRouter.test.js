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
import { getTestRouter } from './testRouter';
import ExtJS from '../interface/ExtJS';

jest.mock('../interface/ExtJS');

describe('createRouter - login redirect', () => {
  let router;

  beforeEach(() => {
    ExtJS.hasUser = jest.fn().mockReturnValue(false);
    ExtJS.state = jest.fn().mockReturnValue({
      getValue: jest.fn().mockReturnValue(false)
    });

    router = getTestRouter();
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should redirect authenticated users away from login page', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    const loginRoute = {
      name: 'login',
      url: '/login',
      component: () => null,
      data: { visibilityRequirements: {} }
    };

    router.stateRegistry.register(loginRoute);

    await router.urlService.sync();

    const originalGo = router.stateService.go.bind(router.stateService);
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation((...args) => {
      return originalGo(...args);
    });

    await router.stateService.go('login').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('browse.welcome');

    goSpy.mockRestore();
  });

  it('should allow unauthenticated users to access login page', async () => {
    ExtJS.hasUser.mockReturnValue(false);

    const loginRoute = {
      name: 'login',
      url: '/login',
      component: () => null,
      data: { visibilityRequirements: {} }
    };

    router.stateRegistry.register(loginRoute);

    await router.urlService.sync();

    await router.stateService.go('login');

    expect(router.stateService.current.name).toBe('login');
  });

  it('should allow authenticated users to access non-login pages', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    await router.urlService.sync();

    await router.stateService.go('browse.welcome');

    expect(router.stateService.current.name).toBe('browse.welcome');
    expect(router.stateService.current.name).not.toBe('login');
  });

  it('should redirect authenticated users away from login page to default route', async () => {
    ExtJS.hasUser.mockReturnValue(true);

    const loginRoute = {
      name: 'login',
      url: '/login',
      component: () => null,
      data: { visibilityRequirements: {} }
    };

    router.stateRegistry.register(loginRoute);

    await router.urlService.sync();

    const originalGo = router.stateService.go.bind(router.stateService);
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation((...args) => {
      return originalGo(...args);
    });

    await router.stateService.go('login').catch(() => {});

    expect(goSpy).toHaveBeenCalledWith('browse.welcome');
    expect(router.stateService.current.name).not.toBe('login');

    goSpy.mockRestore();
  });
});
