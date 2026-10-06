import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { adminAccount, renderPage, response } from '../../test/renderApp'
import AdminUsersPage from './AdminUsersPage'
import AdminUserDetailPage from './AdminUserDetailPage'
import AdminUserFormPage from './AdminUserFormPage'

const account = { id: 2, email: 'jamie@example.test', role: 'STUDENT', name: 'Jamie', active: true, createdAt: '2026-10-01T00:00:00Z', lastLoginAt: null }
const detail = { account, profile: { name: 'Jamie', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2 }, usage: { activeConnections: 0, acceptedGroups: 0, matchRequestsSent: 3, groupsLed: 1, groupsJoined: 0 } }
afterEach(() => vi.restoreAllMocks())

describe('administrator account forms and privacy', () => {
  it('prefills public fields, fixes role, and omits blank contact replacement from update', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(detail))
    renderPage(<AdminUserFormPage />, '/admin/users/2/edit', '/admin/users/:id/edit', adminAccount)
    expect(await screen.findByDisplayValue('Jamie')).toBeInTheDocument()
    expect(screen.getByLabelText('Replace contact number (optional)')).toHaveValue('')
    expect(screen.queryByRole('combobox', { name: 'Account role' })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: ' JAMIE.NEW@example.test ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save account changes' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/admin/users/2', { email: 'jamie.new@example.test', name: 'Jamie', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2 }))
  })
  it('supports an optional write-only contact replacement', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(detail))
    renderPage(<AdminUserFormPage />, '/admin/users/2/edit', '/admin/users/:id/edit', adminAccount)
    await screen.findByDisplayValue('Jamie')
    fireEvent.change(screen.getByLabelText('Replace contact number (optional)'), { target: { value: '+65 9999 1111' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save account changes' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/admin/users/2', expect.objectContaining({ contactNumber: '+65 9999 1111' })))
  })
  it('validates student creation and sends only account fields for an administrator', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...detail, account: { ...account, id: 100, role: 'ADMIN', name: null }, profile: null, usage: null }))
    renderPage(<AdminUserFormPage />, '/admin/users/new', '/admin/users/new', adminAccount)
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(screen.getByText('Enter a contact number, up to 255 characters.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Account role'), { target: { value: 'ADMIN' } })
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'newadmin@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'a-long-passphrase' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/users', { email: 'newadmin@example.test', role: 'ADMIN', password: 'a-long-passphrase' }))
  })
  it('shows actual usage and never presents a stored contact field', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    renderPage(<AdminUserDetailPage />, '/admin/users/2', '/admin/users/:id', adminAccount)
    expect(await screen.findByText('Never logged in')).toBeInTheDocument()
    expect(screen.getByText('Accepted groups')).toBeInTheDocument()
    expect(screen.getByText('Buddy requests sent')).toBeInTheDocument()
    expect(screen.queryByText('Contact number')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue('+65 9999 1111')).not.toBeInTheDocument()
  })
})

describe('administrator lifecycle actions', () => {
  it('confirms deactivation with its dependency consequences and uses POST', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response(detail)).mockResolvedValue(response({ ...detail, account: { ...account, active: false } }))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...detail, account: { ...account, active: false } }))
    const remove = vi.spyOn(api, 'delete')
    renderPage(<AdminUserDetailPage />, '/admin/users/2', '/admin/users/:id', adminAccount)
    fireEvent.click(await screen.findByRole('button', { name: 'Deactivate account' }))
    expect(post).not.toHaveBeenCalled()
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('led groups will close')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Deactivate account' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/users/2/deactivate'))
    expect(remove).not.toHaveBeenCalled()
    expect(await screen.findByRole('button', { name: 'Reactivate account' })).toBeInTheDocument()
  })
  it('makes permanent deletion a distinct confirmation requiring the email', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    renderPage(<AdminUserDetailPage />, '/admin/users/2', '/admin/users/:id', adminAccount)
    fireEvent.click(await screen.findByRole('button', { name: 'Delete account permanently' }))
    const dialog = screen.getByRole('dialog'), confirm = within(dialog).getByRole('button', { name: 'Delete permanently' })
    expect(confirm).toBeDisabled()
    expect(dialog).toHaveTextContent("Every group they lead and those groups' dependent records")
    expect(remove).not.toHaveBeenCalled()
    fireEvent.change(within(dialog).getByLabelText('Type jamie@example.test to confirm'), { target: { value: 'jamie@example.test' } })
    fireEvent.click(confirm)
    await waitFor(() => expect(remove).toHaveBeenCalledWith('/admin/users/2'))
    expect(await screen.findByText('Destination page')).toBeInTheDocument()
  })
  it('disables removal of the currently signed-in account', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ account: { ...account, ...adminAccount, active: true }, profile: null, usage: null }))
    renderPage(<AdminUserDetailPage />, '/admin/users/99', '/admin/users/:id', adminAccount)
    expect(await screen.findByRole('button', { name: 'Deactivate account' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Delete account permanently' })).toBeDisabled()
  })
  it('renders truthful summary data and sends the selected role/status filters', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => {
      if (path.endsWith('/summary')) return response({ total: 12, active: 10, inactive: 2, students: 10, admins: 2 })
      return response([account])
    })
    renderPage(<AdminUsersPage />, '/admin/users', '/admin/users', adminAccount)
    expect(await screen.findByText('Total accounts')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    await screen.findByRole('table')
    fireEvent.change(screen.getByLabelText('Role'), { target: { value: 'STUDENT' } })
    fireEvent.change(screen.getByLabelText('Account status'), { target: { value: 'false' } })
    await waitFor(() => expect(get).toHaveBeenCalledWith('/admin/users', expect.objectContaining({ params: { role: 'STUDENT', active: 'false' } })))
  })
  it('renders a server action conflict without leaving the form pending', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...detail, account: { ...account, active: false } }))
    vi.spyOn(api, 'post').mockRejectedValue(new Error('Account is already active'))
    renderPage(<AdminUserDetailPage />, '/admin/users/2', '/admin/users/:id', adminAccount)
    fireEvent.click(await screen.findByRole('button', { name: 'Reactivate account' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Account is already active')
    expect(screen.getByRole('button', { name: 'Reactivate account' })).toBeEnabled()
  })
})
