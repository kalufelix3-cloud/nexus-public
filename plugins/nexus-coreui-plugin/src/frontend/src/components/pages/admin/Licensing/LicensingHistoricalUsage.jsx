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
import {useMachine} from '@xstate/react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCaretUp, faCaretDown, faMinus, faInfoCircle } from '@fortawesome/free-solid-svg-icons';

import { NxH2, NxP, NxTable, NxTableHead, NxTableRow, NxTableCell, NxTableBody, NxTooltip } from '@sonatype/react-shared-components';

import LicensingHistoricalUsageMachine from './LicensingHistoricalUsageMachine';
import LicensingStrings from '../../../../constants/pages/admin/system/LicensingStrings';
import ChangeIcon from './ChangeIcon';
import './Licensing.scss';

export default function LicensingHistorcalUsage() {
  const [state] = useMachine(LicensingHistoricalUsageMachine, {
      devTools: true
    });
    const {data} = state.context;
  
    const formatPercentage = (value) => {
      if (value == 'N/A') {
        return value;
      }
      return `${Math.abs(value)}%`;
    };

    return (<>
        <NxH2>{LicensingStrings.LICENSING.HISTORICAL_USAGE.TITLE}</NxH2>
      <NxP>{LicensingStrings.LICENSING.HISTORICAL_USAGE.DESCRIPTION}</NxP>
      <NxTable className="licensing-usage-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>{LicensingStrings.LICENSING.HISTORICAL_USAGE.MONTH}</NxTableCell>
            <NxTableCell>{LicensingStrings.LICENSING.HISTORICAL_USAGE.PEAK_COMPONENTS}</NxTableCell>
            <NxTableCell>
              {LicensingStrings.LICENSING.HISTORICAL_USAGE.COMPONENTS_CHANGE}
              <NxTooltip title={LicensingStrings.LICENSING.HISTORICAL_USAGE.COMPONENTS_CHANGE_TOOLTIP}>
                <span><FontAwesomeIcon icon={faInfoCircle} /></span>
              </NxTooltip>
            </NxTableCell>
            <NxTableCell>{LicensingStrings.LICENSING.HISTORICAL_USAGE.TOTAL_REQUESTS}</NxTableCell>
            <NxTableCell>
              {LicensingStrings.LICENSING.HISTORICAL_USAGE.REQUESTS_CHANGE}
              <NxTooltip title={LicensingStrings.LICENSING.HISTORICAL_USAGE.REQUESTS_CHANGE_TOOLTIP}>
                <span><FontAwesomeIcon icon={faInfoCircle} /></span>
              </NxTooltip>
            </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody>
            {data && data.map((item, index) => (
              <NxTableRow key={index}>
                <NxTableCell>{new Intl.DateTimeFormat('en-US', { year: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(item.metricDate))}</NxTableCell>
                <NxTableCell>{item.componentCount.toLocaleString()}</NxTableCell>
                <NxTableCell>
                  <ChangeIcon value={item.percentageChangeComponent} /> {formatPercentage(item.percentageChangeComponent)}
                </NxTableCell>
                <NxTableCell>{item.requestCount.toLocaleString()}</NxTableCell>
                <NxTableCell>
                  <ChangeIcon value={item.percentageChangeRequest} /> {formatPercentage(item.percentageChangeRequest)}
                </NxTableCell>
              </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
    </>); 
}
