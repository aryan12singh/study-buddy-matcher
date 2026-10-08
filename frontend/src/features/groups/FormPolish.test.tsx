import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { appRoutes } from '../../routes'
import { api } from '../../shared/api/client'
import { AuthContext } from '../../shared/auth/context'
import { adminAccount, response, studentAccount } from '../../test/renderApp'
import type { Account } from '../../shared/auth/context'

const course = { id: 3, code: 'IS442', name: 'Object Oriented Programming' }
const group = { id: 5, name: 'OOP crew', description: '', courseId: 3, courseCode: 'IS442', courseName: 'OOP', leaderId: 1, leaderName: 'Priya',
  preferredStudyMode: null, studyGoals: [], availability: [], maxGroupSize: 4, memberCount: 1, active: true,
  viewer: { leader: true, member: true, requestId: null, requestStatus: null }, createdAt: '2026-10-01T00:00:00Z', members: [] }
const detail = { account: { id: 100, email: 'other-admin@example.test', role: 'ADMIN', name: null, active: true,
  createdAt: '2026-10-01T00:00:00Z', lastLoginAt: null }, profile: null, usage: null }

function renderApplication(path: string, account: Account = studentAccount, entries = [path]) {
  const router = createMemoryRouter(appRoutes, { initialEntries: entries })
  const auth = { account, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }
  const view = render(<AuthContext.Provider value={auth}><RouterProvider router={router} /></AuthContext.Provider>)
  return { ...view, router, auth }
}
function reads() {
  return vi.spyOn(api, 'get').mockImplementation(async path => response(
    path === '/courses' ? [course] : path === '/groups/5' ? group : path === '/admin/users/100' ? detail :
      path === '/admin/users/summary' ? { total: 1, active: 1, inactive: 0, students: 0, admins: 1 } :
        path === '/notifications/unread-count' ? { count: 0 } : []
  ))
}
afterEach(() => vi.restoreAllMocks())

describe('form protection and destination feedback', () => {
  it('restores scrolling when browser Back discards a form with its schedule-copy dialog open', async () => {
    reads()
    const { router } = renderApplication('/groups/5/edit', studentAccount, ['/groups/5', '/groups/5/edit'])
    // Wait for the saved group to fill the form, or loading it would replace the added block
    expect(await screen.findByDisplayValue('OOP crew')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Copy time block 1 to other days' }))
    expect(document.body.style.overflow).toBe('hidden')
    await act(async () => { await router.navigate(-1) })
    fireEvent.click(await screen.findByRole('button', { name: 'Discard changes' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/5'))
    // The router updates its location before React commits the new page, so wait for the detail page itself
    expect(await screen.findByRole('heading', { level: 1, name: 'OOP crew' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
  })
  it('lets an expired session remove an unsaved form and its reload warning', async () => {
    reads()
    const { router, auth, rerender } = renderApplication('/groups/new')
    fireEvent.change(await screen.findByLabelText('Group name'), { target: { value: 'Private draft' } })
    rerender(<AuthContext.Provider value={{ ...auth, account: null }}><RouterProvider router={router} /></AuthContext.Provider>)
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByLabelText('Group name')).not.toBeInTheDocument()
    const event = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(false)
  })
  it('protects dirty Cancel, keeps the draft on request and discards only after confirmation', async () => {
    reads()
    const post = vi.spyOn(api, 'post')
    const { router } = renderApplication('/groups/new')
    fireEvent.change(await screen.findByLabelText('Group name'), { target: { value: 'Draft crew' } })
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    expect(screen.getByRole('dialog')).toHaveTextContent('Your edits have not been saved.')
    expect(router.state.location.pathname).toBe('/groups/new')
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(screen.getByLabelText('Group name')).toHaveValue('Draft crew')
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups'))
    expect(post).not.toHaveBeenCalled()
  })
  it('protects browser Back and sidebar links, then removes the native reload warning on unmount', async () => {
    reads()
    const { router, unmount } = renderApplication('/groups/new', studentAccount, ['/groups', '/groups/new'])
    fireEvent.change(await screen.findByLabelText('Group name'), { target: { value: 'Draft crew' } })
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(true)
    await act(async () => { await router.navigate(-1) })
    fireEvent.click(await screen.findByRole('button', { name: 'Keep editing' }))
    expect(router.state.location.pathname).toBe('/groups/new')
    fireEvent.click(screen.getByRole('link', { name: 'Connections' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Discard changes' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/connections'))
    unmount()
    const cleanReload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(cleanReload)
    expect(cleanReload.defaultPrevented).toBe(false)
  })
  it('does not block a clean form or a reverted edit', async () => {
    reads()
    const { router } = renderApplication('/groups/5/edit')
    const input = await screen.findByDisplayValue(group.name)
    fireEvent.change(input, { target: { value: 'Different name' } })
    fireEvent.change(input, { target: { value: group.name } })
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/5'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
  it('shows group creation feedback on its destination without blocking a successful save', async () => {
    reads()
    vi.spyOn(api, 'post').mockResolvedValue(response(group))
    const { router } = renderApplication('/groups/new')
    fireEvent.change(await screen.findByLabelText('Group name'), { target: { value: group.name } })
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study group' }))
    expect(await screen.findByText('Group created.')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.state).not.toHaveProperty('notice'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss success message' }))
    expect(screen.queryByText('Group created.')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: 'Back to groups' }))
    await screen.findByRole('heading', { name: 'Study groups', level: 1 })
    await act(async () => { await router.navigate(-1) })
    expect(screen.queryByText('Group created.')).not.toBeInTheDocument()
  })
  it('announces a successful group update on the destination', async () => {
    reads()
    vi.spyOn(api, 'put').mockResolvedValue(response(group))
    renderApplication('/groups/5/edit')
    fireEvent.change(await screen.findByDisplayValue(group.name), { target: { value: 'Updated crew' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save group changes' }))
    expect(await screen.findByText('Group updated.')).toBeInTheDocument()
  })
  it('protects unsaved admin credentials, then announces creation using generic history state', async () => {
    reads()
    vi.spyOn(api, 'post').mockResolvedValue(response(detail))
    const { router } = renderApplication('/admin/users/new', adminAccount)
    fireEvent.change(screen.getByLabelText('Account role'), { target: { value: 'ADMIN' } })
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: detail.account.email } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'a-test-only-passphrase' } })
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(screen.getByLabelText('Password')).toHaveValue('a-test-only-passphrase')
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('Account created.')).toBeInTheDocument()
    await waitFor(() => expect(Object.keys(router.state.location.state)).toEqual(['noticeConsumed']))
    expect(screen.queryByDisplayValue('a-test-only-passphrase')).not.toBeInTheDocument()
  })
  it('announces an account update and permanent deletion on their destinations', async () => {
    reads()
    vi.spyOn(api, 'put').mockResolvedValue(response(detail))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    const { router } = renderApplication('/admin/users/100/edit', adminAccount)
    fireEvent.change(await screen.findByDisplayValue(detail.account.email), { target: { value: 'edited@example.test' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save account changes' }))
    expect(await screen.findByText('Account updated.')).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: 'Delete account permanently' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText(`Type ${detail.account.email} to confirm`), { target: { value: detail.account.email } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete permanently' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/users'))
    await screen.findByRole('heading', { name: 'User accounts', level: 1 })
    expect(await screen.findByText('Account permanently deleted.')).toBeInTheDocument()
    expect(remove).toHaveBeenCalledWith('/admin/users/100')
  })
})
