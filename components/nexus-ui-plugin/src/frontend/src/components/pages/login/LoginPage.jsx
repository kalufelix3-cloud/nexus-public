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

import React, { useState } from 'react';
import { 
  NxTile, 
  NxButton, 
  NxTextInput,
  NxFieldset,
  NxFormGroup
} from '@sonatype/react-shared-components';
import LoginLayout from '../../layout/LoginLayout';
import UIStrings from '../../../constants/UIStrings';

const {LOGIN_TITLE, LOGIN_SUBTITLE, USERNAME_LABEL, PASSWORD_LABEL, LOGIN_BUTTON} = UIStrings;

import './LoginPage.scss';

/**
 * Login page component that renders within LoginLayout.
 * Displays a welcome message and login form matching the design specification.
 * @param {Object} logoConfig - Logo configuration passed to LoginLayout
 */
export default function LoginPage({ logoConfig }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
  };

  return (
    <LoginLayout logoConfig={logoConfig}>
      <div className="login-page">
        <NxTile className="login-tile" data-testid="login-tile">
          <NxTile.Header>
            <NxTile.HeaderTitle>
              {LOGIN_TITLE}
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>
              {LOGIN_SUBTITLE}
            </NxTile.HeaderSubtitle>
          </NxTile.Header>
          <NxTile.Content>
            <div className="login-content">              
              <form className="login-form" onSubmit={handleSubmit}>
                <NxFieldset>
                  <NxFormGroup label={USERNAME_LABEL} className="username-field">
                    <NxTextInput
                      id="username"
                      value={username}
                      onChange={setUsername}
                      placeholder={USERNAME_LABEL}
                      isPristine={false}
                    />
                  </NxFormGroup>
                  
                  <NxFormGroup label={PASSWORD_LABEL}>
                    <NxTextInput
                      id="password"
                      type="password"
                      value={password}
                      onChange={setPassword}
                      placeholder={PASSWORD_LABEL}
                      isPristine={false}
                    />
                  </NxFormGroup>
                  
                  <NxButton
                    type="submit"
                    variant="primary"
                    className="login-button"
                  >
                    {LOGIN_BUTTON}
                  </NxButton>
                </NxFieldset>
              </form>
            </div>
          </NxTile.Content>
        </NxTile>
      </div>
    </LoginLayout>
  );
}
