import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api, refreshResources } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import FindBuddiesPage from './FindBuddiesPage'

const is442 = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const is216 = { id: 3, code: 'IS216', name: 'Web Application Development' }
const myProfile = { id: 1, coursesTaken: [is442, is216], targetCourse: is442, preferredStudyMode: 'ONLINE', availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '11:00' }] }
const breakdown = (course: number, availability: number) => [
  { criterion: 'COURSE', score: course, enabled: true, known: true }, { criterion: 'AVAILABILITY', score: availability, enabled: true, known: true },
  { criterion: 'STUDY_MODE', score: 1, enabled: true, known: true }, { criterion: 'STUDY_GOAL', score: 0, enabled: true, known: true }, { criterion: 'GROUP_SIZE', score: 0.5, enabled: true, known: true }
]
const jamie = {
  studentId: 2, name: 'Jamie Lee', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, studyMode: 'ONLINE',
  studyGoals: ['EXAM_PREPARATION'], totalScore: 0.8, explanation: 'Same course and 2 shared hours per week.', relevantCourse: is442, quality: 'STRONG', breakdown: breakdown(1, 0.5), sharedHoursPerWeek: 2,
  sharedSlots: [{ dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '11:00:00' }], relationship: 'STRANGER'
}
const alex = {
  studentId: 5, name: 'Alex Tan', school: 'SOE', programme: 'Economics', yearOfStudy: 1, studyMode: null,
  studyGoals: [], totalScore: 0.2, explanation: 'Different courses and no shared hours.', relevantCourse: null, quality: 'FAIR', breakdown: breakdown(0, 0), sharedHoursPerWeek: 0, sharedSlots: [], relationship: 'STRANGER'
}
afterEach(() => { vi.restoreAllMocks() })

/** A search response ranked by the given strategy. */
function ranked(matches: object[], strategy = 'BALANCED') {
  return { strategy, matches, sort: 'BEST_MATCH', configurationVersion: 2, totalMatches: matches.length, limit: 20 }
}

/** Answers the student's profile and match searches by URL; a custom match response or profile can be supplied. */
function mockApi(matches: () => Promise<unknown> = () => Promise.resolve(response(ranked([jamie, alex]))), profile: object = myProfile) {
  return vi.spyOn(api, 'get').mockImplementation(url => (url === '/matches' ? matches()
    : Promise.resolve(response(url === '/courses' ? [is442, is216] : profile))) as never)
}

function openPage(path = '/find-buddies?courseId=4') {
  return renderPage(<FindBuddiesPage />, path, '/find-buddies')
}

function names() {
  return within(screen.getByLabelText('Matching students')).getAllByRole('heading', { level: 3 }).map(heading => heading.textContent)
}

async function openFilters() {
  fireEvent.click(screen.getByRole('button', { name: /^Filters/ }))
  await screen.findByRole('option', { name: 'IS216 Web Application Development' })
}

async function card(name: string) {
  return (await screen.findByRole('link', { name })).closest('article') as HTMLElement
}

async function sendRequest(name: string) {
  const row = await card(name)
  fireEvent.click(within(row).getByRole('button', { name: 'Send match request' }))
  const dialog = await screen.findByRole('dialog', { name: `Send match request to ${name}` })
  await within(dialog).findByRole('option', { name: /IS442/ })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
  await screen.findByText(`Match request sent to ${name}. Track it under Outgoing requests on Connections.`)
  return row
}

