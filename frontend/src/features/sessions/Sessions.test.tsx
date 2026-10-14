import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import SessionsPage from './SessionsPage'
import SessionFormPage from './SessionFormPage'
import SessionDetailPage from './SessionDetailPage'
import UpcomingSessions from './UpcomingSessions'
import GroupSessionAgenda from './GroupSessionAgenda'
import type { StudySession } from './api'
import { notificationLink } from '../notifications/api'
import { nextSuggestedTimes, validateSessionForm, initialSessionForm } from './sessionForm'

const session: StudySession = {
  id: 11, version: 3, scope: 'GROUP', groupId: 5, connectionId: null, title: 'Exam revision',
  startLocal: '2026-10-20T10:00', endLocal: '2026-10-20T11:00', startAt: '2026-10-20T02:00:00Z', endAt: '2026-10-20T03:00:00Z',
  timeZone: 'Asia/Singapore', occurrenceCount: 1, agenda: 'Work through the practice paper', location: 'Library', roomLink: '/groups/5/room',
  status: 'SCHEDULED', canManage: true, occurrences: [{ startAt: '2026-10-20T02:00:00Z', endAt: '2026-10-20T03:00:00Z' }],
  nextOccurrence: { startAt: '2026-10-20T02:00:00Z', endAt: '2026-10-20T03:00:00Z' }, updatedAt: '2026-10-14T00:00:00Z', cancelledAt: null,
}
const group = { id: 5, name: 'Revision crew', active: true, viewer: { leader: true, member: true } }
const availability = { timeZone: 'Asia/Singapore', participantCount: 2, sharedMinutes: 60, sharedSlots: [{ dayOfWeek: 'MONDAY', startTime: '10:00', endTime: '11:00' }] }
function failure(status: number, message: string, fields?: Record<string, string>) {
  return new AxiosError(message, 'ERR_BAD_RESPONSE', undefined, undefined, { config: { headers: new AxiosHeaders() }, status, statusText: 'Error', headers: {}, data: { message, fieldErrors: fields } })
}
function serve(overrides: Record<string, unknown> = {}) {
  const data: Record<string, unknown> = { '/groups/5': group, '/groups/6': { ...group, id: 6, name: 'New crew' },
    '/sessions/11': session, '/sessions/12': { ...session, id: 12, title: 'New session' }, '/sessions/mine': [session],
    '/groups/5/sessions': [session], '/groups/5/sessions/availability': availability, '/groups/6/sessions/availability': availability,
    '/connections': [{ id: 8, otherStudentId: 2, otherStudentName: 'Jamie', createdAt: '2026-10-14T00:00:00Z' }],
    '/connections/8/sessions': [{ ...session, scope: 'BUDDY', groupId: null, connectionId: 8 }], '/connections/8/sessions/availability': { ...availability, sharedSlots: [] }, ...overrides }
  return vi.spyOn(api, 'get').mockImplementation(async path => response(data[path as string]))
}
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals() })

