import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { RouterProvider } from 'react-router-dom'
import { api, refreshResources } from '../shared/api/client'
import { AuthContext } from '../shared/auth/context'
import { renderPage, response } from './renderApp'
import GroupDetailPage from '../features/groups/GroupDetailPage'
import GroupManagePage from '../features/groups/GroupManagePage'
import StudentProfilePage from '../features/students/StudentProfilePage'

const group = (id = 5, applicant = false) => ({ id, name: `Study group ${id}`, description: 'Revision', courseId: 3,
  courseCode: 'IS442', courseName: 'OOP', leaderId: 9, leaderName: 'Leader', preferredStudyMode: 'IN_PERSON',
  studyGoals: [], maxGroupSize: 5, memberCount: 2, active: true, pendingApplications: null,
  viewer: { leader: false, member: !applicant, requestId: applicant ? 20 + id : null, requestStatus: applicant ? 'PENDING' : null },
  createdAt: '2026-10-20T00:00:00Z', availability: [], members: applicant ? []
    : [{ studentId: 1, name: 'Current student', leader: false, joinedAt: '2026-10-20T01:00:00Z' }] })
const profile = (id = 2, state = 'INCOMING_PENDING', relationshipId = 10) => ({ id, name: `Student ${id}`, school: 'SCIS',
  programme: 'Information Systems', yearOfStudy: 3, coursesTaken: [], targetCourse: null, preferredStudyMode: null,
  studyGoals: [], preferredGroupSizeMin: null, preferredGroupSizeMax: null, availability: [],
  relationship: { state, requestId: state.endsWith('PENDING') ? relationshipId : null, connectionId: state === 'CONNECTED' ? relationshipId : null } })

afterEach(() => vi.restoreAllMocks())

describe('confirmation target lifetime', () => {
  it.each([false, true])('dismisses a group confirmation when navigating to another group (applicant: %s)', async applicant => {
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/sessions') ? [] : group(Number(path.split('/')[2]), applicant))))
    const post = vi.spyOn(api, 'post')
    const { router } = renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    fireEvent.click(await screen.findByRole('button', { name: applicant ? 'Withdraw application' : 'Leave group' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Study group 5')
    await act(async () => { await router.navigate('/groups/6') })
    await screen.findByRole('heading', { name: 'Study group 6' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it.each(['INCOMING_PENDING', 'OUTGOING_PENDING', 'CONNECTED'])('dismisses a profile confirmation when navigating to another student (%s)', async state => {
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(profile(Number(path.split('/')[2]), state, path.includes('/2/') ? 10 : 11))))
    const { router } = renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    const label = state === 'CONNECTED' ? 'Disconnect' : state === 'INCOMING_PENDING' ? 'Decline request' : 'Withdraw request'
    fireEvent.click(await screen.findByRole('button', { name: label }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Student 2')
    await act(async () => { await router.navigate('/students/3') })
    await screen.findByRole('heading', { name: "Student 3's study profile" })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('dismisses an application confirmation when a refresh replaces the pending application', async () => {
    let requestId = 25
    vi.spyOn(api, 'get').mockImplementation(() => Promise.resolve(response({ ...group(5, true), viewer: { ...group(5, true).viewer, requestId } })))
    renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Withdraw application' }))
    requestId = 26
    act(() => refreshResources())
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it.each(['INCOMING_PENDING', 'OUTGOING_PENDING', 'CONNECTED'])('dismisses a profile confirmation when a refresh replaces its relation (%s)', async state => {
    let relationshipId = 10
    vi.spyOn(api, 'get').mockImplementation(() => Promise.resolve(response(profile(2, state, relationshipId))))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    const label = state === 'CONNECTED' ? 'Disconnect' : state === 'INCOMING_PENDING' ? 'Decline request' : 'Withdraw request'
    fireEvent.click(await screen.findByRole('button', { name: label }))
    relationshipId = 11
    act(() => refreshResources())
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it.each(['group', 'profile', 'manage'])('clears the %s confirmation when the signed-in account changes', async kind => {
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/sessions') || path.endsWith('/join-requests') ? []
      : kind === 'profile' ? profile() : kind === 'manage' ? { ...group(), viewer: { leader: true, member: true } } : group())))
    const { auth, rerender, router } = kind === 'group'
      ? renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
      : kind === 'manage' ? renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
        : renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    fireEvent.click(await screen.findByRole('button', { name: kind === 'group' ? 'Leave group' : kind === 'manage' ? 'Close group' : 'Decline request' }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    rerender(<AuthContext.Provider value={{ ...auth, account: { ...auth.account, id: 7 } }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it.each(['group', 'profile', 'manage'])('clears the %s confirmation and stops fetching after logout', async kind => {
    const get = vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/sessions') || path.endsWith('/join-requests') ? []
      : kind === 'profile' ? profile() : kind === 'manage' ? { ...group(), viewer: { leader: true, member: true } } : group())))
    const { auth, rerender, router } = kind === 'group'
      ? renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
      : kind === 'manage' ? renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
        : renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    fireEvent.click(await screen.findByRole('button', { name: kind === 'group' ? 'Leave group' : kind === 'manage' ? 'Close group' : 'Decline request' }))
    const reads = get.mock.calls.length
    rerender(<AuthContext.Provider value={{ ...auth, account: null }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await act(async () => {})
    expect(get).toHaveBeenCalledTimes(reads)
  })

  it('dismisses a leave confirmation when the current membership tenure changes', async () => {
    let joinedAt = '2026-10-20T01:00:00Z'
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/sessions') ? []
      : { ...group(), members: [{ ...group().members[0], joinedAt }] })))
    renderPage(<GroupDetailPage />, '/groups/5', '/groups/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Leave group' }))
    joinedAt = '2026-10-23T01:00:00Z'
    await act(async () => { refreshResources() })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('dismisses group closure when the managed group changes', async () => {
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/join-requests') ? [] : { ...group(Number(path.split('/')[2])), viewer: { leader: true, member: true } })))
    const { router } = renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    fireEvent.click(await screen.findByRole('button', { name: 'Close group' }))
    await act(async () => { await router.navigate('/groups/6/manage') })
    await screen.findByRole('heading', { name: 'Manage Study group 6' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('dismisses a member removal when that student has a new membership tenure', async () => {
    let joinedAt = '2026-10-20T01:00:00Z'
    vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path.endsWith('/join-requests') ? []
      : { ...group(), viewer: { leader: true, member: true }, members: [{ studentId: 2, name: 'Member', leader: false, joinedAt }] })))
    renderPage(<GroupManagePage />, '/groups/5/manage', '/groups/:id/manage')
    fireEvent.click(await screen.findByRole('button', { name: 'Members' }))
    fireEvent.click(screen.getByRole('button', { name: 'Remove member' }))
    joinedAt = '2026-10-23T01:00:00Z'
    await act(async () => { refreshResources() })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('keeps a same-target confirmation open across an unchanged refresh', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response(profile()))
    const post = vi.spyOn(api, 'post').mockRejectedValue(new Error('Try later'))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Decline request' }))
    const dialog = screen.getByRole('dialog')
    await act(async () => { refreshResources() })
    expect(get).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('dialog')).toBe(dialog)
    fireEvent.click(within(dialog).getByRole('button', { name: 'Decline request' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/match-requests/10/decline'))
  })
})
