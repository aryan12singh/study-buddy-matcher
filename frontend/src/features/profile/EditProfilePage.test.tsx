import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
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

describe('edit study profile', () => {
  it('shows the saved profile and keeps edits when switching tabs', async () => {
    serve(profile)
    renderPage(<EditProfilePage />, '/profile', '/profile')
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

  it('blocks saving without a course before calling the server', async () => {
    serve(profile)
    const put = vi.spyOn(api, 'put')
    renderPage(<EditProfilePage />, '/profile?tab=preferences', '/profile')
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
    renderPage(<EditProfilePage />, '/profile?tab=preferences', '/profile')
    fireEvent.click(await screen.findByLabelText('IS210 Business Process Analysis and Solutioning'))
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me', expect.objectContaining({
      courseIds: [1, 2, 3], targetCourseId: 3, preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER',
      studyGoals: ['PROBLEM_SOLVING'], name: 'Priya Nair', yearOfStudy: 2
    })))
    expect(await screen.findByText('Choose at most 8 courses')).toBeInTheDocument()
  })

  it('explains an empty week and saves new availability', async () => {
    serve({ ...profile, availability: [] })
    const put = vi.spyOn(api, 'put').mockResolvedValue(response([{ dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '10:00:00' }]))
    renderPage(<EditProfilePage />, '/profile?tab=availability', '/profile')
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
    renderPage(<EditProfilePage />, '/profile?tab=availability', '/profile')
    fireEvent.click(await screen.findByRole('button', { name: 'Save availability' }))
    expect(await screen.findByText('Time blocks on the same day cannot overlap.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })
})