describe('dated study session workflows', () => {
  it('previews the next three group dates instead of prioritizing older recurring series', async () => {
    const recurring = [1, 2, 3].map((id, index) => ({
      ...session, id, title: `Older weekly series ${id}`, occurrenceCount: 52,
      startAt: `2026-0${id}-01T02:00:00Z`, endAt: `2026-0${id}-01T03:00:00Z`,
      nextOccurrence: { startAt: `2026-10-${15 + index}T02:00:00Z`, endAt: `2026-10-${15 + index}T03:00:00Z` }
    }))
    const imminent = { ...session, id: 4, title: 'Imminent one-off', startAt: '2026-10-14T02:00:00Z', endAt: '2026-10-14T03:00:00Z',
      nextOccurrence: { startAt: '2026-10-14T02:00:00Z', endAt: '2026-10-14T03:00:00Z' } }
    // The scope endpoint orders original starts; the agenda must order each next occurrence.
    serve({ '/groups/5/sessions': [...recurring, imminent] })
    renderPage(<GroupSessionAgenda groupId={5} leader={false} />)
    const list = await screen.findByRole('list', { name: 'Study sessions' })
    const links = within(list).getAllByRole('heading', { level: 3 }).map(heading => within(heading).getByRole('link').getAttribute('href'))
    expect(links).toEqual(['/sessions/4', '/sessions/1', '/sessions/2'])
  })

  it('shows real upcoming dates on Home and links session notifications to their detail', async () => {
    const get = serve()
    renderPage(<UpcomingSessions />)
    expect(await screen.findByRole('link', { name: 'Exam revision' })).toHaveAttribute('href', '/sessions/11')
    expect(screen.getByText(/20 Oct 2026/)).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/sessions/mine', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(notificationLink({ id: 1, type: 'GROUP_SESSION_UPDATED', resourceType: 'SESSION', resourceId: 11, read: false, createdAt: '', message: '', eventKey: null })).toEqual({ to: '/sessions/11', label: 'View study session' })
  })

  it('retains separate list permissions and explains an empty agenda', async () => {
    serve({ '/groups/5': { ...group, viewer: { leader: false, member: true } }, '/groups/5/sessions': [] })
    renderPage(<SessionsPage kind="GROUP" />, '/groups/5/sessions', '/groups/:id/sessions')
    expect(await screen.findByText('No study sessions yet')).toBeInTheDocument()
    expect(screen.getByText('The group leader plans and manages sessions.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Plan study session' })).not.toBeInTheDocument()
  })

  it('offers either active buddy participant a scheduling entry', async () => {
    serve()
    renderPage(<SessionsPage kind="BUDDY" />, '/connections/8/sessions', '/connections/:id/sessions')
    expect(await screen.findByRole('link', { name: 'Plan study session' })).toHaveAttribute('href', '/connections/8/sessions/new')
  })

  it('validates before writes and applies a shared-time suggestion in Singapore time', async () => {
    serve()
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(session))
    const { router } = renderPage(<SessionFormPage kind="GROUP" />, '/groups/5/sessions/new', '/groups/:id/sessions/new')
    fireEvent.click(await screen.findByRole('button', { name: 'Create study session' }))
    expect(screen.getByText('Enter a title of 1–255 characters.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Session title'), { target: { value: '  Exam revision  ' } })
    fireEvent.change(screen.getByLabelText('Session timezone'), { target: { value: 'Europe/London' } })
    fireEvent.click(await screen.findByRole('button', { name: /Use next Monday/ }))
    expect(screen.getByLabelText('Session timezone')).toHaveValue('Asia/Singapore')
    fireEvent.change(screen.getByLabelText('Number of weekly dates'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study session' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/sessions/11'))
    expect(post).toHaveBeenCalledWith('/groups/5/sessions', expect.objectContaining({ title: 'Exam revision', occurrenceCount: 3, timeZone: 'Asia/Singapore', roomLink: '/groups/5/room' }))
  })

  it('keeps a draft after a version conflict and never silently saves against a newer revision', async () => {
    const get = serve()
    const put = vi.spyOn(api, 'put').mockRejectedValue(failure(409, 'The session changed; reopen the latest version'))
    renderPage(<SessionFormPage />, '/sessions/11/edit', '/sessions/:id/edit')
    fireEvent.change(await screen.findByLabelText('Session title'), { target: { value: 'My careful edits' } })
    get.mockImplementation(async path => response(path === '/sessions/11' ? { ...session, version: 4 } : availability))
    fireEvent(window, new Event('focus'))
    fireEvent.click(screen.getByRole('button', { name: 'Save session changes' }))
    expect(await screen.findByText('The session changed; reopen the latest version')).toBeInTheDocument()
    expect(screen.getByLabelText('Session title')).toHaveValue('My careful edits')
    expect(put).toHaveBeenCalledWith('/sessions/11', expect.objectContaining({ title: 'My careful edits', expectedVersion: 3 }))
  })

  it('displays server daylight-saving errors without discarding entered times', async () => {
    serve()
    vi.spyOn(api, 'put').mockRejectedValue(failure(400, 'Invalid session times', { startLocal: 'This local time does not exist in the selected timezone' }))
    renderPage(<SessionFormPage />, '/sessions/11/edit', '/sessions/:id/edit')
    fireEvent.change(await screen.findByLabelText('Session timezone'), { target: { value: 'America/New_York' } })
    fireEvent.change(screen.getByLabelText('Starts (session timezone)'), { target: { value: '2027-03-14T02:30' } })
    fireEvent.change(screen.getByLabelText('Ends (session timezone)'), { target: { value: '2027-03-14T04:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save session changes' }))
    expect(await screen.findByText('This local time does not exist in the selected timezone')).toBeInTheDocument()
    expect(screen.getByLabelText('Starts (session timezone)')).toHaveValue('2027-03-14T02:30')
  })

  it('blocks unsaved navigation and discards private form data when write permission is revoked', async () => {
    serve()
    vi.spyOn(api, 'put').mockRejectedValue(failure(403, 'Membership removed'))
    renderPage(<SessionFormPage />, '/sessions/11/edit', '/sessions/:id/edit')
    fireEvent.change(await screen.findByLabelText('Session title'), { target: { value: 'Private draft' } })
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    expect(await screen.findByRole('dialog', { name: 'Discard unsaved changes?' })).toBeInTheDocument()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Keep editing' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save session changes' }))
    expect(await screen.findByText('Membership removed')).toBeInTheDocument()
    expect(screen.queryByLabelText('Session title')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue('Private draft')).not.toBeInTheDocument()
  })

  it('prevents a late save from navigating over a newer group form', async () => {
    serve()
    const write = deferred<never>()
    vi.spyOn(api, 'post').mockReturnValue(write.promise)
    const { router } = renderPage(<SessionFormPage kind="GROUP" />, '/groups/5/sessions/new', '/groups/:id/sessions/new')
    fireEvent.change(await screen.findByLabelText('Session title'), { target: { value: 'Old group draft' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study session' }))
    await router.navigate('/groups/6/sessions/new')
    fireEvent.click(await screen.findByRole('button', { name: 'Leave page' }))
    expect(await screen.findByText('For New crew')).toBeInTheDocument()
    await act(async () => { write.resolve(response(session)) })
    expect(router.state.location.pathname).toBe('/groups/6/sessions/new')
    expect(screen.getByLabelText('Session title')).toHaveValue('')
  })

  it('confirms whole-plan cancellation and exports the cancelled snapshot', async () => {
    const get = serve()
    const post = vi.spyOn(api, 'post').mockImplementation(async () => {
      get.mockImplementation(async () => response({ ...session, status: 'CANCELLED', version: 4, canManage: false }))
      return response({ ...session, status: 'CANCELLED', version: 4 })
    })
    renderPage(<SessionDetailPage />, '/sessions/11', '/sessions/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Cancel session' }))
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel for everyone' }))
    expect(await screen.findByText('This plan is cancelled. Download the cancellation snapshot to update your calendar.')).toBeInTheDocument()
    expect(post).toHaveBeenCalledWith('/sessions/11/cancel', { expectedVersion: 3 })
    expect(screen.getByRole('button', { name: 'Download calendar (.ics)' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Edit session' })).not.toBeInTheDocument()
  })

  it('downloads an authenticated calendar snapshot and releases its temporary URL', async () => {
    const create = vi.fn(() => 'blob:session-calendar'), revoke = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { value: create, configurable: true })
    Object.defineProperty(URL, 'revokeObjectURL', { value: revoke, configurable: true })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const get = serve()
    get.mockImplementation(async path => response(path.endsWith('/calendar') ? new Blob(['BEGIN:VCALENDAR']) : session))
    renderPage(<SessionDetailPage />, '/sessions/11', '/sessions/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Download calendar (.ics)' }))
    await waitFor(() => expect(revoke).toHaveBeenCalledWith('blob:session-calendar'))
    expect(get).toHaveBeenCalledWith('/sessions/11/calendar', expect.objectContaining({ responseType: 'blob', headers: { Accept: 'text/calendar' } }))
    expect(create).toHaveBeenCalledOnce()
  })

  it('does not download a late private response after changing session views', async () => {
    const create = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { value: create, configurable: true })
    const delayed = deferred<never>()
    const get = serve()
    get.mockImplementation(path => path.endsWith('/calendar') ? delayed.promise : Promise.resolve(response(path === '/sessions/11' ? session : { ...session, id: 12, title: 'New session' })))
    const { router } = renderPage(<SessionDetailPage />, '/sessions/11', '/sessions/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Download calendar (.ics)' }))
    await router.navigate('/sessions/12')
    await screen.findByRole('heading', { name: 'New session' })
    await act(async () => { delayed.resolve(response(new Blob(['private snapshot']))) })
    expect(create).not.toHaveBeenCalled()
  })

  it('checks dates, bounded repeats, safe links and next weekly suggestions without timezone coercion', () => {
    const form = initialSessionForm(session, 5)
    expect(validateSessionForm({ ...form, endLocal: '2026-10-21T01:00' }, 5)).toEqual({})
    expect(validateSessionForm({ ...form, startLocal: '2026-02-30T10:00', occurrenceCount: '53', roomLink: 'https://meet.example/room?token=private' }, 5)).toMatchObject({ startLocal: expect.any(String), occurrenceCount: expect.any(String), roomLink: expect.any(String) })
    expect(nextSuggestedTimes({ dayOfWeek: 'MONDAY', startTime: '10:00', endTime: '11:00' }, new Date('2026-10-19T03:00:00Z'))).toEqual({ startLocal: '2026-10-26T10:00', endLocal: '2026-10-26T11:00', timeZone: 'Asia/Singapore' })
  })
})