describe('Find buddies page', () => {
  it('uses an explicit course search in backend rank order', async () => {
    const get = mockApi()
    openPage()
    expect(await screen.findByRole('link', { name: 'Jamie Lee' })).toHaveAttribute('href', '/students/2')
    expect(screen.getByRole('heading', { name: 'Find buddies' })).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4 } }))
    expect(screen.getByText('2 students match your search')).toBeInTheDocument()
    expect(names()).toEqual(['Jamie Lee Year 2', 'Alex Tan Year 1'])
    expect(screen.queryByRole('form', { name: 'Filter study buddies' })).not.toBeInTheDocument()
  })

  it('names the ranking strategy the admin chose', async () => {
    mockApi(() => Promise.resolve(response(ranked([jamie], 'AVAILABILITY_FIRST'))))
    openPage()
    expect(await screen.findByText(/Ranked by Availability first matching\. Ranks by availability overlap/)).toBeInTheDocument()
  })

  it.each(['AVAILABILITY_FIRST', 'COURSE_FIRST', 'BALANCED'])('preserves the %s response order across match qualities', async strategy => {
    const fairFirst = { ...alex, studentId: 8, name: 'Fran Lim', sharedHoursPerWeek: 8, breakdown: breakdown(1, 1) }
    const good = { ...alex, studentId: 9, name: 'Gus Koh', quality: 'GOOD' }
    const strong = strategy === 'COURSE_FIRST' ? { ...jamie, breakdown: breakdown(0, 0.5) } : jamie
    const backendOrder = strategy === 'BALANCED' ? [strong, good, fairFirst, alex] : [fairFirst, strong, good, alex]
    mockApi(() => Promise.resolve(response(ranked(backendOrder, strategy))))
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    const list = screen.getByLabelText('Matching students')
    expect(names()).toEqual(backendOrder.map(match => `${match.name} Year ${match.yearOfStudy}`))
    expect(within(list).getByText('Strong match')).toBeInTheDocument()
    expect(within(list).getByText('Good match')).toBeInTheDocument()
    expect(within(list).getAllByText('Fair match')).toHaveLength(2)
  })

  it('shows the score breakdown, shared hours heatmap and preferences', async () => {
    mockApi()
    openPage()
    const card = (await screen.findByRole('link', { name: 'Jamie Lee' })).closest('article') as HTMLElement
    expect(within(card).getByText('Strong match')).toBeInTheDocument()
    const breakdownList = within(card).getByRole('list', { name: 'Score breakdown' })
    expect(within(breakdownList).getByRole('meter', { name: 'Course score' })).toHaveAttribute('aria-valuetext', 'Same course')
    expect(within(breakdownList).getByRole('meter', { name: 'Availability score' })).toHaveAttribute('aria-valuetext', '2 h')
    expect(within(breakdownList).getByRole('meter', { name: 'Study goal score' })).toHaveAttribute('aria-valuetext', 'Differs')
    expect(within(card).getByTitle('Monday morning: 2 hours shared')).toHaveClass('heat-3')
    expect(within(card).getByTitle('Tuesday morning: not shared')).toHaveClass('heat-0')
    expect(within(card).getByText('2 hours of shared free time a week')).toBeInTheDocument()
    expect(within(card).getByText('Online')).toBeInTheDocument()
    expect(within(card).getByText('Exam preparation')).toBeInTheDocument()
  })

  it('opens a profile with a way back to Find buddies', async () => {
    mockApi()
    const { router } = openPage()
    const card = (await screen.findByRole('link', { name: 'Jamie Lee' })).closest('article') as HTMLElement
    fireEvent.click(within(card).getByRole('link', { name: 'View profile' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/students/2'))
    expect(router.state.location.state).toEqual({ returnTo: { path: '/find-buddies?courseId=4', label: 'Back to find buddies' } })
  })

  it('only offers a match request when none is pending and they are not connected', async () => {
    const requested = { ...jamie, relationship: 'OUTGOING_PENDING' }
    const asked = { ...alex, studentId: 8, name: 'Fran Lim', relationship: 'INCOMING_PENDING' }
    const buddy = { ...alex, studentId: 9, name: 'Gus Koh', relationship: 'CONNECTED' }
    mockApi(() => Promise.resolve(response(ranked([requested, asked, buddy, alex]))))
    openPage()
    const card = async (name: string) => (await screen.findByRole('link', { name })).closest('article') as HTMLElement
    expect(within(await card('Jamie Lee')).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    expect(within(await card('Jamie Lee')).queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    expect(within(await card('Fran Lim')).getByRole('link', { name: 'Respond to their request' })).toHaveAttribute('href', '/connections?view=incoming')
    expect(within(await card('Gus Koh')).getByText('Connected')).toBeInTheDocument()
    expect(within(await card('Gus Koh')).queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    expect(within(await card('Alex Tan')).getByRole('button', { name: 'Send match request' })).toBeEnabled()
  })

  it('applies filters, shows them as chips, and drops one when its chip is removed', async () => {
    const get = mockApi()
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    await openFilters()
    expect(screen.getByLabelText('Course')).toHaveValue('4')
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Study mode'), { target: { value: 'ONLINE' } })
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4, studyGoal: 'EXAM_PREPARATION', studyMode: 'ONLINE' } })))
    expect(await screen.findByText('2 students match your search')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Filters · 3' })).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(screen.getByRole('button', { name: 'Remove IS442 Object Oriented Programming filter' }))
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/matches', expect.objectContaining({ params: { studyGoal: 'EXAM_PREPARATION', studyMode: 'ONLINE' } })))
    expect(screen.getByRole('button', { name: 'Filters · 2' })).toBeInTheDocument()
  })

  it('lets an incomplete profile search the course catalog and offers profile setup', async () => {
    mockApi(undefined, { id: 1, coursesTaken: [], targetCourse: null })
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    fireEvent.click(screen.getByRole('button', { name: /^Filters/ }))
    expect(await screen.findByRole('link', { name: 'Complete your study profile' })).toHaveAttribute('href', '/students/1/edit')
    expect(screen.getByLabelText('Course')).toBeEnabled()
  })

  it('explains an empty search result and updated filters', async () => {
    mockApi(() => Promise.resolve(response(ranked([]))))
    openPage()
    expect(await screen.findByText('No students match this search')).toBeInTheDocument()
    await openFilters()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    expect(await screen.findByText('No students match this search')).toBeInTheDocument()
  })

  it('shows an error with a working retry', async () => {
    let attempts = 0
    mockApi(() => ++attempts === 1 ? Promise.reject(new Error('Network down')) : Promise.resolve(response(ranked([jamie]))))
    openPage()
    expect(await screen.findByText('Unable to load this view')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'Jamie Lee' })).toBeInTheDocument()
  })

  it('sends a match request with the filters as matching context', async () => {
    let sent = false
    mockApi(() => Promise.resolve(response(ranked(sent ? [{ ...jamie, relationship: 'OUTGOING_PENDING' }, alex] : [jamie, alex]))))
    const post = vi.spyOn(api, 'post').mockImplementation(() => { sent = true; return Promise.resolve(response({ id: 9 })) as never })
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    await openFilters()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    await screen.findByText('2 students match your search')
    const jamieRow = screen.getByRole('link', { name: 'Jamie Lee' }).closest('article') as HTMLElement
    fireEvent.click(within(jamieRow).getByRole('button', { name: 'Send match request' }))
    const dialog = await screen.findByRole('dialog', { name: 'Send match request to Jamie Lee' })
    await within(dialog).findByRole('option', { name: /IS442/ })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/match-requests',
      { receiverId: 2, message: null, context: { origin: 'MATCHING', courseId: 4, studyGoal: 'EXAM_PREPARATION' } }))
    expect(await screen.findByText(/Match request sent to Jamie Lee/)).toBeInTheDocument()
    expect(await within(jamieRow).findByRole('button', { name: 'Match requested' })).toBeDisabled()
  })

  it('marks a confirmed recipient pending while the refreshed search is delayed', async () => {
    const refresh = deferred<never>()
    let loads = 0
    mockApi(() => ++loads === 1 ? Promise.resolve(response(ranked([jamie, alex]))) : refresh.promise)
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    const row = await sendRequest('Jamie Lee')
    await waitFor(() => expect(loads).toBe(2))
    expect(within(row).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    expect(within(row).queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    expect(within(await card('Alex Tan')).getByRole('button', { name: 'Send match request' })).toBeEnabled()
    expect(post).toHaveBeenCalledTimes(1)
  })

  it('handles send confirmation while the current search response is unavailable', async () => {
    const post = deferred<never>(), oldRefresh = deferred<never>(), refreshed = deferred<never>()
    let loads = 0
    mockApi(() => {
      loads++
      if (loads === 1) return Promise.resolve(response(ranked([jamie])))
      return loads === 2 ? oldRefresh.promise : refreshed.promise
    })
    vi.spyOn(api, 'post').mockReturnValue(post.promise)
    openPage()
    const row = await card('Jamie Lee')
    fireEvent.click(within(row).getByRole('button', { name: 'Send match request' }))
    const dialog = await screen.findByRole('dialog', { name: 'Send match request to Jamie Lee' })
    await within(dialog).findByRole('option', { name: /IS442/ })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    // Another resource change can purge the cards while this POST is in flight.
    act(() => { refreshResources(true) })
    expect(screen.queryByRole('link', { name: 'Jamie Lee' })).not.toBeInTheDocument()
    await act(async () => { post.resolve(response({ id: 9 })) })
    expect(await screen.findByText(/Match request sent to Jamie Lee/)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(loads).toBe(3))
    await act(async () => { refreshed.resolve(response(ranked([{ ...jamie, relationship: 'OUTGOING_PENDING' }]))) })
    expect(within(await card('Jamie Lee')).getByRole('button', { name: 'Match requested' })).toBeDisabled()
  })

  it('keeps repeat sending unavailable through a failed refresh and retry', async () => {
    const retry = deferred<never>()
    let loads = 0
    mockApi(() => {
      loads++
      if (loads === 1) return Promise.resolve(response(ranked([jamie])))
      return loads === 2 ? Promise.reject(new Error('Refresh failed')) : retry.promise
    })
    vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    await sendRequest('Jamie Lee')
    expect(await screen.findByText('Refresh failed')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await waitFor(() => expect(loads).toBe(3))
    expect(screen.queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    await act(async () => { retry.resolve(response(ranked([{ ...jamie, relationship: 'OUTGOING_PENDING' }]))) })
    expect(within(await card('Jamie Lee')).getByRole('button', { name: 'Match requested' })).toBeDisabled()
  })

  it.each(['cancelled', 'declined'])('allows another request when fresh data says the previous request was %s', async () => {
    const refresh = deferred<never>()
    let loads = 0
    mockApi(() => ++loads === 1 ? Promise.resolve(response(ranked([jamie]))) : refresh.promise)
    vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    const row = await sendRequest('Jamie Lee')
    expect(within(row).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    await act(async () => { refresh.resolve(response(ranked([{ ...jamie, relationship: 'STRANGER' }]))) })
    expect(within(row).getByRole('button', { name: 'Send match request' })).toBeEnabled()
    expect(within(row).queryByRole('button', { name: 'Match requested' })).not.toBeInTheDocument()
  })

  it.each(['INCOMING_PENDING', 'CONNECTED'])('reconciles the local pending state with authoritative %s data', async relationship => {
    const refresh = deferred<never>()
    let loads = 0
    mockApi(() => ++loads === 1 ? Promise.resolve(response(ranked([jamie]))) : refresh.promise)
    vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    const row = await sendRequest('Jamie Lee')
    expect(within(row).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    await act(async () => { refresh.resolve(response(ranked([{ ...jamie, relationship }]))) })
    if (relationship === 'CONNECTED') expect(within(row).getByText('Connected')).toBeInTheDocument()
    else expect(within(row).getByRole('link', { name: 'Respond to their request' })).toBeInTheDocument()
    expect(within(row).queryByRole('button', { name: 'Match requested' })).not.toBeInTheDocument()
  })

  it('does not mark the recipient pending when sending fails', async () => {
    const get = mockApi()
    vi.spyOn(api, 'post').mockRejectedValue(new Error('Send failed'))
    openPage()
    const row = await card('Jamie Lee')
    fireEvent.click(within(row).getByRole('button', { name: 'Send match request' }))
    const dialog = await screen.findByRole('dialog', { name: 'Send match request to Jamie Lee' })
    await within(dialog).findByRole('option', { name: /IS442/ })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    expect(await within(dialog).findByText('Send failed')).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(within(row).getByRole('button', { name: 'Send match request' })).toBeEnabled()
    expect(within(row).queryByRole('button', { name: 'Match requested' })).not.toBeInTheDocument()
    expect(get.mock.calls.filter(([url]) => url === '/matches')).toHaveLength(1)
  })

  it('tracks multiple confirmed recipients until a fresh response arrives', async () => {
    const firstRefresh = deferred<never>(), secondRefresh = deferred<never>()
    let loads = 0
    mockApi(() => {
      loads++
      if (loads === 1) return Promise.resolve(response(ranked([jamie, alex])))
      return loads === 2 ? firstRefresh.promise : secondRefresh.promise
    })
    vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    const jamieRow = await sendRequest('Jamie Lee')
    const alexRow = await sendRequest('Alex Tan')
    expect(within(jamieRow).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    expect(within(alexRow).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    await act(async () => {
      secondRefresh.resolve(response(ranked([{ ...jamie, relationship: 'CONNECTED' }, { ...alex, relationship: 'OUTGOING_PENDING' }])))
    })
    expect(within(jamieRow).getByText('Connected')).toBeInTheDocument()
    expect(within(alexRow).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    await act(async () => { firstRefresh.resolve(response(ranked([jamie, alex]))) })
    expect(within(jamieRow).getByText('Connected')).toBeInTheDocument()
    expect(within(alexRow).getByRole('button', { name: 'Match requested' })).toBeDisabled()
  })

  it.each(['before confirmation', 'in the same batch as confirmation'])('keeps a second confirmed recipient pending when the first refresh arrives %s', async timing => {
    const firstRefresh = deferred<never>(), secondPost = deferred<never>(), secondRefresh = deferred<never>()
    let loads = 0
    mockApi(() => {
      loads++
      if (loads === 1) return Promise.resolve(response(ranked([jamie, alex])))
      return loads === 2 ? firstRefresh.promise : secondRefresh.promise
    })
    const post = vi.spyOn(api, 'post').mockResolvedValueOnce(response({ id: 9 })).mockReturnValueOnce(secondPost.promise)
    openPage()
    await sendRequest('Jamie Lee')
    const alexRow = await card('Alex Tan')
    fireEvent.click(within(alexRow).getByRole('button', { name: 'Send match request' }))
    const dialog = await screen.findByRole('dialog', { name: 'Send match request to Alex Tan' })
    await within(dialog).findByRole('option', { name: /IS442/ })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Send match request' }))
    expect(post).toHaveBeenCalledTimes(2)
    const beforeSecondSend = response(ranked([{ ...jamie, relationship: 'OUTGOING_PENDING' }, alex]))
    if (timing === 'before confirmation') {
      await act(async () => { firstRefresh.resolve(beforeSecondSend) })
      await act(async () => { secondPost.resolve(response({ id: 10 })) })
    } else {
      await act(async () => {
        firstRefresh.resolve(beforeSecondSend)
        // Queue the first GET's update without ending this React batch.
        await Promise.resolve()
        await Promise.resolve()
        secondPost.resolve(response({ id: 10 }))
      })
    }
    expect(await screen.findByText(/Match request sent to Alex Tan/)).toBeInTheDocument()
    await waitFor(() => expect(loads).toBe(3))
    expect(within(alexRow).getByRole('button', { name: 'Match requested' })).toBeDisabled()
    expect(within(alexRow).queryByRole('button', { name: 'Send match request' })).not.toBeInTheDocument()
    // The second send's own refresh remains authoritative if that request ended.
    await act(async () => {
      secondRefresh.resolve(response(ranked([{ ...jamie, relationship: 'OUTGOING_PENDING' }, { ...alex, relationship: 'STRANGER' }])))
    })
    expect(within(alexRow).getByRole('button', { name: 'Send match request' })).toBeEnabled()
  })

  it('uses fresh filtered relationships and ignores a delayed response for the old filters', async () => {
    const oldRefresh = deferred<never>(), filtered = deferred<never>()
    let loads = 0
    mockApi(() => {
      loads++
      if (loads === 1) return Promise.resolve(response(ranked([jamie])))
      return loads === 2 ? oldRefresh.promise : filtered.promise
    })
    vi.spyOn(api, 'post').mockResolvedValue(response({ id: 9 }))
    openPage()
    await sendRequest('Jamie Lee')
    await openFilters()
    fireEvent.change(screen.getByLabelText('Study mode'), { target: { value: 'ONLINE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    await waitFor(() => expect(loads).toBe(3))
    await act(async () => { filtered.resolve(response(ranked([{ ...jamie, relationship: 'INCOMING_PENDING' }]))) })
    const row = await card('Jamie Lee')
    expect(within(row).getByRole('link', { name: 'Respond to their request' })).toBeInTheDocument()
    await act(async () => { oldRefresh.resolve(response(ranked([{ ...jamie, relationship: 'OUTGOING_PENDING' }]))) })
    expect(within(row).getByRole('link', { name: 'Respond to their request' })).toBeInTheDocument()
    // A later authoritative STRANGER response supersedes every local confirmation.
    mockApi(() => Promise.resolve(response(ranked([jamie]))))
    act(() => { refreshResources() })
    expect(await within(row).findByRole('button', { name: 'Send match request' })).toBeEnabled()
  })

  it('requires a course or goal on first use and sends no unqualified request', async () => {
    const get = mockApi()
    openPage('/find-buddies')
    expect(await screen.findByText('Start your first matching search')).toBeInTheDocument()
    await screen.findByRole('option', { name: 'IS442 Object Oriented Programming' })
    expect(get.mock.calls.filter(([url]) => url === '/matches')).toHaveLength(0)
    expect(screen.getByLabelText('Sort matches')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    expect(screen.getAllByText('Choose a course or study goal before searching.')).toHaveLength(2)
    expect(get.mock.calls.filter(([url]) => url === '/matches')).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    await screen.findByRole('link', { name: 'Jamie Lee' })
    expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { studyGoal: 'EXAM_PREPARATION' } }))
  })

  it.each(['MOST_SHARED_HOURS', 'SAME_YEAR_FIRST', 'BEST_MATCH'])('passes the %s sort to the server and preserves its order', async sort => {
    const get = mockApi(() => Promise.resolve(response({ ...ranked([alex, jamie]), sort })))
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    fireEvent.change(screen.getByLabelText('Sort matches'), { target: { value: sort } })
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4, sort } })))
    expect(names()).toEqual(['Alex Tan Year 1', 'Jamie Lee Year 2'])
    if (sort === 'BEST_MATCH') expect(screen.getByText(/Ranked by Balanced matching/)).toBeInTheDocument()
    else expect(screen.getByText(new RegExp(`Sorted by ${sort === 'MOST_SHARED_HOURS' ? 'Most shared hours' : 'Same year first'}\\. Balanced matching breaks ties`))).toBeInTheDocument()
  })

  it('shows exact server compatibility, explanation, relevant course and disabled bars', async () => {
    const match = { ...jamie, totalScore: 0.747, explanation: 'Course aligned; study goals disabled.',
      breakdown: jamie.breakdown.map(item => ({ ...item, enabled: item.criterion !== 'STUDY_GOAL' })) }
    mockApi(() => Promise.resolve(response(ranked([match]))))
    openPage()
    const row = await card('Jamie Lee')
    expect(within(row).getByText('74.7% compatibility')).toBeInTheDocument()
    expect(within(row).getByText('A comparison of study preferences, not a probability of success.')).toBeInTheDocument()
    expect(within(row).getByText('Course aligned; study goals disabled.')).toBeInTheDocument()
    expect(within(row).getByText('Relevant course: IS442 Object Oriented Programming')).toBeInTheDocument()
    expect(within(row).getByRole('meter', { name: 'Study goal score' })).toHaveAttribute('aria-valuetext', 'Disabled by administrator')
    expect(screen.getByText(/Configuration version 2/)).toBeInTheDocument()
  })

  it('explains a bounded result and resets without requesting a context-free search', async () => {
    const get = mockApi(() => Promise.resolve(response({ ...ranked([jamie]), totalMatches: 35 })))
    openPage()
    expect(await screen.findByText('Showing 1 of 35 matching students. Narrow your filters to see others.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Remove IS442 Object Oriented Programming filter' }))
    expect(await screen.findByText('Start your first matching search')).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Filter study buddies' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Jamie Lee' })).not.toBeInTheDocument()
    expect(get.mock.calls.filter(([url]) => url === '/matches')).toHaveLength(1)
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    await screen.findByRole('link', { name: 'Jamie Lee' })
    await openFilters()
    fireEvent.click(screen.getByRole('button', { name: 'Reset search' }))
    expect(await screen.findByText('Start your first matching search')).toBeInTheDocument()
    expect(get.mock.calls.filter(([url]) => url === '/matches')).toHaveLength(2)
  })

  it('applies every optional filter and preserves context when a preference chip is removed', async () => {
    const get = mockApi()
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    await openFilters()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.change(screen.getByLabelText('Study mode'), { target: { value: 'ONLINE' } })
    fireEvent.change(screen.getByLabelText('Group size'), { target: { value: 'ONE_TO_ONE' } })
    fireEvent.change(screen.getByLabelText('Shared free time'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Minimum match quality'), { target: { value: 'STRONG' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find study buddies' }))
    const params = { courseId: 4, studyGoal: 'EXAM_PREPARATION', studyMode: 'ONLINE', groupSize: 'ONE_TO_ONE', minSharedHours: 2, minQuality: 'STRONG' }
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params })))
    fireEvent.click(screen.getByRole('button', { name: 'Remove Strong only filter' }))
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4, studyGoal: 'EXAM_PREPARATION', studyMode: 'ONLINE', groupSize: 'ONE_TO_ONE', minSharedHours: 2 } })))
  })


  it('restores selected filters and sort from a shared or return link', async () => {
    const get = mockApi()
    openPage('/find-buddies?courseId=3&studyGoal=EXAM_PREPARATION&studyMode=ONLINE&groupSize=SMALL_GROUP&minSharedHours=4&minQuality=GOOD&sort=SAME_YEAR_FIRST')
    await screen.findByRole('link', { name: 'Jamie Lee' })
    expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 3, studyGoal: 'EXAM_PREPARATION', studyMode: 'ONLINE', groupSize: 'SMALL_GROUP', minSharedHours: 4, minQuality: 'GOOD', sort: 'SAME_YEAR_FIRST' } }))
    expect(screen.getByLabelText('Sort matches')).toHaveValue('SAME_YEAR_FIRST')
    await openFilters()
    expect(screen.getByLabelText('Course')).toHaveValue('3')
    expect(screen.getByLabelText('Shared free time')).toHaveValue('4')
  })

})
