import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { api } from '../api/client'
import { AuthContext } from '../auth/context'
import { response, studentAccount } from '../../test/renderApp'
import AppShell from './AppShell'

function openShell(path = '/home', unread = 0) {
  vi.spyOn(api, 'get').mockImplementation(async url => response(url === '/notifications/unread-count' ? { count: unread } : []))
  const auth = { account: studentAccount, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }
  const router = createMemoryRouter([{ element: <AppShell />, children: [{ path: '*', element: <p>Page body</p> }] }], { initialEntries: [path] })
  render(<AuthContext.Provider value={auth}><RouterProvider router={router} /></AuthContext.Provider>)
  return router
}

afterEach(() => vi.restoreAllMocks())

describe('application shell', () => {
  it('lists Achievements under You and keeps it highlighted on the forest it links to', () => {
    openShell('/forest')
    const you = within(screen.getByRole('group', { name: 'You' }))
    const achievements = you.getByRole('link', { name: 'Achievements' })
    expect(achievements).toHaveAttribute('href', '/achievements')
    expect(achievements).toHaveAttribute('aria-current', 'page')
    expect(achievements).toHaveClass('active')
  })

  it('reads the unread count as part of the notifications link', async () => {
    openShell('/home', 3)
    const link = await screen.findByRole('link', { name: 'Notifications, 3 unread' })
    expect(link).toHaveAttribute('href', '/notifications')
    expect(link.querySelector('[aria-label]')).toBeNull()
  })

  it('moves focus into the menu when it opens and returns it to the button on Escape', () => {
    openShell()
    const button = screen.getByRole('button', { name: 'Menu' })
    const navigation = screen.getByRole('navigation', { name: 'Main navigation' })
    fireEvent.click(button)
    expect(button).toHaveAttribute('aria-expanded', 'true')
    expect(navigation).toHaveClass('is-open')
    expect(within(navigation).getByRole('link', { name: 'Home' })).toHaveFocus()
    fireEvent.keyDown(document.activeElement as Element, { key: 'Escape' })
    expect(button).toHaveAttribute('aria-expanded', 'false')
    expect(navigation).not.toHaveClass('is-open')
    expect(button).toHaveFocus()
  })
})
