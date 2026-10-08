import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { refreshResources } from '../../shared/api/client'
import { REFRESH_INTERVAL_MS } from '../../shared/api/useResource'
import { deferred, renderPage, response } from '../../test/renderApp'
import StudentProfilePage from './StudentProfilePage'

const publicProfile = { id: 2, name: 'Jamie', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, coursesTaken: [], targetCourse: null, preferredStudyMode: null, studyGoals: [], preferredGroupSizeMin: null, preferredGroupSizeMax: null, availability: [], relationship: { state: 'STRANGER', requestId: null, connectionId: null } }
afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})
describe('profile privacy and relationship actions', () => {
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
  it.each(['focus', 'timer'] as const)('keeps the request draft and context across %s refreshes while purging private profile data', async refresh => {
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
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
    expect(screen.getByText('Loading…', { selector: '.loading-caption' })).toBeInTheDocument()
    expectDraft()
    await act(async () => {
      pending.resolve(response(connectedProfile))
      await pending.promise
    })
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
