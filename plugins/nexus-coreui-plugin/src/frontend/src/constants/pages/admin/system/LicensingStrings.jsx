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
import {NxTextLink} from "@sonatype/react-shared-components";
import { scrollToUsageCenter } from '../../../../interfaces/LocationUtils'; 

export default {
  LICENSING: {
    MENU: {
      text: 'Licensing',
      description: 'A valid license is required for PRO features; manage it here'
    },
    SECTIONS: {
      DETAILS: 'Licensing',
      USAGE: 'Licensed Usage',
      INSTALL: 'Install License',
    },
    DETAILS: {
      COMPANY: {
        LABEL: 'Company',
      },
      NAME: {
        LABEL: 'Name',
      },
      EMAIL: {
        LABEL: 'Email',
      },
      EFFECTIVE_DATE: {
        LABEL: 'Effective Date',
      },
      LICENSE_TYPES: {
        LABEL: 'License Type(s)',
      },
      EXPIRATION_DATE: {
        LABEL: 'Expiration Date',
      },
      NUMBER_OF_USERS: {
        LABEL: 'Number of Licensed Users',
      },
      FINGERPRINT: {
        LABEL: 'Fingerprint',
      },
      REQUESTS_PER_MONTH: {
        LABEL: 'Requests Per Month',
      },
      TOTAL_COMPONENTS: {
        LABEL: 'Total Components',
      },
      USAGE_DESCRIPTION: {
        LABEL: () => (
          <>
            Your license is based on the total components stored and monthly requests.
            Track your current consumption on the <NxTextLink className="review-usage-link" onClick={scrollToUsageCenter}>Usage Center</NxTextLink>.
            <br />
            <NxTextLink external target='_blank' href="http://links.sonatype.com/products/nexus/pro/store?utm_medium=product&utm_source=nexus_repository&utm_campaign=repo_pricing_expansion" data-analytics-id="contact-us-link"> Contact us</NxTextLink> for additional capacity.
          </>
        )
      },
    },
    INSTALL: {
      LABEL: 'License',
      DESCRIPTION: 'Installing a new license requires restarting the server to take effect',
      MESSAGES: {
        ERROR: (error) => `Unable to update license for the reason identified below. Verify that you selected the correct file. If the problem persists, contact our support team. Reason: ${error}`,
        SUCCESS: 'License installed. Restart is only required if you are enabling new PRO features.',
      },
      BUTTONS: {
        UPLOAD: 'Upload License',
      }
    },
    AGREEMENT: {
      CAPTION: 'Nexus Repository Manager License Agreement',
      BUTTONS: {
        DECLINE: 'I Decline',
        ACCEPT: 'I Accept',
        DOWNLOAD: 'Download a copy of the agreement'
      }
    },
  }
};