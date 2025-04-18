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
import { UIView, useCurrentStateAndParams, useRouter } from '@uirouter/react';

import SettingsSidebar from './SettingsSidebar';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import { useIsSettingsVisible } from '../useIsSettingsVisible';

export default function SettingsPageLayout() {
  const { ADMIN, BROWSE } = ROUTE_NAMES;

  const router = useRouter();

  const {
    state: { name: currentPageName },
  } = useCurrentStateAndParams();
  const isRepositorySectionVisible = useIsSettingsVisible(`${ADMIN.REPOSITORY.DIRECTORY}.`);
  const isSecuritySectionVisible = useIsSettingsVisible(`${ADMIN.SECURITY.DIRECTORY}.`);
  const isSystemSectionVisible = useIsSettingsVisible(`${ADMIN.SYSTEM.DIRECTORY}.`);
  const isSupportSectionVisible = useIsSettingsVisible(`${ADMIN.SUPPORT.DIRECTORY}.`);
  const isIQSectionVisible = useIsSettingsVisible(`${ADMIN.IQ}`);

  if (currentPageName === ADMIN.DIRECTORY) {
    if (isRepositorySectionVisible) {
      router.stateService.go(ADMIN.REPOSITORY.DIRECTORY);
    } else if (isSecuritySectionVisible) {
      router.stateService.go(ADMIN.SECURITY.DIRECTORY);
    } else if (isSystemSectionVisible) {
      router.stateService.go(ADMIN.SYSTEM.DIRECTORY);
    } else if (isSupportSectionVisible) {
      router.stateService.go(ADMIN.SUPPORT.DIRECTORY);
    } else if (isIQSectionVisible) {
      router.stateService.go(ADMIN.IQ);
    } else {
      router.stateService.go(BROWSE.WELCOME);
    }
  }

  return (
    <>
      <SettingsSidebar />
      <UIView />
    </>
  );
}
