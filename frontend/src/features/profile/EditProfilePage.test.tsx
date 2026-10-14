import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
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
  yearOfStudy: 2, contactNumber: 'Synthetic contact 1', coursesTaken: [courses[0], courses[1]], targetCourse: courses[2],
  preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: ['PROBLEM_SOLVING'],
  availability: [{ dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '11:00:00' }]
}

function serve(data: MyProfile) {
  return vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/profile/me' ? data : courses))
}

afterEach(() => vi.restoreAllMocks())

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
    expect(screen.getByLabelText('Contact number')).toHaveValue('Synthetic contact 1')
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
