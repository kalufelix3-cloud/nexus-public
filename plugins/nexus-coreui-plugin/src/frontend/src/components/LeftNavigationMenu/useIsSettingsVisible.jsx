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
import { isVisible } from '@sonatype/nexus-ui-plugin';

import { ROUTE_NAMES } from '../../routerConfig/routeNames/routeNames';
import useFilteredRoutes from '../../hooks/useFilteredRoutes';

const ADMIN = ROUTE_NAMES.ADMIN;

export function useIsSettingsVisible(section = `${ADMIN.DIRECTORY}.`) {
  const visibleRoutes = useFilteredRoutes(
    state =>
      state.name.startsWith(section) &&
      !state?.data?.visibilityRequirements?.ignoreForMenuVisibilityCheck &&
      isVisible(state.data.visibilityRequirements)
  );

  return visibleRoutes.length > 0;
}
