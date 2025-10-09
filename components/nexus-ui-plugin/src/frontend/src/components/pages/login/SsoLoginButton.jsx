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

import React, { useState, useMemo } from 'react';
import { NxButton, NxLoadingSpinner } from '@sonatype/react-shared-components';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';

const { SSO_BUTTON, SSO_BUTTON_LOADING } = UIStrings;

/**
 * @param {string} contextPath - The application context path (e.g., '/nexus')
 * @param {string} path - The target path (e.g., '/saml')
 */
function buildRedirectUrl(contextPath, path) {
  // Handle empty context path
  if (!contextPath || contextPath === '/') {
    return path;
  }
  
  // Remove trailing slashes from context path and leading slashes from path
  const cleanContextPath = contextPath.replace(/\/+$/, '');
  const cleanPath = path.replace(/^\/+/, '');
  
  return `${cleanContextPath}/${cleanPath}`;
}

/**
 * SSO Login Button component that redirects to the appropriate authentication endpoint
 * based on the configured SSO method (SAML or OAuth2/OIDC)
 */
export default function SsoLoginButton() {
  const [isRedirecting, setIsRedirecting] = useState(false);

  const samlEnabled = ExtJS.useState(() => ExtJS.state().getValue('samlEnabled', false));
  const oauth2Enabled = ExtJS.useState(() => ExtJS.state().getValue('oauth2Enabled', false));
  const contextPath = ExtJS.useState(() => ExtJS.state().getValue('nexus-context-path', ''));

  const redirectUrl = useMemo(() => {
    if (samlEnabled) {
      return buildRedirectUrl(contextPath, '/saml');
    }
    if (oauth2Enabled) {
      return buildRedirectUrl(contextPath, '/oidc/login');
    }
    return null;
  }, [samlEnabled, oauth2Enabled, contextPath]);

  if (!redirectUrl) {
    return null;
  }

  const handleSsoLogin = () => {
    setIsRedirecting(true);
    window.location.assign(redirectUrl);
  };

  return (
    <NxButton
      type="button"
      variant="primary"
      onClick={handleSsoLogin}
      disabled={isRedirecting}
      className="sso-login-button"
      data-analytics-id="nxrm-login-sso"
    >
      {isRedirecting ? (
        <NxLoadingSpinner>
          {SSO_BUTTON_LOADING}
        </NxLoadingSpinner>
      ) : (
        SSO_BUTTON
      )}
    </NxButton>
  );
}
