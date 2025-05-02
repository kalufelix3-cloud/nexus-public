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
import ReactDOM from 'react-dom';
import {UIRouter, UIView} from '@uirouter/react';
import {ExtJS} from '@sonatype/nexus-ui-plugin'

import { getRouter } from './routerConfig/routerConfig';
import LeftNavigationMenu from "./components/LeftNavigationMenu/LeftNavigationMenu";
import GlobalHeader from "./components/GlobalHeader/GlobalHeader";

import './App.scss';
import SystemNotices from './components/widgets/SystemStatusAlerts/SystemNotices';
import UpgradeModal from './components/pages/user/Welcome/UpgradeModal';
import {useRedirectOnLogout} from './hooks/useRedirectOnLogout';
import usePreventPushStateOnHash from './hooks/usePreventPushStateOnHash';

export function App() {
  useRedirectOnLogout();
  usePreventPushStateOnHash();

  return (
      <>
        <SystemNotices/>

        <GlobalHeader/>

        <LeftNavigationMenu/>

        <UpgradeModal/>

        <UIView/>
      </>);
}

ExtJS.waitForExtJs(() => {
  const router = getRouter();
  const el = document.createElement('div');
  el.className = 'nx-page nxrm-page';
  document.body.appendChild(el);
  ReactDOM.render(
      <UIRouter router={router}>
        <App/>
      </UIRouter>, el);
});
