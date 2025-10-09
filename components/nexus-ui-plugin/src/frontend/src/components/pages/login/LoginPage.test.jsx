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

jest.mock('./LoginForm', () => {
  return function LoginForm(props) {
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

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: jest.fn()
    }
  })
}));

jest.mock('@sonatype/nexus-ui-plugin');

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

  describe('self-hosted environment', () => {
    it('renders login form when no SSO is enabled', () => {
      setupStates(false, false, false);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when SAML is enabled', () => {
      setupStates(true, false, false);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('renders both SSO button and login form with divider when OAuth2 is enabled', () => {
      setupStates(false, true, false);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      // Divider should appear when both SSO and local login are shown
      expect(screen.getByText(UIStrings.SSO_DIVIDER_LABEL)).toBeInTheDocument();
    });

    it('sets LoginForm primaryButton to false when SSO is enabled', () => {
      setupStates(true, false, false);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'false');
    });

    it('sets LoginForm primaryButton to true when SSO is not enabled', () => {
      setupStates(false, false, false);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      const loginForm = screen.getByTestId('login-form');
      expect(loginForm).toHaveAttribute('data-primary-button', 'true');
    });
  });

  describe('cloud environment', () => {
    it('does not render login form in cloud environment without SSO', () => {
      setupStates(false, false, true);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_TITLE)).toBeInTheDocument();
      expect(screen.getByText(UIStrings.LOGIN_SUBTITLE)).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      expect(screen.queryByTestId('sso-login-button')).not.toBeInTheDocument();
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with OAuth2 enabled', () => {
      setupStates(false, true, true);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with SAML enabled', () => {
      setupStates(true, false, true);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
      // No divider because local login is not shown in cloud
      expect(screen.queryByText(UIStrings.SSO_DIVIDER_LABEL)).not.toBeInTheDocument();
    });

    it('renders only SSO button without divider in cloud environment with both SAML and OAuth2 enabled', () => {
      setupStates(true, true, true);
      
      render(<LoginPage logoConfig={mockLogoConfig} />);
      
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
      
      render(<LoginPage logoConfig={customLogoConfig} />);
      
      expect(screen.getByTestId('login-layout')).toBeInTheDocument();
    });
  });

  describe('anonymous access', () => {
    it('does not render anonymous access button when anonymous username is not configured', () => {
      setupStates(false, false, false, null);

      render(<LoginPage logoConfig={mockLogoConfig} />);

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.queryByTestId('continue-without-login-button')).not.toBeInTheDocument();
    });

    it('renders anonymous access button when anonymous username is configured', () => {
      setupStates(false, false, false, 'anonymous');

      render(<LoginPage logoConfig={mockLogoConfig} />);

      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('renders anonymous access button with SSO enabled', () => {
      setupStates(true, false, false, 'anonymous');

      render(<LoginPage logoConfig={mockLogoConfig} />);

      expect(screen.getByTestId('sso-login-button')).toBeInTheDocument();
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
      expect(screen.getByTestId('continue-without-login-button')).toBeInTheDocument();
    });

    it('does not render anonymous access button when anonymous username is empty string', () => {
      setupStates(false, false, false, '');

      render(<LoginPage logoConfig={mockLogoConfig} />);

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

      render(<LoginPage logoConfig={mockLogoConfig} />);

      // Should default to self-hosted behavior
      expect(screen.getByTestId('login-form')).toBeInTheDocument();
    });

    it('handles missing logoConfig gracefully', () => {
      setupStates(false, false, false, null);

      render(<LoginPage />);

      expect(screen.getByTestId('login-tile')).toBeInTheDocument();
    });
  });
});
