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

import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import { UIRouter } from '@uirouter/react';
import { App } from './App';
import { getRouter } from './routerConfig/routerConfig';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { helperFunctions } from './components/widgets/SystemStatusAlerts/CELimits/UsageHelper';

// mocking out the Welcome page to avoid having to mock all the various ExtJs functions/state required to render it
jest.mock('./components/pages/user/Welcome/Welcome', () => {
  return () => (<main><h1>Welcome Test Mock</h1></main>);
});

jest.mock('./components/pages/admin/Api/Api', () => {
  return () => (<main><h1>API Mock</h1></main>);
});

jest.mock('./components/pages/admin/Users/UsersExt', () => {
  return () => (<main><h1>UserExt Mock</h1></main>)
});

describe('App', () => {
  let historySpy;

  beforeEach(() => {
    givenExtJSState();
    givenUser();
    historySpy = jest.spyOn(History.prototype, 'pushState');

    window.location.hash = '';
  });

  it('should render', async () => {
    renderComponent();

    await assertBasicPageLayoutRenders();
  });

  describe('Community Edition Hard Limit Banner', () => {
    // We will just a simple test here to make sure the banner is rendered in the context of the page
    // Full testing the CEHardLimitAlert logic has its own test suite
    it('should render given a community edition is over the limit and an admin user is logged in', async () => {
      const givenGracePeriodEndDate = givenDateNDaysInTheFuture(20);

      givenUseState({
        [helperFunctions.useThrottlingStatusValue]: 'Over limits',
        [helperFunctions.useGracePeriodEndsDate]: givenGracePeriodEndDate,
        [helperFunctions.useDaysUntilGracePeriodEnds]: 12
      });

      await renderComponent();

      await assertBasicPageLayoutRenders();
      await assertCommunityEditionLimitMessageShowing(
          '20 Days Remaining',
          'This instance of Nexus Repository Community Edition has exceeded its usage limit.');
    });
  });

  describe('Login Prompting', () => {
    beforeEach(() => {
      global.NX.Security.askToAuthenticate.mockImplementation((msg, { success }) => success());
    });

    it('shows login given user does not have permissions to view a route and is not authenticated', async () => {
      global.NX.Security.hasUser.mockReturnValue(false);

      global.Ext.getApplication = jest.fn().mockReturnValue({
        getStore: jest.fn().mockReturnValue({
          on: jest.fn(),
          un: jest.fn()
        }),
        getController: jest.fn().mockImplementation((key) => {
          if (key === 'Permissions') {
            return {
              on: jest.fn().mockImplementation((handler) => {
                if (handler.changed) {
                  changed();
                }
              }),
              un: jest.fn()
            }
          } else {
            return {
              on: jest.fn(),
              un: jest.fn()
            }
          }
        })
      });

      const { router } = await renderComponent();

      await assertBasicPageLayoutRenders();

      // the transaction should still fail because even though we resolved the login prompt successfully upon
      // re-checking visiblity we'll find the user still does not have permissions
      let errorOnTransition = null;
      try {
        await router.stateService.go('admin.security.users')
      } catch (ex) {
        errorOnTransition = ex.message
      }

      expect(errorOnTransition).toEqual('The transition has been aborted')

      await waitFor(() => {
        expect(global.NX.Security.askToAuthenticate).toHaveBeenCalledWith(null, expect.objectContaining({
          success: expect.any(Function),
          cancel: expect.any(Function),
          failure: expect.any(Function)
        }));
      });
    });

    it('does not show for login given user does not have permissions but is already authenticated', async () => {
      global.NX.Security.hasUser.mockReturnValue(true);

      const { router } = await renderComponent();

      // the transaction should still fail because even though we resolved the login prompt successfully upon
      // re-checking visiblity we'll find the user still does not have permissions
      let errorOnTransition = null;
      try {
        await router.stateService.go('admin.security.users')
      } catch (ex) {
        errorOnTransition = ex.message
      }

      expect(errorOnTransition).toEqual('The transition has been aborted')

      await assertMissingRoutePageRendered();

      await waitFor(() => {
        expect(global.NX.Security.askToAuthenticate).not.toHaveBeenCalled();
      });
    });
  });

  it('should direct to 404 when page not found', async () => {
    const { router } = await renderComponent();
    await assertBasicPageLayoutRenders();

    router.urlService.url('some-page-that-does-not-exist', false);

    await assertMissingRoutePageRendered();
  });

  it("history hash pushState is intercepted and ignored", async () => {
    await renderComponent();

    history.pushState({}, '', '#');
    expect(historySpy).not.toHaveBeenCalled();

    history.pushState({}, '', '');
    expect(historySpy).toHaveBeenCalledWith({}, '', '');
  });

  async function renderComponent() {
    const router = getRouter();

    const renderResult = render(
        <UIRouter router={router}>
          <App />
        </UIRouter>
      );

    return { renderResult, router }
  }

  async function assertBasicPageLayoutRenders() {
    await assertRendersPageContents();
    await assertRendersGlobalHeader();
    await assertRendersLeftNav();
  }

  async function assertRendersGlobalHeader() {
    const banner = await screen.findByRole('banner');
    expect(banner).toBeVisible();
  }

  async function assertRendersLeftNav() {
    const sideNav = await screen.findByRole('navigation', 'global sidebar');
    expect(sideNav).toBeVisible();
  }

  async function assertRendersPageContents() {
    const main = await screen.findByRole('main');
    expect(main).toBeVisible();
    expect(within(main).getByRole('heading', 'Welcome Test Mock')).toBeVisible();
  }

  function assertCommunityEditionLimitMessageShowing(title, message) {
    const alert = screen.getByRole('complementary', { name: 'alert system notice'});
    expect(alert).toBeVisible();
    expect(within(alert).getByRole('heading', { name: title })).toBeVisible();
    expect(within(alert).getByText(message, { exact: false })).toBeVisible()
  }

  function givenExtJSState(values = getDefaultState(), edition = 'COMMUNITY') {
    const getValueMock = jest.fn().mockImplementation((key, defaultValue) => {
      return values[key] || defaultValue;
    });

    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getEdition: jest.fn().mockReturnValue(edition),
      getVersionMajorMinor: jest.fn().mockReturnValue('1.2.3-some-version'),
      getValue: getValueMock,
      getUser: jest.fn()
    });

    global.NX.State.getValue = getValueMock;
  }

  function givenUseState(values = {}) {
    jest.spyOn(ExtJS, 'useState').mockImplementation((key) => values[key]);
  }

  function getDefaultState() {
    return {
      usertoken: { licenseValid: true },
      dbUpgrade: { currentState: null }
    }
  }

  function givenUser(user = { name: 'admin', administrator: true }) {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue(user);
  }

  function givenDateNDaysInTheFuture(days) {
    const date = new Date()
    date.setDate(date.getDate() + days);

    return date;
  }

  async function assertMissingRoutePageRendered() {
    expect(await screen.findByRole('heading', { name: '404' })).toBeVisible();
    expect(screen.getByRole('heading', {name: 'RESOURCE NOT FOUND'})).toBeInTheDocument();
  }
});
