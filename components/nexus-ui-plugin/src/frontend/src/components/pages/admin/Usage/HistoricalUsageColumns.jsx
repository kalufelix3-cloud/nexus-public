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
import UIStrings from '../../../../constants/UIStrings';
import ChangeIcon from './ChangeIcon';
import HumanReadableUtils from '../../../../interface/HumanReadableUtils';

const formatPercentage = (value) => {
  if (value === 'N/A') return value;
  return `${Math.abs(Number(value))}%`;
};

export const historicalUsageColumns = {
  metricDateMonth: {
    key: 'metricDate',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.MONTH}</>,
    Cell: (item) => {
      const date = new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'short',
        timeZone: 'UTC'
      }).format(new Date(item.metricDate));
      return <>{date}</>;
    }
  },
  peakComponents: {
    key: 'peakComponents',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.PEAK_COMPONENTS}</>,
    Cell: (item) => <>{item.componentCount.toLocaleString()}</>
  },
  percentageChangeComponent: {
    key: 'percentageChangeComponent',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.COMPONENTS_CHANGE}</>,
    Cell: (item) => (
      <>
        <ChangeIcon value={item.percentageChangeComponent} /> {formatPercentage(item.percentageChangeComponent)}
      </>
    ),
    tooltip: UIStrings.HISTORICAL_USAGE.COMPONENTS_CHANGE_TOOLTIP
  },
  totalRequests: {
    key: 'totalRequests',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.TOTAL_REQUESTS}</>,
    Cell: (item) => <>{item.requestCount.toLocaleString()}</>
  },
  percentageChangeRequests: {
    key: 'percentageChangeRequests',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.REQUESTS_CHANGE}</>,
    Cell: (item) => (
      <>
        <ChangeIcon value={item.percentageChangeRequest} /> {formatPercentage(item.percentageChangeRequest)}
      </>
    ),
    tooltip: UIStrings.HISTORICAL_USAGE.REQUESTS_CHANGE_TOOLTIP
  },
  totalEgress: {
    key: 'responseSize',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.TOTAL_EGRESS}</>,
    Cell: (item) => <>{HumanReadableUtils.bytesToString(item.responseSize)}</>,
    tooltip: UIStrings.HISTORICAL_USAGE.TOTAL_EGRESS_TOOLTIP
  },
  percentageChangeEgress: {
    key: 'percentageChangeEgress',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.EGRESS_CHANGE}</>,
    Cell: (item) => (
      <>
        <ChangeIcon value={item.percentageChangeEgress} /> {formatPercentage(item.percentageChangeEgress)}
      </>
    ),
    tooltip: UIStrings.HISTORICAL_USAGE.EGRESS_CHANGE_TOOLTIP
  },
  peakStorage: {
    key: 'peakStorage',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.PEAK_STORAGE}</>,
    Cell: (item) => <>{HumanReadableUtils.bytesToString(item.peakStorage)}</>
  },
  percentageChangeStorage: {
    key: 'percentageChangeStorage',
    Header: () => <>{UIStrings.HISTORICAL_USAGE.STORAGE_CHANGE}</>,
    Cell: (item) => (
      <>
        <ChangeIcon value={item.percentageChangeStorage} /> {formatPercentage(item.percentageChangeStorage)}
      </>
    ),
    tooltip: UIStrings.HISTORICAL_USAGE.STORAGE_CHANGE_TOOLTIP
  }
};
