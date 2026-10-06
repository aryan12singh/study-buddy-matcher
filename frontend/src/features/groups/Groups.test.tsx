import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { REFRESH_INTERVAL_MS } from '../../shared/api/useResource'
import { deferred, renderPage, response } from '../../test/renderApp'
import GroupFormPage from './GroupFormPage'
import GroupManagePage from './GroupManagePage'
import GroupDetailPage from './GroupDetailPage'
import GroupsPage from './GroupsPage'

const course = { id: 3, code: 'IS442', name: 'Object Oriented Programming' }
const group = { id: 5, name: 'OOP study crew', description: 'Weekly questions', courseId: 3, courseCode: 'IS442', courseName: 'Object Oriented Programming', leaderId: 1, leaderName: 'Priya', preferredStudyMode: 'IN_PERSON', studyGoals: ['PROBLEM_SOLVING'], maxGroupSize: 4, memberCount: 2, active: true, viewer: { leader: true, member: true, requestId: null, requestStatus: null }, createdAt: '2026-10-07T00:00:00Z', availability: [{ dayOfWeek: 'MONDAY', startTime: '18:00:00', endTime: '20:00:00' }], members: [{ studentId: 1, name: 'Priya', leader: true, joinedAt: '2026-10-07T00:00:00Z' }, { studentId: 2, name: 'Jamie', leader: false, joinedAt: '2026-10-07T01:00:00Z' }] }
const applicant = { id: 20, groupId: 5, groupName: 'OOP study crew', studentId: 3, studentName: 'Alex', message: 'May I join?', status: 'PENDING', createdAt: '2026-10-07T02:00:00Z', respondedAt: null, groupActive: true }
afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('group fields and saved state', () => {
  it('validates missing fields and reversed weekly time before making a request', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([course]))
    const post = vi.spyOn(api, 'post')
    renderPage(<GroupFormPage />, '/groups/new', '/groups/new')
    await screen.findByLabelText('Course')
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    fireEvent.change(screen.getByLabelText('End 1'), { target: { value: '08:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study group' }))
    expect(screen.getByText('Choose a course.')).toBeInTheDocument()
    expect(screen.getByText('Enter a name, up to 255 characters.')).toBeInTheDocument()
    expect(screen.getByText('Time block 1 must end after it starts.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })
  it('creates a group with every field and weekly slot', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([course]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, '/groups/new', '/groups/new')
    await screen.findByLabelText('Course')
    fireEvent.change(screen.getByLabelText('Group name'), { target: { value: '  OOP study crew  ' } })
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Maximum group size'), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText('Meeting mode (optional)'), { target: { value: 'ONLINE' } })
    fireEvent.change(screen.getByLabelText('Description (optional)'), { target: { value: 'Weekly questions' } })
    fireEvent.click(screen.getByLabelText('Problem solving'))
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    fireEvent.change(screen.getByLabelText('Day 1'), { target: { value: 'TUESDAY' } })
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '18:00' } })
    fireEvent.change(screen.getByLabelText('End 1'), { target: { value: '20:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study group' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/groups', { name: 'OOP study crew', description: 'Weekly questions', courseId: 3, maxGroupSize: 5, preferredStudyMode: 'ONLINE', studyGoals: ['PROBLEM_SOLVING'], availability: [{ dayOfWeek: 'TUESDAY', startTime: '18:00', endTime: '20:00' }] }))
    expect(await screen.findByText('Destination page')).toBeInTheDocument()
  })
  it('validates input-only time edits and saves their corrected values', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([course]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, '/groups/new', '/groups/new')
    await screen.findByLabelText('Course')
    fireEvent.change(screen.getByLabelText('Group name'), { target: { value: 'Time input regression' } })
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    fireEvent.input(screen.getByLabelText('End 1'), { target: { value: '08:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study group' }))
    expect(screen.getByText('Time block 1 must end after it starts.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.input(screen.getByLabelText('Start 1'), { target: { value: '08:00' } })
    fireEvent.input(screen.getByLabelText('End 1'), { target: { value: '11:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study group' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/groups', expect.objectContaining({ availability: [{ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '11:00' }] })))
  })
  it('prefills saved values and rejects capacity below accepted membership', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : { ...group, memberCount: 3 }))
    const put = vi.spyOn(api, 'put')
    renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    expect(await screen.findByDisplayValue('OOP study crew')).toBeInTheDocument()
    expect(screen.getByLabelText('Start 1')).toHaveValue('18:00')
    expect(screen.getByLabelText('Problem solving')).toBeChecked()
    fireEvent.change(screen.getByLabelText('Maximum group size'), { target: { value: '2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save group changes' }))
    expect(screen.getByText(/Capacity must be a whole number of at least 3/)).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })
  it('shows the saved agenda and never exposes contact through member rows', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(group))
    renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'View agenda' }))
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('Problem solving')
    expect(dialog).toHaveTextContent('18:00–20:00')
    expect(dialog).toHaveTextContent('Asia/Singapore')
    expect(screen.getByText(/Being in the same group does not share contact numbers/)).toBeInTheDocument()
  })
  it('renders full, pending and closed states without a request action', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...group, active: false, viewer: { leader: false, member: false, requestStatus: 'PENDING', requestId: 20 } }))
    renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    expect(await screen.findByText('Closed group', { selector: '.status-badge' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Request membership' })).not.toBeInTheDocument()
    expect(screen.queryByText('Manage group')).not.toBeInTheDocument()
  })
  it.each(['focus', 'timer'] as const)('keeps the application draft during a pending %s refresh and displays current server decisions', async refresh => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
    const pending = deferred<never>()
    vi.spyOn(api, 'get')
      .mockResolvedValueOnce(response({ ...group, viewer: { leader: false, member: false, requestId: null, requestStatus: null } }))
      .mockImplementationOnce(() => pending.promise)
    const post = vi.spyOn(api, 'post').mockRejectedValue({
      isAxiosError: true,
      response: { status: 409, data: { message: 'This group is closed.' } }
    })
    renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Request membership' }))
    const dialog = screen.getByRole('dialog', { name: 'Request to join OOP study crew' })
    fireEvent.change(within(dialog).getByLabelText('Message (optional)'), { target: { value: 'I can meet on Monday evenings.' } })

    await act(async () => {
      if (refresh === 'focus') window.dispatchEvent(new Event('focus'))
      else vi.advanceTimersByTime(REFRESH_INTERVAL_MS)
    })
    expect(screen.getByRole('status')).toHaveTextContent('Loading')
    expect(screen.getByRole('dialog')).toBe(dialog)
    expect(within(dialog).getByLabelText('Message (optional)')).toHaveValue('I can meet on Monday evenings.')
    await act(async () => {
      pending.resolve(response({ ...group, active: false }))
      await pending.promise
    })
    expect(screen.getByText('Closed group', { selector: '.status-badge' })).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBe(dialog)
    fireEvent.click(within(dialog).getByRole('button', { name: 'Request membership' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('This group is closed.')
    expect(post).toHaveBeenCalledWith('/groups/5/join-requests', { message: 'I can meet on Monday evenings.' })
    expect(within(dialog).getByLabelText('Message (optional)')).toHaveValue('I can meet on Monday evenings.')
  })
  it('uses the mine and own-application APIs with real empty/history states', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : path === '/group-join-requests/mine' ? [{ ...applicant, status: 'REJECTED', groupActive: false }] : []))
    renderPage(<GroupsPage />, '/groups', '/groups')
    expect(await screen.findByText('No groups match these filters')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Your groups' }))
    expect(await screen.findByText('No groups joined yet')).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/groups/mine', expect.any(Object))
    fireEvent.click(screen.getByRole('button', { name: 'Your applications' }))
    expect(await screen.findByText('Rejected', { selector: '.status-badge' })).toBeInTheDocument()
    expect(screen.getByText('Closed group', { selector: '.status-badge' })).toBeInTheDocument()
  })
})

