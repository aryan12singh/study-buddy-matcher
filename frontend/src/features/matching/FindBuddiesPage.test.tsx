import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api, refreshResources } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import FindBuddiesPage from './FindBuddiesPage'

const is442 = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const is216 = { id: 3, code: 'IS216', name: 'Web Application Development' }
const myProfile = { id: 1, coursesTaken: [is442, is216], targetCourse: is442 }
const breakdown = (course: number, availability: number) => [
  { criterion: 'COURSE', score: course }, { criterion: 'AVAILABILITY', score: availability },
  { criterion: 'STUDY_MODE', score: 1 }, { criterion: 'STUDY_GOAL', score: 0 }, { criterion: 'GROUP_SIZE', score: 0.5 }
]
const jamie = {
  studentId: 2, name: 'Jamie Lee', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2, studyMode: 'ONLINE',
  studyGoals: ['EXAM_PREPARATION'], quality: 'STRONG', breakdown: breakdown(1, 0.5), sharedHoursPerWeek: 2,
  sharedSlots: [{ dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '11:00:00' }], relationship: 'STRANGER'
}
const alex = {
  studentId: 5, name: 'Alex Tan', school: 'SOE', programme: 'Economics', yearOfStudy: 1, studyMode: null,
  studyGoals: [], quality: 'FAIR', breakdown: breakdown(0, 0), sharedHoursPerWeek: 0, sharedSlots: [], relationship: 'STRANGER'
}
afterEach(() => { vi.restoreAllMocks() })

/** A search response ranked by the given strategy. */
function ranked(matches: object[], strategy = 'BALANCED') {
  return { strategy, matches }
}

/** Answers the student's profile and match searches by URL; a custom match response or profile can be supplied. */
function mockApi(matches: () => Promise<unknown> = () => Promise.resolve(response(ranked([jamie, alex]))), profile: object = myProfile) {
  return vi.spyOn(api, 'get').mockImplementation(url => (url === '/matches' ? matches()
    : Promise.resolve(response(url === '/courses' ? [is442, is216] : profile))) as never)
}

function openPage() {
  return renderPage(<FindBuddiesPage />, '/find-buddies', '/find-buddies')
}

function names() {
  return within(screen.getByLabelText('Matching students')).getAllByRole('heading', { level: 3 }).map(heading => heading.textContent)
}

async function openFilters() {
  fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
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
  it('lists every student best match first without any filters', async () => {
    const get = mockApi()
    openPage()
    expect(await screen.findByRole('link', { name: 'Jamie Lee' })).toHaveAttribute('href', '/students/2')
    expect(screen.getByRole('heading', { name: 'Find buddies' })).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: {} }))
    expect(screen.getByText('2 students to study with')).toBeInTheDocument()
    expect(names()).toEqual(['Jamie Lee Year 2', 'Alex Tan Year 1'])
    expect(screen.queryByRole('form', { name: 'Filter study buddies' })).not.toBeInTheDocument()
  })

  it('names the ranking strategy the admin chose', async () => {
    mockApi(() => Promise.resolve(response(ranked([jamie], 'AVAILABILITY_FIRST'))))
    openPage()
    expect(await screen.findByText(/Ranked by Availability first matching\. Ranks by shared free time/)).toBeInTheDocument()
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
    expect(router.state.location.state).toEqual({ returnTo: { path: '/find-buddies', label: 'Back to find buddies' } })
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
    expect(screen.getByLabelText('Course')).toHaveValue('')
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Study mode'), { target: { value: 'ONLINE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    await waitFor(() => expect(get).toHaveBeenCalledWith('/matches', expect.objectContaining({ params: { courseId: 4, studyMode: 'ONLINE' } })))
    expect(await screen.findByText('2 students match your filters')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Filters · 2' })).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(screen.getByRole('button', { name: 'Remove IS442 Object Oriented Programming filter' }))
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/matches', expect.objectContaining({ params: { studyMode: 'ONLINE' } })))
    expect(screen.getByRole('button', { name: 'Filters · 1' })).toBeInTheDocument()
  })

  it('points a student with no courses to their profile', async () => {
    mockApi(undefined, { id: 1, coursesTaken: [], targetCourse: null })
    openPage()
    await screen.findByRole('link', { name: 'Jamie Lee' })
    fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
    expect(await screen.findByRole('link', { name: 'Add courses to your profile' })).toHaveAttribute('href', '/students/1/edit')
    expect(screen.getByLabelText('Course')).toBeDisabled()
  })

  it('explains an empty result with and without filters', async () => {
    mockApi(() => Promise.resolve(response(ranked([]))))
    openPage()
    expect(await screen.findByText('No other students to match yet')).toBeInTheDocument()
    await openFilters()
    fireEvent.change(screen.getByLabelText('Study goal'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    expect(await screen.findByText('No students match these filters')).toBeInTheDocument()
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
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
    await screen.findByText('2 students match your filters')
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
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
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
})
