import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { refreshResources } from '../../shared/api/client'
import { REFRESH_INTERVAL_MS } from '../../shared/api/useResource'
import { deferred, renderPage, response } from '../../test/renderApp'
import StudentProfilePage from './StudentProfilePage'
import { RouterProvider } from 'react-router-dom'
import { AuthContext } from '../../shared/auth/context'
import { adminAccount } from '../../test/renderApp'
import { achievementStanding } from '../../test/achievementStanding'

const publicProfile = { id: 2, name: 'Jamie', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, coursesTaken: [], targetCourse: null, preferredStudyMode: null, studyGoals: [], preferredGroupSizeMin: null, preferredGroupSizeMax: null, availability: [], relationship: { state: 'STRANGER', requestId: null, connectionId: null } }
afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})
describe('profile privacy and relationship actions', () => {
  it('loads a compact private achievement summary only on the signed-in students own self profile', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path === '/achievements/me'
      ? achievementStanding : { ...publicProfile, id: 1, relationship: { state: 'SELF' } })))
    renderPage(<StudentProfilePage />, '/students/1', '/students/:id')
    const summary = await screen.findByRole('region', { name: 'Your achievements' })
    expect(await within(summary).findByText('Current planned streak')).toBeInTheDocument()
    expect(within(summary).getByText('Private to you. Current badge tiers reflect your records; the streak counts planned session weeks.')).toBeInTheDocument()
    expect(within(summary).getByRole('link', { name: 'View all achievements' })).toHaveAttribute('href', '/achievements')
    expect(get.mock.calls.filter(([path]) => path === '/achievements/me')).toHaveLength(1)
  })
  it.each(['SELF', 'STRANGER', 'CONNECTED'])('never fetches or renders achievements for another students %s profile', async state => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ ...publicProfile, relationship: { state } }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByText('No weekly availability saved.')
    expect(screen.queryByRole('region', { name: 'Your achievements' })).not.toBeInTheDocument()
    expect(get.mock.calls.every(([path]) => path !== '/achievements/me')).toBe(true)
  })
  it('does not fetch achievements for administrators even if a self relationship is supplied', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ ...publicProfile, id: 99, relationship: { state: 'SELF' } }))
    renderPage(<StudentProfilePage />, '/students/99', '/students/:id', adminAccount)
    await screen.findByText('No weekly availability saved.')
    expect(screen.queryByRole('region', { name: 'Your achievements' })).not.toBeInTheDocument()
    expect(get.mock.calls.every(([path]) => path !== '/achievements/me')).toBe(true)
  })
  it('keeps the profile usable when the independent achievement request fails and supports retry', async () => {
    let achievementReads = 0
    vi.spyOn(api, 'get').mockImplementation(path => {
      if (path !== '/achievements/me') return Promise.resolve(response({ ...publicProfile, id: 1, relationship: { state: 'SELF' } }))
      return ++achievementReads === 1 ? Promise.reject(new Error('Achievement service unavailable')) : Promise.resolve(response(achievementStanding))
    })
    renderPage(<StudentProfilePage />, '/students/1', '/students/:id')
    expect(await screen.findByRole('alert')).toHaveTextContent('Achievement service unavailable')
    expect(screen.getByRole('link', { name: 'Edit my profile' })).toBeInTheDocument()
    expect(screen.getByText('No weekly availability saved.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await screen.findByText('Current planned streak')
  })
  it('aborts the private request when navigating to another profile and ignores a late response', async () => {
    const pending = deferred<never>()
    const get = vi.spyOn(api, 'get').mockImplementation(path => path === '/achievements/me' ? pending.promise
      : Promise.resolve(response(path === '/students/1/profile' ? { ...publicProfile, id: 1, relationship: { state: 'SELF' } } : publicProfile)))
    const { router } = renderPage(<StudentProfilePage />, '/students/1', '/students/:id')
    await screen.findByRole('region', { name: 'Your achievements' })
    await waitFor(() => expect(get.mock.calls.some(([path]) => path === '/achievements/me')).toBe(true))
    const signal = get.mock.calls.find(([path]) => path === '/achievements/me')?.[1]?.signal as AbortSignal
    await act(async () => { await router.navigate('/students/2') })
    expect(signal.aborted).toBe(true)
    await act(async () => { pending.resolve(response(achievementStanding)) })
    expect(screen.queryByRole('region', { name: 'Your achievements' })).not.toBeInTheDocument()
  })
  it('hides private standing immediately when the signed-in owner changes or logs out', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(path => Promise.resolve(response(path === '/achievements/me'
      ? achievementStanding : { ...publicProfile, id: 1, relationship: { state: 'SELF' } })))
    const { auth, router, rerender } = renderPage(<StudentProfilePage />, '/students/1', '/students/:id')
    await screen.findByText('Current planned streak')
    rerender(<AuthContext.Provider value={{ ...auth, account: { ...auth.account, id: 2 } }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByRole('region', { name: 'Your achievements' })).not.toBeInTheDocument()
    rerender(<AuthContext.Provider value={{ ...auth, account: null }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByRole('region', { name: 'Your achievements' })).not.toBeInTheDocument()
    expect(get.mock.calls.filter(([path]) => path === '/achievements/me')).toHaveLength(1)
  })
  it('links a sent request to the outgoing requests view', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...publicProfile,
      relationship: { state: 'OUTGOING_PENDING', requestId: 7, connectionId: null }
    }))
    const { router } = renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    const link = await screen.findByRole('link', { name: 'View your requests' })
    expect(link).toHaveAttribute('href', '/connections?view=outgoing')
    fireEvent.click(link)
    await waitFor(() => expect(router.state.location.pathname + router.state.location.search).toBe('/connections?view=outgoing'))
  })
  it.each([
    { origin: 'matching', path: '/find-buddies?courseId=3&sort=MOST_SHARED_HOURS', label: 'Back to matching', state: { returnTo: { path: '/find-buddies?courseId=3&sort=MOST_SHARED_HOURS', label: 'Back to matching' } } },
    { origin: 'a direct link', path: '/connections', label: 'Back to connections', state: undefined }
  ])('preserves both unavailable-profile return actions from $origin', async ({ path, label, state }) => {
    const config = { headers: new AxiosHeaders() }
    vi.spyOn(api, 'get').mockRejectedValue(new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined,
      { status: 404, statusText: '', headers: {}, config, data: { message: 'Student 2 not found' } }))
    const { router } = renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    if (state) await act(async () => { await router.navigate('/students/2', { state }) })
    await screen.findByText('Profile unavailable')

    const actions = screen.getAllByRole('link', { name: label })
    expect(actions).toHaveLength(2)
    for (const action of actions) expect(action).toHaveAttribute('href', path)
    fireEvent.click(actions[1])
    await screen.findByText('Destination page')
    expect(router.state.location.pathname + router.state.location.search).toBe(path)
  })
  it.each([
    { status: 404, unavailable: true },
    { status: 500, unavailable: false }
  ])('shows a $status as unavailable: $unavailable', async ({ status, unavailable }) => {
    const config = { headers: new AxiosHeaders() }
    vi.spyOn(api, 'get').mockRejectedValue(new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined,
      { status, statusText: '', headers: {}, config, data: { message: 'Student 2 not found' } }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    if (unavailable) {
      expect(await screen.findByText('Profile unavailable')).toBeInTheDocument()
      expect(screen.queryByText('Student 2 not found')).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
    } else {
      expect(await screen.findByRole('button', { name: 'Try again' })).toBeInTheDocument()
    }
  })
  it.each(['STRANGER', 'OUTGOING_PENDING', 'INCOMING_PENDING'])('withholds copy access for the %s relationship even if a contact field is accidentally supplied', async state => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...publicProfile, contactNumber: 'Hidden contact', relationship: { state, requestId: 10, connectionId: null } }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByText('Contact number is private until a buddy request is accepted. Group membership does not share contact numbers.')
    expect(screen.queryByRole('button', { name: 'Copy contact number' })).not.toBeInTheDocument()
    expect(screen.queryByText('Hidden contact')).not.toBeInTheDocument()
  })
  it('removes the contact copy control during a privacy-purging refresh', async () => {
    const pending = deferred<never>()
    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ ...publicProfile, contactNumber: 'Synthetic contact', relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 } })).mockImplementation(() => pending.promise)
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByRole('button', { name: 'Copy contact number' })
    refreshResources(true)
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Copy contact number' })).not.toBeInTheDocument())
    pending.resolve(response(publicProfile))
    await screen.findByRole('button', { name: 'Send match request' })
  })
  it('shows public study information, honest missing data and a send action', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(publicProfile))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    expect(await screen.findByText('Contact number is private until a buddy request is accepted. Group membership does not share contact numbers.')).toBeInTheDocument()
    expect(screen.getByText('No weekly availability saved.')).toBeInTheDocument()
    expect(screen.getByText('No courses listed.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send match request' })).toBeInTheDocument()
  })
  it.each(['focus', 'timer'] as const)('keeps the request draft and context across %s refreshes and drops a contact the refreshed profile no longer carries', async refresh => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
    const pending = deferred<never>()
    const connectedProfile = {
      ...publicProfile,
      contactNumber: '+65 9999 1111',
      relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 }
    }
    let profileReads = 0
    vi.spyOn(api, 'get').mockImplementation(path => {
      if (path === '/courses') {
        return Promise.resolve(response([{ id: 3, code: 'IS442', name: 'Object Oriented Programming' }]))
      }
      profileReads++
      if (profileReads === 1) return Promise.resolve(response(publicProfile))
      if (profileReads === 2) return Promise.resolve(response(connectedProfile))
      return pending.promise
    })
    const post = vi.spyOn(api, 'post').mockRejectedValue({
      isAxiosError: true,
      response: { status: 409, data: { message: 'Already connected to this student.' } }
    })
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Send match request' }))
    const dialog = screen.getByRole('dialog', { name: 'Send match request to Jamie' })
    await within(dialog).findByLabelText('Course')
    fireEvent.change(within(dialog).getByLabelText('Message (optional)'), { target: { value: 'Review the OOP exercises together?' } })
    fireEvent.change(within(dialog).getByLabelText('Course'), { target: { value: '3' } })
    fireEvent.change(within(dialog).getByLabelText('Study goal'), { target: { value: 'PROBLEM_SOLVING' } })

    async function refreshProfile() {
      await act(async () => {
        if (refresh === 'focus') window.dispatchEvent(new Event('focus'))
        else vi.advanceTimersByTime(REFRESH_INTERVAL_MS)
      })
    }
    function expectDraft() {
      expect(screen.getByRole('dialog')).toBe(dialog)
      expect(within(dialog).getByLabelText('Message (optional)')).toHaveValue('Review the OOP exercises together?')
      expect(within(dialog).getByLabelText('Course')).toHaveValue('3')
      expect(within(dialog).getByLabelText('Study goal')).toHaveValue('PROBLEM_SOLVING')
    }

    await refreshProfile()
    expect(screen.getByText('+65 9999 1111')).toBeInTheDocument()
    expectDraft()
    await refreshProfile()
    expect(profileReads).toBe(3)
    // The page stays on screen while revalidating instead of blanking to a skeleton.
    expect(screen.getByText('+65 9999 1111')).toBeInTheDocument()
    expect(screen.queryByText('Loading…', { selector: '.loading-caption' })).not.toBeInTheDocument()
    expectDraft()
    // The other student ended the connection: the refreshed public profile replaces the contact.
    await act(async () => {
      pending.resolve(response(publicProfile))
      await pending.promise
    })
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
    expectDraft()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Already connected to this student.')
    expect(post).toHaveBeenCalledWith('/match-requests', {
      receiverId: 2,
      message: 'Review the OOP exercises together?',
      context: { origin: 'PROFILE', courseId: 3, studyGoal: 'PROBLEM_SOLVING' }
    })
    expectDraft()
  })
  it.each(['STRANGER', 'INCOMING_PENDING', 'OUTGOING_PENDING'])('withholds displayed contact in %s state even if a bad payload supplies it', async state => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...publicProfile, contactNumber: '+65 9999 1111', relationship: { state, requestId: 10, connectionId: null } }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByText('SCIS')
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
  })
  it('accepts incoming requests and refetches the connected profile', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ ...publicProfile, relationship: { state: 'INCOMING_PENDING', requestId: 10, connectionId: null } })).mockResolvedValue(response({ ...publicProfile, contactNumber: '+65 9999 1111', relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 } }))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ status: 'ACCEPTED' }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    fireEvent.click(await screen.findByRole('button', { name: 'Accept request' }))
    expect(await screen.findByText('+65 9999 1111')).toBeInTheDocument()
    expect(post).toHaveBeenCalledWith('/match-requests/10/accept')
  })
  it('immediately purges connected contact after disconnect, before the public refetch completes', async () => {
    const pending = deferred<never>()
    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ ...publicProfile, contactNumber: '+65 9999 1111', relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 } })).mockImplementationOnce(() => pending.promise)
    vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByText('+65 9999 1111')
    fireEvent.click(screen.getByRole('button', { name: 'Disconnect' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Disconnect' }))
    await waitFor(() => expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument())
    pending.resolve(response(publicProfile))
    expect(await screen.findByRole('button', { name: 'Send match request' })).toBeInTheDocument()
  })
  it('discards an earlier private payload when a refresh fails', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response({ ...publicProfile, contactNumber: '+65 9999 1111', relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 } })).mockRejectedValueOnce(new Error('Student unavailable'))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByText('+65 9999 1111')
    refreshResources(true)
    expect(await screen.findByRole('alert')).toHaveTextContent('Student unavailable')
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
  })
  it('does not fetch malformed or missing student IDs', async () => {
    const get = vi.spyOn(api, 'get')
    renderPage(<StudentProfilePage />, '/students/not-a-number', '/students/:id')
    expect(screen.getByRole('alert')).toHaveTextContent('invalid')
    expect(get).not.toHaveBeenCalled()
  })
})
