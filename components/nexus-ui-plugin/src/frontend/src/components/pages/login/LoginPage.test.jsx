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
import { render, screen } from '@testing-library/react';

import { ExtJS } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import LoginPage from './LoginPage';

// Mock the child components
jest.mock('../../layout/LoginLayout', () => {
  return function LoginLayout({ children }) {
    return <div data-testid="login-layout">{children}</div>;
  };
});

const mockLoginFormOnSuccess = jest.fn();
const mockLoginFormOnError = jest.fn();

jest.mock('./LoginForm', () => {
  return function LoginForm(props) {
    mockLoginFormOnSuccess.mockImplementation(props.onSuccess);
    mockLoginFormOnError.mockImplementation(props.onError);
    return <div data-testid="login-form" data-primary-button={props.primaryButton}>Login Form</div>;
  };
});

jest.mock('./SsoLoginButton', () => {
  return function SsoLoginButton() {
    return <button data-testid="sso-login-button">SSO Login</button>;
  };
});

jest.mock('./AnonymousAccessButton', () => {
  return function AnonymousAccessButton({ onClick }) {
    return <button data-testid="continue-without-login-button" onClick={onClick}>Continue without login</button>;
  };
});

const mockRouterGo = jest.fn();
const mockRouterUrl = jest.fn();
const mockRouterParams = {};

jest.mock('@uirouter/react', () => {
  const actualModule = jest.requireActual('@uirouter/react');
  return {
    ...actualModule,
    useRouter: () => ({
      stateService: {
        go: mockRouterGo
      },
      urlService: {
        url: mockRouterUrl
      },
      globals: {
        params: mockRouterParams
      }
    })
  };
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn(),
    waitForNextPermissionChange: jest.fn().mockResolvedValue()
  }
}));

describe('LoginPage', () => {
  const mockLogoConfig = { url: 'test-logo.png' };
  const mockUseState = jest.fn();
  const mockState = jest.fn();

  beforeEach(() => {
    ExtJS.useState = mockUseState;
    ExtJS.state = mockState;
    mockState.mockReturnValue({
      getValue: jest.fn().mockImplementation((key, defaultValue) => defaultValue)
    });
    jest.clearAllMocks();
  });

  function setupStates(samlEnabled = false, oauth2Enabled = false, isCloud = false, anonymousUsername = null) {
    mockUseState
      .mockReturnValueOnce(samlEnabled)        // samlEnabled
      .mockReturnValueOnce(oauth2Enabled)      // oauth2Enabled
      .mockReturnValueOnce(isCloud)            // isCloudEnvironment
      .mockReturnValueOnce(anonymousUsername); // anonymousUsername
  }

  function renderComponent(props) {
    return render(<LoginPage {...props} />);
  }

  describe('self-hosted environment', () => {
    it('renders login form when no SSO is enabled', () => {
      setupStates(false, false, false);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when SAML is enabled', () => {
      setupStates(true, false, false);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when OAuth2 is enabled', () => {
      setupStates(false, true, false);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('sets LoginForm primaryButton to false when SSO is enabled', () => {
      setupStates(true, false, false);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'false');
    });

    it('sets LoginForm primaryButton to true when SSO is not enabled', () => {
      setupStates(false, false, false);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'true');
    });
  });

  describe('cloud environment', () => {
    it('does not render login form in cloud environment without SSO', () => {
      setupStates(false, false, true);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with OAuth2 enabled', () => {
      setupStates(false, true, true);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with SAML enabled', () => {
      setupStates(true, false, true);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with both SAML and OAuth2 enabled', () => {
      setupStates(true, true, true);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });
  });

  describe('logo configuration', () => {
    it('passes logoConfig to LoginLayout', () => {
      setupStates(false, false, false);
      const customLogoConfig = { url: 'custom-logo.png', alt: 'Custom Logo' };
      
      renderComponent({ logoConfig: customLogoConfig });
      
      expect(screen.getByTestId('login-layout')).toBeInTheDocument();
    });
  });

  describe('anonymous access', () => {
    it('does not render anonymous access button when anonymous username is not configured', () => {
      setupStates(false, false, false, null);

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('renders anonymous access button when anonymous username is configured', () => {
      setupStates(false, false, false, 'anonymous');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('renders anonymous access button with SSO enabled', () => {
      setupStates(true, false, false, 'anonymous');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('does not render anonymous access button when anonymous username is empty string', () => {
      setupStates(false, false, false, '');

      renderComponent({ logoConfig: mockLogoConfig });

      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('does not render anonymous access button when anonymous username is undefined', () => {
      setupStates(false, false, false, undefined);

      render(<LoginPage logoConfig={mockLogoConfig} />);

      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('handles undefined ExtJS state gracefully', () => {
      mockUseState.mockReturnValue(undefined);

      renderComponent({ logoConfig: mockLogoConfig });

      // Should default to self-hosted behavior
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
    });

    it('handles missing logoConfig gracefully', () => {
      setupStates(false, false, false, null);

      renderComponent({});

      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
    });
  });

  describe('redirection after login', () => {
    beforeEach(() => {
      mockRouterParams.returnTo = undefined;
      mockRouterGo.mockClear();
      mockRouterUrl.mockClear();
      mockLoginFormOnSuccess.mockClear();
    });

    it('redirects to returnTo URL after successful login', async () => {
      setupStates(false, false, false, null);
      mockRouterParams.returnTo = '#admin/repository/repositories';
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      await mockLoginFormOnSuccess({ username: 'testuser' });
      
      expect(mockRouterUrl).toHaveBeenCalledWith('#admin/repository/repositories');
      expect(mockRouterGo).not.toHaveBeenCalled();
    });

    it('redirects to welcome page when no returnTo is provided', async () => {
      setupStates(false, false, false, null);
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      await mockLoginFormOnSuccess({ username: 'testuser' });
      
      expect(mockRouterGo).toHaveBeenCalledWith('browse.welcome');
      expect(mockRouterUrl).not.toHaveBeenCalled();
    });

    it('redirects to missing route page when redirection is unsuccessful', async () => {
      setupStates(false, false, false, null);
      ExtJS.waitForNextPermissionChange.mockRejectedValueOnce(new Error('Permission check failed'));
      
      renderComponent({ logoConfig: mockLogoConfig });
      
      await mockLoginFormOnSuccess({ username: 'testuser' });
      
      expect(mockRouterGo).toHaveBeenCalledWith('missing_route');
    });
  });
});
