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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import LoginPageStrings from '../../../constants/LoginPageStrings';
import ExtJS from '../../../interface/ExtJS';
import LoginForm from './LoginForm';

jest.mock('../../../interface/ExtJS');

jest.setTimeout(15000);

describe('LoginForm', () => {
  const selectors = {
    usernameInput: () => screen.getByPlaceholderText(LoginPageStrings.USERNAME_LABEL),
    passwordInput: () => screen.getByPlaceholderText(LoginPageStrings.PASSWORD_LABEL),
    loginButton: () => screen.getByTestId('login-primary-button'),
    loadingButton: () => screen.queryByText(LoginPageStrings.LOGIN_BUTTON_LOADING),
    serverErrorMessage: () => document.querySelector('.server-error-message')
  };

  let onSuccessSpy;
  let onErrorSpy;

  beforeEach(() => {
    onSuccessSpy = jest.fn();
    onErrorSpy = jest.fn();
    ExtJS.requestSession.mockReset();
  });

  function renderComponent(props = {}) {
    const defaultProps = {
      primaryButton: true,
      onSuccess: onSuccessSpy,
      onError: onErrorSpy
    };

    return render(<LoginForm {...defaultProps} {...props} />);
  }

  async function fillCredentials(username, password) {
    const usernameInput = selectors.usernameInput();
    const passwordInput = selectors.passwordInput();

    await userEvent.clear(usernameInput);
    await userEvent.type(usernameInput, username);
    await userEvent.clear(passwordInput);
    await userEvent.type(passwordInput, password);

    await waitFor(() => {
      expect(usernameInput).toHaveValue(username);
      expect(passwordInput).toHaveValue(password);
    });
  }

  describe('rendering', () => {
    it('renders the inputs and submit button', () => {
      renderComponent();

      expect(selectors.usernameInput()).toBeInTheDocument();
      expect(selectors.passwordInput()).toBeInTheDocument();
      expect(selectors.loginButton()).toBeInTheDocument();
      expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
    });

    it('uses primary button styling by default', () => {
      renderComponent();

      expect(selectors.loginButton()).toHaveClass('nx-btn--primary');
    });

    it('switches to default button styling when primary=false', () => {
      renderComponent({ primaryButton: false });

      const loginButton = selectors.loginButton();
      expect(loginButton).not.toHaveClass('nx-btn--primary');
      expect(loginButton).toHaveClass('nx-btn');
    });

    it('adds analytics ids to form controls', () => {
      renderComponent();

      const usernameInput = document.querySelector('[data-analytics-id="login-username-input"]');
      const passwordInput = document.querySelector('[data-analytics-id="login-password-input"]');
      const submitButton = document.querySelector('[data-analytics-id="login-submit-button"]');

      expect(usernameInput).toBeInTheDocument();
      expect(passwordInput).toBeInTheDocument();
      expect(submitButton).toBe(selectors.loginButton());
    });

    it('shows loading state while authentication is pending', async () => {
      renderComponent();
      await fillCredentials('testuser', 'testpass');

      // Create a promise that we can control
      let resolvePromise;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      ExtJS.requestSession.mockImplementation(() => pendingPromise);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.loadingButton()).toBeInTheDocument();
      }, { timeout: 3000 });

      expect(selectors.usernameInput()).toBeDisabled();
      expect(selectors.passwordInput()).toBeDisabled();

      // Clean up by resolving the promise
      resolvePromise({ status: 204 });
    }, 20000); // Increase test timeout
  });

  describe('validation', () => {
    it('shows username required error when submitting empty username', async () => {
      renderComponent();

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        const usernameInput = selectors.usernameInput();
        expect(usernameInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('shows password required error when submitting empty password', async () => {
      renderComponent();
      const usernameInput = selectors.usernameInput();

      await userEvent.type(usernameInput, 'testuser');
      await waitFor(() => expect(usernameInput).toHaveValue('testuser'));

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        const passwordInput = selectors.passwordInput();
        expect(passwordInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('shows both username and password required errors when both are empty', async () => {
      renderComponent();

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });
    });
  });

  describe('form submission', () => {
    it('invokes authenticate on submit and notifies success', async () => {
      renderComponent();
      await fillCredentials('admin', 'admin123');

      ExtJS.requestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(ExtJS.requestSession).toHaveBeenCalledWith('admin', 'admin123');
        expect(onSuccessSpy).toHaveBeenCalledWith({
          username: 'admin',
          response: { status: 204 }
        });
      });
    });

    it('clears password field on authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'badpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.passwordInput()).toHaveValue('');
      });
      expect(onErrorSpy).toHaveBeenCalledWith(authError);

      expect(selectors.usernameInput()).toHaveValue('admin');
      
      expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
      expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      expect(screen.queryByText(LoginPageStrings.ERRORS.USERNAME_REQUIRED)).not.toBeInTheDocument();
      expect(screen.queryByText(LoginPageStrings.ERRORS.PASSWORD_REQUIRED)).not.toBeInTheDocument();
    });

    it('displays connection errors returned by the server', async () => {
      renderComponent();
      await fillCredentials('admin', 'oops');

      const failure = new Error('Server error');
      failure.response = { status: 500, data: { message: 'Authentication failed' } };
      ExtJS.requestSession.mockRejectedValue(failure);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => expect(selectors.serverErrorMessage()).toHaveTextContent('Authentication failed'));
      expect(onErrorSpy).toHaveBeenCalledWith(failure);
    });

    it('focuses username input after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toBeInTheDocument();
      });

      expect(document.activeElement).toBe(document.getElementById('username'));
    });

    it('allows user to retry after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
      });

      ExtJS.requestSession.mockResolvedValue({ response: { status: 204 } });
      await userEvent.type(selectors.passwordInput(), 'correctpass');

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(onSuccessSpy).toHaveBeenCalled();
      });
    });

    it('clears error message when user modifies username after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });

      await userEvent.type(selectors.usernameInput(), 'x');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
        expect(selectors.usernameInput()).not.toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).not.toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('clears error message when user modifies password after authentication error', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
        expect(selectors.usernameInput()).toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).toHaveAttribute('aria-invalid', 'true');
      });

      // User starts correcting password (which was cleared after error)
      await userEvent.type(selectors.passwordInput(), 'newpass');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
        expect(selectors.usernameInput()).not.toHaveAttribute('aria-invalid', 'true');
        expect(selectors.passwordInput()).not.toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('clears error message and allows resubmission after user corrects credentials', async () => {
      renderComponent();
      await fillCredentials('admin', 'wrongpass');

      const authError = new Error('Forbidden');
      authError.response = { status: 403, data: {} };
      ExtJS.requestSession.mockRejectedValue(authError);

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).toHaveTextContent(LoginPageStrings.ERRORS.WRONG_CREDENTIALS);
      });

      await userEvent.clear(selectors.usernameInput());
      await userEvent.type(selectors.usernameInput(), 'correctuser');

      await waitFor(() => {
        expect(selectors.serverErrorMessage()).not.toBeInTheDocument();
      });

      await userEvent.type(selectors.passwordInput(), 'correctpass');

      ExtJS.requestSession.mockResolvedValue({ response: { status: 204 } });

      await act(async () => {
        await userEvent.click(selectors.loginButton());
      });

      await waitFor(() => {
        expect(onSuccessSpy).toHaveBeenCalledWith({
          username: 'correctuser',
          response: { status: 204 }
        });
      });
    });
  });

  describe('field interactions', () => {
    it('updates username and password values as the user types', async () => {
      renderComponent();

      await fillCredentials('my-user', 'my-pass');

      expect(selectors.usernameInput()).toHaveValue('my-user');
      expect(selectors.passwordInput()).toHaveValue('my-pass');
    });

    it('toggles password visibility', async () => {
      renderComponent();
      await userEvent.type(selectors.passwordInput(), 'mypassword');

      const showButton = screen.getByLabelText('Show password');
      expect(selectors.passwordInput()).toHaveAttribute('type', 'password');

      await act(async () => {
        await userEvent.click(showButton);
      });

      expect(selectors.passwordInput()).toHaveAttribute('type', 'text');
      const hideButton = screen.getByLabelText('Hide password');

      await act(async () => {
        await userEvent.click(hideButton);
      });

      expect(selectors.passwordInput()).toHaveAttribute('type', 'password');
    });
  });
});
