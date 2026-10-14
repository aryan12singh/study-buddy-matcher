import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { AxiosError, AxiosHeaders } from 'axios'
import { api, refreshResources, setAccessToken } from '../../shared/api/client'
import AuthProvider from '../../shared/auth/AuthProvider'
import { AuthContext } from '../../shared/auth/context'
import RequireRole from '../../shared/auth/RequireRole'
import { deferred, renderPage, response, studentAccount } from '../../test/renderApp'
import StudentProfilePage from '../students/StudentProfilePage'
import EditProfilePage from './EditProfilePage'
import type { MyProfile } from './api'

const courses = [
  { id: 1, code: 'IS442', name: 'Object Oriented Programming' },
  { id: 2, code: 'IS212', name: 'Software Project Management' },
  { id: 3, code: 'IS210', name: 'Business Process Analysis and Solutioning' }
]
const profile: MyProfile = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya Nair', school: 'SCIS', programme: 'Information Systems',
  yearOfStudy: 2, contactNumber: '+12025550101', coursesTaken: [courses[0], courses[1]], targetCourse: courses[2],
  preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: ['PROBLEM_SOLVING'],
  availability: [{ dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '11:00:00' }]
}

function serve(data: MyProfile) {
  return vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/profile/me' ? data : courses))
}

afterEach(() => { vi.restoreAllMocks(); setAccessToken(null) })

describe('getting to the edit page', () => {
  const viewed = { id: 1, name: 'Priya Nair', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, coursesTaken: [],
    targetCourse: null, preferredStudyMode: null, studyGoals: [], preferredGroupSizeMin: null, preferredGroupSizeMax: null,
    availability: [], relationship: { state: 'SELF', requestId: null, connectionId: null } }

  it('offers Edit my profile only on your own profile', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(viewed))
    renderPage(<StudentProfilePage />, '/students/1', '/students/:id')
    expect(await screen.findByRole('link', { name: 'Edit my profile' })).toHaveAttribute('href', '/students/1/edit')
  })
  it('does not offer editing on another student’s profile', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...viewed, id: 2, name: 'Jamie', relationship: { state: 'STRANGER', requestId: null, connectionId: null } }))
    renderPage(<StudentProfilePage />, '/students/2', '/students/:id')
    await screen.findByRole('heading', { name: "Jamie's study profile" })
    expect(screen.queryByRole('link', { name: 'Edit my profile' })).not.toBeInTheDocument()
  })
  it('sends another student’s edit URL back to their profile without loading anything', async () => {
    const get = vi.spyOn(api, 'get')
    renderPage(<EditProfilePage />, '/students/2/edit', '/students/:id/edit')
    expect(await screen.findByText('Destination page')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })
})

