/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, { useRef } from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useExtJsUnsavedChangesGuard } from './useExtJsUnsavedChangesGuard';

// Mock ExtJS and related globals
const warnBeforeNavigateMock = jest.fn();
const hasDirtMock = jest.fn();
const resetUnsavedChangesFlagMock = jest.fn();
const getFeatureContentMock = jest.fn(() => ({
  down: jest.fn(() => ({ getForm: jest.fn(() => ({ reset: jest.fn() })) })),
  getForm: jest.fn(() => ({ reset: jest.fn() })),
  resetUnsavedChangesFlag: resetUnsavedChangesFlagMock,
}));

global.Ext = {
  getApplication: () => ({
    getController: () => ({
      hasDirt: hasDirtMock,
      warnBeforeNavigate: warnBeforeNavigateMock,
      getFeatureContent: getFeatureContentMock,
    }),
  }),
};

describe('useExtJsUnsavedChangesGuard', () => {
  function TestComponent() {
    const ref = useRef({ contains: () => false });
    useExtJsUnsavedChangesGuard(ref);
    return <div ref={ref}>Test</div>;
  }

  beforeEach(() => {
    jest.clearAllMocks();
    hasDirtMock.mockReturnValue(false);
    warnBeforeNavigateMock.mockImplementation(cb => {
      cb && cb();
      return false;
    });
  });

  it('should not prevent navigation if there is no unsaved dirt', () => {
    render(<TestComponent />);
    const a = document.createElement('a');
    a.setAttribute('href', '#test');
    document.body.appendChild(a);
    userEvent.click(a);
    expect(warnBeforeNavigateMock).not.toHaveBeenCalled();
    document.body.removeChild(a);
  });

  it('should call warnBeforeNavigate if there is unsaved dirt on link click', () => {
    hasDirtMock.mockReturnValue(true);
    render(<TestComponent />);
    const a = document.createElement('a');
    a.setAttribute('href', '#test');
    document.body.appendChild(a);
    userEvent.click(a);
    expect(warnBeforeNavigateMock).toHaveBeenCalled();
    document.body.removeChild(a);
  });

  it('should handle popstate event with unsaved dirt', () => {
    hasDirtMock.mockReturnValue(true);
    render(<TestComponent />);
    const event = new PopStateEvent('popstate');
    window.dispatchEvent(event);
    expect(warnBeforeNavigateMock).toHaveBeenCalled();
  });
});
