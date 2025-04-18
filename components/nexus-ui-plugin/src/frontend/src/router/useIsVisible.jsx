/**
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
<<<<<<<< HEAD:private/plugins/nexus-cloudui-plugin/src/frontend/src/components/UpgradeAlert/UpgradeAlert.scss
.nx-upgrade-alert {
  margin: 0;
  width: 100%;
  max-width: 100%;
}

.upgrade-in-progress-alert {
  text-align: center;
}

.upgrade-alert-btn-bar {
  width: 100%;
  justify-content: space-between;
}

.alert-text {
  display: inline-flex;
========

import ExtJS from '../interface/ExtJS';
import isVisible from './isVisible';

export default function useIsVisible(visibilityRequirements) {
  return ExtJS.useVisiblityWithChanges(() => isVisible(visibilityRequirements));
>>>>>>>> d414d7b5f3 (NEXUS-46919 reduce shared code burden (#12302)):components/nexus-ui-plugin/src/frontend/src/router/useIsVisible.jsx
}
