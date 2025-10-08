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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import SsoLoginButton from './SsoLoginButton';

jest.mock('@sonatype/nexus-ui-plugin');

describe('SsoLoginButton', () => {
  const mockUseState = jest.fn();
  const mockState = jest.fn();

  beforeEach(() => {
    ExtJS.useState = mockUseState;
    ExtJS.state = mockState;
    mockState.mockReturnValue({
      getValue: jest.fn()
    });

    Object.defineProperty(window, 'location', {
      value: {
        assign: jest.fn()
      },
      writable: true
    });

    jest.clearAllMocks();
  });

  describe('visibility', () => {
    it('renders null when neither SAML nor OAuth2 is enabled', () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      const { container } = render(<SsoLoginButton />);
      expect(container.firstChild).toBeNull();
    });

    it('renders button when SAML is enabled', () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when OAuth2 is enabled', () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });

    it('renders button when both SAML and OAuth2 are enabled', () => {
      mockUseState
        .mockReturnValueOnce(true) // samlEnabled
        .mockReturnValueOnce(true) // oauth2Enabled
        .mockReturnValueOnce('');  // contextPath

      render(<SsoLoginButton />);
      expect(screen.getByRole('button', { name: /continue with sso/i })).toBeInTheDocument();
    });
  });

  describe('button properties', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('has correct attributes', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('type', 'button');
      expect(button).toHaveAttribute('data-analytics-id', 'sso-login-button');
      expect(button).toHaveClass('sso-login-button');
    });

    it('displays correct text when not loading', () => {
      render(<SsoLoginButton />);
      expect(screen.getByText('Continue with SSO')).toBeInTheDocument();
    });
  });

  describe('SAML redirect', () => {
    it('redirects to /saml with empty context path', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });

    it('redirects to context path + /saml with context path', async () => {
      mockUseState
        .mockReturnValueOnce(true)      // samlEnabled
        .mockReturnValueOnce(false)     // oauth2Enabled
        .mockReturnValueOnce('/nexus'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml');
      });
    });

    it('handles context path with trailing slash', async () => {
      mockUseState
        .mockReturnValueOnce(true)       // samlEnabled
        .mockReturnValueOnce(false)      // oauth2Enabled
        .mockReturnValueOnce('/nexus/'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/saml');
      });
    });
  });

  describe('OAuth2 redirect', () => {
    it('redirects to /oidc/login with empty context path', async () => {
      mockUseState
        .mockReturnValueOnce(false) // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/oidc/login');
      });
    });

    it('redirects to context path + /oidc/login with context path', async () => {
      mockUseState
        .mockReturnValueOnce(false)     // samlEnabled
        .mockReturnValueOnce(true)      // oauth2Enabled
        .mockReturnValueOnce('/nexus'); // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/nexus/oidc/login');
      });
    });
  });

  describe('SAML priority', () => {
    it('prefers SAML when both SAML and OAuth2 are enabled', async () => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(true)  // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath

      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith('/saml');
      });
    });
  });

  describe('button interaction', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('triggers redirect when clicked', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      fireEvent.click(button);

      expect(window.location.assign).toHaveBeenCalledWith('/saml');
    });
  });

  describe('accessibility', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('is keyboard accessible', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      // Check that the button can receive focus and has proper attributes
      expect(button).toHaveAttribute('type', 'button');
      expect(button).not.toHaveAttribute('tabindex', '-1');
    });
  });

  describe('analytics', () => {
    beforeEach(() => {
      mockUseState
        .mockReturnValueOnce(true)  // samlEnabled
        .mockReturnValueOnce(false) // oauth2Enabled
        .mockReturnValueOnce('');   // contextPath
    });

    it('has analytics attribute', () => {
      render(<SsoLoginButton />);
      const button = screen.getByRole('button');

      expect(button).toHaveAttribute('data-analytics-id', 'sso-login-button');
    });
  });
});
