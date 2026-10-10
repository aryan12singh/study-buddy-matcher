import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import ConnectionsPage from '../connections/ConnectionsPage'

const is442 = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const is216 = { id: 3, code: 'IS216', name: 'Web Application Development' }
const myProfile = { id: 1, coursesTaken: [is442, is216], targetCourse: is442 }
const jamie = { studentId: 2, name: 'Jamie Lee', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, quality: 'STRONG', sharedHoursPerWeek: 3 }
const alex = { studentId: 5, name: 'Alex Tan', school: 'SOE', programme: 'Economics', yearOfStudy: 1, quality: 'FAIR', sharedHoursPerWeek: 0 }
afterEach(() => { vi.restoreAllMocks() })

/** Answers the student's profile and match searches by URL; a custom match response or profile can be supplied. */
function mockApi(matches: () => Promise<unknown> = () => Promise.resolve(response([jamie, alex])), profile: object = myProfile) {
  return vi.spyOn(api, 'get').mockImplementation(url => (url === '/matches' ? matches()
    : Promise.resolve(response(url === '/courses' ? [is442, is216] : profile))) as never)
}

async function openFindBuddies() {
  const result = renderPage(<ConnectionsPage />, '/connections?view=find', '/connections')
  await screen.findByRole('option', { name: 'IS216 Web Application Development' })
  return result
}

describe('Find buddies tab on the Connections page', () => {
  it('opens with the search form and does not search until asked', async () => {
    const get = mockApi()
    await openFindBuddies()
    expect(screen.getByRole('button', { name: 'Find buddies' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Find a study buddy')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalledWith('/matches', expect.anything())
    expect(get).not.toHaveBeenCalledWith('/match-requests', expect.anything())
  })

  it('opens when switching from another tab', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const { router } = renderPage(<ConnectionsPage />, '/connections', '/connections')
    expect(await screen.findByText('No incoming requests')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Find buddies' }))
    expect(await screen.findByText('Find a study buddy')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?view=find')
  })

  it('offers only my courses and starts at my target course', async () => {
    mockApi()
    await openFindBuddies()
    const course = screen.getByLabelText('Course')
    expect(course).toHaveValue('4')
    expect(within(course).getAllByRole('option').map(option => option.textContent))
      .toEqual(['Any course', 'IS216 Web Application Development', 'IS442 Object Oriented Programming'])
  })

  it('points a student with no courses to their profile but still allows a goal search', async () => {
    const get = mockApi(undefined, { id: 1, coursesTaken: [], targetCourse: null })
    renderPage(<ConnectionsPage />, '/connections?view=find', '/connections')
    expect(await screen.findByRole('link', { name: 'Add courses to your profile' })).toHaveAttribute('href', '/students/1/edit')
    expect(screen.getByLabelText('Course')).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { studyGoal: 'EXAM_PREPARATION' } })))
  })

  it('asks for a course or study goal before searching', async () => {
    const get = mockApi()
    await openFindBuddies()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    expect(screen.getByText('Choose a course or a study goal to search.')).toBeInTheDocument()
    expect(screen.getByLabelText('Course')).toHaveAttribute('aria-invalid', 'true')
    expect(get).not.toHaveBeenCalledWith('/matches', expect.anything())
  })

  it('sends only the chosen filters and lists ranked results in plain language', async () => {
    const get = mockApi()
    await openFindBuddies()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Study mode'), { target: { value: 'ONLINE' } })
    fireEvent.change(screen.getByLabelText('Group size'), { target: { value: 'SMALL_GROUP' } })
    fireEvent.change(screen.getByLabelText('Minimum match quality'), { target: { value: 'GOOD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    expect(await screen.findByRole('link', { name: 'Jamie Lee' })).toHaveAttribute('href', '/students/2')
    expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4, studyMode: 'ONLINE', groupSize: 'SMALL_GROUP', minQuality: 'GOOD' } }))
    const names = screen.getAllByRole('heading', { level: 2 }).map(heading => heading.textContent)
    expect(names).toEqual(['Jamie Lee', 'Alex Tan'])
    expect(screen.getByText('Strong match')).toBeInTheDocument()
    expect(screen.getByText('3 hours of shared free time a week')).toBeInTheDocument()
    expect(screen.getByText('No shared free time in your weekly schedules')).toBeInTheDocument()
  })

  it('explains an empty result', async () => {
    mockApi(() => Promise.resolve(response([])))
    await openFindBuddies()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    expect(await screen.findByText('No students match these filters')).toBeInTheDocument()
  })

  it('shows an error with a working retry', async () => {
    let attempts = 0
    mockApi(() => ++attempts === 1 ? Promise.reject(new Error('Network down')) : Promise.resolve(response([jamie])))
    await openFindBuddies()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    expect(await screen.findByText('Unable to load this view')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'Jamie Lee' })).toBeInTheDocument()
  })

  it('sends a match request with the search as matching context', async () => {
    mockApi()
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    await openFindBuddies()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search for study buddies' }))
    const jamieRow = (await screen.findByRole('link', { name: 'Jamie Lee' })).closest('article') as HTMLElement
    fireEvent.click(within(jamieRow).getByRole('button', { name: 'Send match request' }))
    const dialog = await screen.findByRole('dialog', { name: 'Send match request to Jamie Lee' })
    await within(dialog).findByRole('option', { name: /IS442/ })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/match-requests',
      { receiverId: 2, message: null, context: { origin: 'MATCHING', courseId: 4, studyGoal: 'EXAM_PREPARATION' } }))
    expect(await screen.findByText(/Match request sent to Jamie Lee/)).toBeInTheDocument()
  })
})
