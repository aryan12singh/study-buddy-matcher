import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { CONTACT_NUMBER_ERROR, CONTACT_NUMBER_HINT } from '../../shared/auth/contactNumber'
import { renderPage, response } from '../../test/renderApp'
import EditProfilePage from './EditProfilePage'
import type { MyProfile } from './api'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const profile: MyProfile = {
  id: 1, email: 'student@example.test', name: 'Student', school: 'SCIS', programme: 'Information Systems',
  yearOfStudy: 2, contactNumber: '+12025550101', coursesTaken: [course], targetCourse: null,
  preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: []
}

afterEach(() => vi.restoreAllMocks())

describe('own profile contact input', () => {
  it('shows the accepted format, rejects extensions inline, and sends corrected international input canonically', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/profile/me' ? profile : [course]))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response({ ...profile, contactNumber: '+12025550100' }))
    renderPage(<EditProfilePage />, '/students/1/edit', '/students/:id/edit')
    const contact = await screen.findByLabelText('Contact number')
    expect(screen.getByText(`${CONTACT_NUMBER_HINT} Shared only with students you are connected to.`)).toBeInTheDocument()
    fireEvent.change(contact, { target: { value: '+12025550100 ext 2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    expect(await screen.findByText(CONTACT_NUMBER_ERROR)).toBeInTheDocument()
    expect(contact).toHaveAttribute('aria-invalid', 'true')
    expect(put).not.toHaveBeenCalled()

    fireEvent.change(contact, { target: { value: '+1 (202) 555-0100' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/profile/me', expect.objectContaining({ contactNumber: '+12025550100' })))
    await waitFor(() => expect(contact).toHaveValue('+12025550100'))
  })
})
