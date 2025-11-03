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
import { NxTile } from '@sonatype/react-shared-components';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import LoginLayout from '../../layout/LoginLayout';
import LocalLogin from './LocalLogin';
import SsoLogin from './SsoLogin';
import AnonymousAccess from './AnonymousAccess';
import InitialPasswordInfo from './InitialPasswordInfo';

const { LOGIN_TITLE, LOGIN_SUBTITLE, SSO_DIVIDER_LABEL } = UIStrings;

import './LoginPage.scss';

/**
 * Login page component that renders within LoginLayout.
 * Displays a welcome message and login form matching the design specification.
 * @param {Object} logoConfig - Logo configuration passed to LoginLayout
 */
export default function LoginPage({ logoConfig }) {
  const samlEnabled = ExtJS.useState(() => ExtJS.state().getValue('samlEnabled', false));
  const oauth2Enabled = ExtJS.useState(() => ExtJS.state().getValue('oauth2Enabled', false));
  const isCloudEnvironment = ExtJS.useState(() => ExtJS.state().getValue('isCloud', false));
  const anonymousUsername = ExtJS.useState(() => ExtJS.state().getValue('anonymousUsername'));
  const adminPasswordFilePath = ExtJS.useState(() => ExtJS.state().getValue('admin.password.file'));
  const isSsoEnabled = samlEnabled || oauth2Enabled;
  const isAnonymousAccessEnabled = !!anonymousUsername;

  const showLocalLogin = !isCloudEnvironment;
  const showInitialPasswordPathInfo = !!adminPasswordFilePath && !isCloudEnvironment;

  return (
    <LoginLayout logoConfig={logoConfig}>
      <div className="login-page">
        <NxTile className="login-tile" data-testid="login-tile">
          <NxTile.Header>
            <NxTile.HeaderTitle>{LOGIN_TITLE}</NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>{LOGIN_SUBTITLE}</NxTile.HeaderSubtitle>
          </NxTile.Header>
          <NxTile.Content>
            <div className="login-content">
              {showInitialPasswordPathInfo && <InitialPasswordInfo passwordFilePath={adminPasswordFilePath} />}
              {isSsoEnabled && (
                <>
                  <SsoLogin />
                  {showLocalLogin && (
                    <div className="login-divider" aria-hidden="true">
                      <span>{SSO_DIVIDER_LABEL}</span>
                    </div>
                  )}
                </>
              )}
              {showLocalLogin && (
                <LocalLogin primaryButton={!isSsoEnabled} />
              )}
              {isAnonymousAccessEnabled && (
                <AnonymousAccess />
              )}
            </div>
          </NxTile.Content>
        </NxTile>
      </div>
    </LoginLayout>
  );
}
