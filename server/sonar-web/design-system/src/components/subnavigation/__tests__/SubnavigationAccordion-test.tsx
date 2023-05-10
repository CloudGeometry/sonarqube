/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { SubnavigationAccordion } from '../SubnavigationAccordion';

it('should have correct style and html structure', () => {
  setupWithProps();

  expect(screen.getByRole('button', { expanded: false })).toBeVisible();
  expect(screen.queryByText('Foo')).not.toBeInTheDocument();
});

it('should display expanded', () => {
  setupWithProps({ initExpanded: true });

  expect(screen.getByRole('button', { expanded: true })).toBeVisible();
  expect(screen.getByText('Foo')).toBeVisible();
});

it('should toggle expand', async () => {
  const user = userEvent.setup();
  setupWithProps();

  expect(screen.queryByText('Foo')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button'));
  expect(screen.getByText('Foo')).toBeVisible();
  await user.click(screen.getByRole('button'));
  expect(screen.queryByText('Foo')).not.toBeInTheDocument();
});

function setupWithProps(props: Partial<FCProps<typeof SubnavigationAccordion>> = {}) {
  return render(
    <SubnavigationAccordion header="Header" id="test" initExpanded={false} {...props}>
      <span>Foo</span>
    </SubnavigationAccordion>
  );
}