describe('group leader permissions and decisions', () => {
  it('does not fetch applicants or render controls for a non-leader', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ ...group, viewer: { ...group.viewer, leader: false } }))
    renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    expect(await screen.findByRole('alert')).toHaveTextContent('Only this group’s leader')
    expect(get).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Approve application' })).not.toBeInTheDocument()
  })
  it('disables approval when full but allows rejection and shows API failures', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path.endsWith('/join-requests') ? [applicant] : { ...group, maxGroupSize: 2 }))
    const post = vi.spyOn(api, 'post').mockRejectedValue(new Error('Application no longer pending'))
    renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    expect(await screen.findByRole('button', { name: 'Approve application' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Reject application' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Application no longer pending')
    expect(post).toHaveBeenCalledWith('/groups/5/join-requests/20/reject')
    expect(screen.getByRole('button', { name: 'Reject application' })).toBeEnabled()
  })
  it('protects the leader and confirms removal of another member', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path.endsWith('/join-requests') ? [] : group))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    await screen.findByText('No pending applicants')
    fireEvent.click(screen.getByRole('button', { name: 'Members' }))
    const buttons = screen.getAllByRole('button', { name: 'Remove member' })
    expect(buttons).toHaveLength(1)
    fireEvent.click(buttons[0])
    expect(remove).not.toHaveBeenCalled()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Remove member' }))
    await waitFor(() => expect(remove).toHaveBeenCalledWith('/groups/5/members/2'))
  })
  it('confirms one-way closure before calling the close API', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path.endsWith('/join-requests') ? [] : group))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...group, active: false }))
    renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    fireEvent.click(await screen.findByRole('button', { name: 'Close group' }))
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('Closing is permanent')
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Close group' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/groups/5/close'))
  })
})