describe('edit study profile', () => {
  it('shows the saved profile and keeps edits when switching tabs', async () => {
    serve(profile)
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    expect(await screen.findByLabelText('Full name')).toHaveValue('Priya Nair')
    expect(screen.getByLabelText('Contact number')).toHaveValue('+12025550101')
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Priya N.' } })
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Study preferences' }))
    expect(screen.getByLabelText('IS442 Object Oriented Programming')).toBeChecked()
    expect(screen.getByLabelText('IS210 Business Process Analysis and Solutioning')).not.toBeChecked()
    expect(screen.getByLabelText('Online')).toBeChecked()

    fireEvent.click(screen.getByRole('button', { name: 'About you' }))
    expect(screen.getByLabelText('Full name')).toHaveValue('Priya N.')
  })

  it('updates the name in the menu bar as soon as a new name is saved', async () => {
    serve(profile)
    vi.spyOn(api, 'put').mockResolvedValue(response({ ...profile, name: 'Priya N.' }))
    const { auth } = renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Priya N.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    await waitFor(() => expect(auth.updateAccountName).toHaveBeenCalledWith('Priya N.'))
  })
  it('blocks saving without a course before calling the server', async () => {
    serve(profile)
    const put = vi.spyOn(api, 'put')
    renderPage(<EditProfilePage />, '/students/1/edit?tab=preferences', '/students/:id/edit')
    fireEvent.click(await screen.findByLabelText('IS442 Object Oriented Programming'))
    fireEvent.click(screen.getByLabelText('IS212 Software Project Management'))
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    expect(await screen.findByText('Choose at least one course you are currently taking.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })

  it('sends the whole profile and shows a server field error next to its field', async () => {
    serve(profile)
    const put = vi.spyOn(api, 'put').mockRejectedValue(new AxiosError('Bad Request', 'ERR_BAD_REQUEST', undefined, undefined, {
      config: { headers: new AxiosHeaders() }, status: 400, statusText: 'Bad Request', headers: {},
      data: { message: 'Check the highlighted fields', fieldErrors: { courseIds: 'Choose at most 8 courses' } }
    }))
    renderPage(<EditProfilePage />, '/students/1/edit?tab=preferences', '/students/:id/edit')
    fireEvent.click(await screen.findByLabelText('IS210 Business Process Analysis and Solutioning'))
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me', expect.objectContaining({
      courseIds: [1, 2, 3], targetCourseId: 3, preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER',
      studyGoals: ['PROBLEM_SOLVING'], name: 'Priya Nair', yearOfStudy: 2
    })))
    expect(await screen.findByText('Choose at most 8 courses')).toBeInTheDocument()
  })
  it('explains how the target preference is used without claiming it selects every search', async () => {
    serve(profile)
    renderPage(<EditProfilePage />, '/students/1/edit?tab=preferences', '/students/:id/edit')
    expect(await screen.findByText('Matching compares this preference with other students. It also sets your home preview; choose the course for each search on Find buddies.')).toBeInTheDocument()
  })

  it('explains an empty week and saves new availability', async () => {
    serve({ ...profile, availability: [] })
    const put = vi.spyOn(api, 'put').mockResolvedValue(response([{ dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '10:00:00' }]))
    renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    expect(await screen.findByRole('heading', { name: 'No weekly times yet' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save availability' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me/availability',
      { slots: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }] }))
    expect(await screen.findByText('Weekly availability saved.')).toBeInTheDocument()
  })

  it('rejects overlapping time blocks before saving', async () => {
    serve({ ...profile, availability: [
      { dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '11:00:00' },
      { dayOfWeek: 'MONDAY', startTime: '10:00:00', endTime: '12:00:00' }
    ] })
    const put = vi.spyOn(api, 'put')
    renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    fireEvent.click(await screen.findByRole('button', { name: 'Save availability' }))
    expect(await screen.findByText('Time blocks on the same day cannot overlap.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })
  it.each(['25:00', '', '12:'] as const)('blocks availability save for the visible %j draft until it is corrected', async text => {
    serve(profile)
    const put = vi.spyOn(api, 'put').mockResolvedValue(response([{ ...profile.availability[0], startTime: '09:30:00' }]))
    renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    const start = await screen.findByLabelText('Start 1')
    fireEvent.change(start, { target: { value: text } })
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    // A form submit also covers pressing Enter without a blur event.
    fireEvent.submit(start.closest('form')!)
    expect(put).not.toHaveBeenCalled()
    expect(start).toHaveValue(text)
    expect(start).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Correct the highlighted weekly times before saving.')).toBeInTheDocument()
    fireEvent.change(start, { target: { value: '09:30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save availability' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me/availability', {
      slots: [{ ...profile.availability[0], startTime: '09:30' }]
    }))
    expect(await screen.findByText('Weekly availability saved.')).toBeInTheDocument()
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
  })
  it('finishes valid shorthand when availability is submitted without leaving the time box', async () => {
    serve(profile)
    const put = vi.spyOn(api, 'put').mockResolvedValue(response([{ ...profile.availability[0], startTime: '09:30:00' }]))
    renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    const start = await screen.findByLabelText('Start 1')
    fireEvent.change(start, { target: { value: '930' } })
    fireEvent.submit(start.closest('form')!)
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me/availability', {
      slots: [{ ...profile.availability[0], startTime: '09:30' }]
    }))
  })
  it('preserves an invalid availability draft across tabs and guards leaving until it is discarded', async () => {
    serve(profile)
    const put = vi.spyOn(api, 'put')
    const { router } = renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    const start = await screen.findByLabelText('Start 1')
    fireEvent.change(start, { target: { value: '' } })
    fireEvent.blur(start)
    fireEvent.click(screen.getByRole('button', { name: 'About you' }))
    fireEvent.click(screen.getByRole('button', { name: /^Weekly availability/ }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('')
    expect(screen.getByText('Enter the start time.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    expect(screen.getByRole('dialog', { name: 'Discard unsaved changes?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('')
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/students/1'))
    expect(put).not.toHaveBeenCalled()
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(false)
  })
  it('treats returning a draft to its saved value as clean work', async () => {
    serve(profile)
    const { router } = renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    const start = await screen.findByLabelText('Start 1')
    fireEvent.change(start, { target: { value: '25:00' } })
    fireEvent.change(start, { target: { value: '09:00' } })
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/students/1'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

function readFailure(status: number) {
  return new AxiosError('Read failed', 'ERR_BAD_RESPONSE', undefined, undefined, {
    config: { headers: new AxiosHeaders() }, status, statusText: 'Read failed', headers: {},
    data: { message: status === 503 ? 'Temporarily unavailable. Try again.' : 'You cannot read this profile.' }
  })
}

const recoverableReads = [
  { label: 'a profile connection error', path: '/profile/me', failure: () => new AxiosError('Network Error', 'ERR_NETWORK') },
  { label: 'a courses service error', path: '/courses', failure: () => readFailure(503) }
]

describe('owner profile draft recovery', () => {
  it.each(recoverableReads)('keeps the profile draft after saving availability and retrying $label', async ({ path, failure }) => {
    const get = serve(profile)
    const savedSlots = [{ ...profile.availability[0], startTime: '09:30:00' }]
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(savedSlots))
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Unsaved profile name' } })
    fireEvent.click(screen.getByRole('button', { name: 'Weekly availability' }))
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '09:30' } })
    get.mockImplementation(async nextPath => {
      if (nextPath === path) throw failure()
      return response(nextPath === '/profile/me' ? { ...profile, availability: savedSlots } : courses)
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save availability' }))
    await screen.findByRole('alert')
    get.mockImplementation(async nextPath => response(nextPath === '/profile/me' ? { ...profile, availability: savedSlots } : courses))
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument())
    fireEvent.click(await screen.findByRole('button', { name: 'About you' }))
    expect(screen.getByLabelText('Full name')).toHaveValue('Unsaved profile name')
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Weekly availability' }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('09:30')
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    expect(screen.getByRole('dialog', { name: 'Discard unsaved changes?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(put).toHaveBeenCalledTimes(1)
    expect(put).toHaveBeenCalledWith('/profile/me/availability', {
      slots: [{ ...profile.availability[0], startTime: '09:30' }]
    })
  })

  it('keeps an invalid availability draft after saving the profile and retrying a profile 503', async () => {
    const get = serve(profile)
    const savedProfile = { ...profile, name: 'Saved profile name' }
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(savedProfile))
    renderPage(<EditProfilePage />, '/students/1/edit?tab=availability', '/students/:id/edit')
    const start = await screen.findByLabelText('Start 1')
    fireEvent.change(start, { target: { value: '25:00' } })
    fireEvent.blur(start)
    fireEvent.click(screen.getByRole('button', { name: 'About you' }))
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: savedProfile.name } })
    get.mockImplementation(async nextPath => {
      if (nextPath === '/profile/me') throw readFailure(503)
      return response(nextPath === '/profile/me' ? savedProfile : courses)
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    await screen.findByRole('alert')
    get.mockImplementation(async nextPath => response(nextPath === '/profile/me' ? savedProfile : courses))
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await waitFor(() => expect(screen.queryByText('Loading…')).not.toBeInTheDocument())
    fireEvent.click(await screen.findByRole('button', { name: /^Weekly availability/ }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('25:00')
    expect(screen.getByLabelText('Start 1')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    fireEvent.submit(screen.getByLabelText('Start 1').closest('form')!)
    expect(screen.getByText('Correct the highlighted weekly times before saving.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    expect(screen.getByRole('dialog', { name: 'Discard unsaved changes?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    fireEvent.click(screen.getByRole('button', { name: 'About you' }))
    expect(screen.getByLabelText('Full name')).toHaveValue(savedProfile.name)
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
    expect(put).toHaveBeenCalledTimes(1)
  })

  it.each([401, 403])('discards drafts when a profile refresh loses permission with %i', async status => {
    const get = serve(profile)
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Private unsaved name' } })
    get.mockImplementation(async nextPath => {
      if (nextPath === '/profile/me') throw readFailure(status)
      return response(nextPath === '/profile/me' ? profile : courses)
    })
    act(() => refreshResources())
    await screen.findByRole('alert')
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue('Private unsaved name')).not.toBeInTheDocument()
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(false)
    get.mockImplementation(async nextPath => response(nextPath === '/profile/me' ? profile : courses))
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByLabelText('Full name')).toHaveValue(profile.name)
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
  })

  it('removes private drafts immediately for an explicit permission purge, before the next read resolves', async () => {
    const get = serve(profile)
    const pendingProfile = deferred<MyProfile>()
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Private unsaved name' } })
    get.mockImplementation(async path => response(path === '/profile/me' ? await pendingProfile.promise : courses))
    act(() => refreshResources(true))
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue('Private unsaved name')).not.toBeInTheDocument()
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(false)
    await act(async () => { pendingProfile.resolve(profile); await pendingProfile.promise })
    expect(await screen.findByLabelText('Full name')).toHaveValue(profile.name)
  })

  it.each(['signed out', 'another account'] as const)('removes drafts and exit warnings as soon as the account is %s', async transition => {
    serve(profile)
    const { auth, rerender, router } = renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Private unsaved name' } })
    const account = transition === 'signed out' ? null : { ...auth.account, id: 2, email: 'other@example.test' }
    rerender(<AuthContext.Provider value={{ ...auth, account }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue('Private unsaved name')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue(profile.contactNumber)).not.toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(false)
  })

  it('lets session expiration clear a dirty editor and its exit warning before navigating to login', async () => {
    setAccessToken('synthetic-profile-session')
    vi.spyOn(api, 'get').mockImplementation(async path => response(
      path === '/auth/me' ? studentAccount : path === '/profile/me' ? profile : courses
    ))
    const router = createMemoryRouter([
      { element: <RequireRole role="STUDENT" />, children: [{ path: '/students/:id/edit', element: <EditProfilePage /> }] },
      { path: '*', element: <p>Signed out</p> }
    ], { initialEntries: ['/students/1/edit'] })
    render(<AuthProvider><RouterProvider router={router} /></AuthProvider>)
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Private unsaved name' } })
    act(() => window.dispatchEvent(new Event('session-expired')))
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    expect(screen.queryByDisplayValue(profile.contactNumber)).not.toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(false)
  })

  it('shows an accessible first-load error and waits for successful reads before mounting the editor', async () => {
    const get = vi.spyOn(api, 'get').mockRejectedValue(readFailure(503))
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    expect(await screen.findByRole('alert')).toHaveTextContent('Temporarily unavailable. Try again.')
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    get.mockImplementation(async path => response(path === '/profile/me' ? profile : courses))
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByLabelText('Full name')).toHaveValue(profile.name)
  })

  it('keeps a pending write protected during a failed refresh and retry without submitting either form again', async () => {
    const get = serve(profile)
    const pendingWrite = deferred<MyProfile>()
    const put = vi.spyOn(api, 'put').mockImplementation(async () => response(await pendingWrite.promise))
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Name being saved' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    expect(screen.getByRole('button', { name: 'Saving profile…' })).toBeDisabled()
    get.mockImplementation(async path => {
      if (path === '/profile/me') throw readFailure(503)
      return response(courses)
    })
    act(() => refreshResources())
    await screen.findByRole('alert')
    get.mockImplementation(async path => response(path === '/profile/me' ? profile : courses))
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    fireEvent.click(await screen.findByRole('button', { name: 'About you' }))
    expect(screen.getByRole('button', { name: 'Saving profile…' })).toBeDisabled()
    fireEvent.submit(screen.getByLabelText('Full name').closest('form')!)
    fireEvent.click(screen.getByRole('button', { name: 'Weekly availability' }))
    expect(screen.getByRole('button', { name: 'Save availability' })).toBeDisabled()
    fireEvent.submit(screen.getByLabelText('Start 1').closest('form')!)
    expect(put).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('link', { name: 'Back to my profile' }))
    expect(screen.getByRole('dialog', { name: 'Leave while saving?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    await act(async () => { pendingWrite.resolve({ ...profile, name: 'Name being saved' }); await pendingWrite.promise })
    fireEvent.click(screen.getByRole('button', { name: 'About you' }))
    expect(await screen.findByText('Profile saved.')).toBeInTheDocument()
    expect(screen.getByLabelText('Full name')).toHaveValue('Name being saved')
  })

  it('does not rename a newly signed-in account when the previous owner’s save resolves after leaving', async () => {
    serve(profile)
    const pendingWrite = deferred<MyProfile>()
    vi.spyOn(api, 'put').mockImplementation(async () => response(await pendingWrite.promise))
    const { auth, rerender, router } = renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    fireEvent.change(await screen.findByLabelText('Full name'), { target: { value: 'Previous owner name' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    const account = { ...auth.account, id: 2, email: 'other@example.test' }
    rerender(<AuthContext.Provider value={{ ...auth, account }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    await act(async () => { pendingWrite.resolve({ ...profile, name: 'Previous owner name' }); await pendingWrite.promise })
    expect(auth.updateAccountName).not.toHaveBeenCalled()
  })
})
