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
import { fireEvent, render, screen, within } from '@testing-library/react';
import NxCollapsibleNavigationLink from './NavigationLinkWithCollapsibleList';
import { faAirFreshener } from '@fortawesome/free-solid-svg-icons';

describe('NxCollapsibleNavigationLink', () => {
  let renderComponent;
  beforeAll(() => {
    const minimalProps = { text: 'base text', href: 'base href', icon: faAirFreshener };
    renderComponent = (props, children = 'this are some children items') =>
      render(
        <NxCollapsibleNavigationLink {...minimalProps} {...props}>
          {children}
        </NxCollapsibleNavigationLink>
      );
  });

  it('renders the link without the list', () => {
    renderComponent();
    const parentLink = screen.getByRole('link', { name: 'base text' });
    expect(parentLink).toBeVisible();
    expect(parentLink).toHaveAttribute('href', 'base href');
    const expandableList = screen.queryByRole('list');
    expect(expandableList).not.toBeInTheDocument();
  });

  it('opens the list on chevron click and collapses it on second click', () => {
    renderComponent();
    const dropdownButton = screen.getByRole('button');
    let expandableList = screen.queryByRole('list');
    expect(expandableList).not.toBeInTheDocument();
    fireEvent.click(dropdownButton);
    expandableList = screen.getByRole('list');
    expect(expandableList).toBeVisible();
    fireEvent.click(dropdownButton);
    expandableList = screen.queryByRole('list');
    expect(expandableList).not.toBeInTheDocument();
  });

  it('renders the link with the list expanded by default', () => {
    renderComponent({ isOpen: true });
    const expandableList = screen.queryByRole('list');
    expect(expandableList).toBeVisible();
  });

  it('renders the link without selected styles', () => {
    renderComponent();
    const parentLink = screen.getByRole('link', { name: 'base text' });
    expect(parentLink).not.toHaveClass('selected');
  });

  it('renders the link with selected styles', () => {
    renderComponent({ isSelected: true });
    const parentLink = screen.getByRole('link', { name: 'base text' });
    expect(parentLink).toHaveClass('selected');
  });
});
