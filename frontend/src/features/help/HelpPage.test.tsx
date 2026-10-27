import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { createMemoryRouter, matchRoutes, RouterProvider } from 'react-router-dom'
import { api, refreshResources } from '../../shared/api/client'
import { appRoutes } from '../../routes'
import { adminAccount, deferred, renderPage, response, studentAccount } from '../../test/renderApp'
import { AuthContext } from '../../shared/auth/context'
import type { Account } from '../../shared/auth/context'
import { FAQS } from './helpContent'
import HelpPage from './HelpPage'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const profile = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2,
  contactNumber: 'Synthetic contact 1', coursesTaken: [course], targetCourse: null, preferredStudyMode: 'ONLINE',
  groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: []
}
const summary = { activeConnections: 0, pendingIncoming: 0, pendingOutgoing: 1, acceptedGroups: 0 }
const group = { id: 5, name: 'Revision crew', active: true, viewer: { member: true, leader: false } }
const data: Record<string, unknown> = { '/profile/me': profile, '/students/me/summary': summary, '/groups/mine': [], '/sessions/mine': [] }
function failure() {
  return new AxiosError('Server Error', 'ERR_BAD_RESPONSE', undefined, undefined, {
    config: { headers: new AxiosHeaders() }, status: 403, statusText: 'Forbidden', headers: {}, data: { message: 'Access is unavailable.' }
  })
}
function serve(failing?: string) {
  return vi.spyOn(api, 'get').mockImplementation(async path => {
    if (path === failing) throw failure()
    return response(data[path as string])
  })
}
function startHere() { return screen.getByRole('region', { name: 'Start here' }) }
function steps() { return within(within(startHere()).getByRole('list')).getAllByRole('listitem') }
function renderProtected(account: Account | null, path = '/help') {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  const tree = (current: Account | null) => <AuthContext.Provider value={{ account: current, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }}><RouterProvider router={router} /></AuthContext.Provider>
  return { ...render(tree(account)), router, tree }
}
afterEach(() => vi.restoreAllMocks())

describe('help page', () => {
  it('links every guide action to a merged student route and makes no provider or room mutation', async () => {
    const get = serve()
    const post = vi.spyOn(api, 'post')
    renderPage(<HelpPage />, '/help', '/help')
    await screen.findByRole('img', { name: '2 of 5 current checks confirmed' })
    expect(get.mock.calls.map(([path]) => path).sort()).toEqual(['/groups/mine', '/profile/me', '/sessions/mine', '/students/me/summary'])
    for (const link of screen.getAllByRole('link')) {
      const href = link.getAttribute('href')!
      const matched = matchRoutes(appRoutes, href.split(/[?#]/)[0] || '/help') ?? []
      expect(matched.at(-1)?.route.path, href).not.toBe('*')
      expect(matched.length, href).toBeGreaterThan(0)
    }
    expect(post).not.toHaveBeenCalled()
    expect(startHere()).not.toHaveTextContent(profile.contactNumber)
  })

  it('shows one next action and separates saved records, missing records and untracked actions', async () => {
    const get = serve()
    renderPage(<HelpPage />, '/help', '/help')
    expect(within(startHere()).getByRole('status')).toHaveTextContent('Checking current records')
    expect(steps()[2]).toHaveTextContent('Search history is not tracked')
    expect(steps()[5]).toHaveTextContent('Room visits are not tracked')
    await screen.findByRole('img', { name: '2 of 5 current checks confirmed' })
    const next = within(startHere()).getByRole('region', { name: 'Add your weekly availability' })
    expect(within(next).getAllByRole('link')).toHaveLength(1)
    expect(within(next).getByRole('link')).toHaveAttribute('href', '/students/1/edit?tab=availability')
    expect(steps()[0]).toHaveTextContent('Saved in your profile')
    expect(steps()[1]).toHaveTextContent('Still needed in your profile')
    expect(steps()[3]).toHaveTextContent('A sent request or active connection exists')
    expect(steps()[4]).toHaveTextContent('No current open-group membership')
    expect(steps()[6]).toHaveTextContent('No ongoing or upcoming session')
    expect(within(startHere()).queryByRole('status')).not.toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(4)
  })

  it('keeps known progress and usable links when a check fails, then retries only that source', async () => {
    const get = serve('/students/me/summary')
    renderPage(<HelpPage />, '/help', '/help')
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Could not check: requests and connections')
    expect(steps()[3]).toHaveTextContent('Could not check')
    expect(steps()[0]).toHaveTextContent('Saved in your profile')
    expect(within(startHere()).getAllByRole('link')).toHaveLength(8)
    get.mockClear()
    get.mockImplementation(async path => response(data[path as string]))
    fireEvent.click(within(alert).getByRole('button', { name: 'Check progress again' }))
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())
    expect(get.mock.calls.map(([path]) => path)).toEqual(['/students/me/summary'])
    expect(steps()[3]).toHaveTextContent('A sent request or active connection exists')
  })

  it('does not choose an activity recommendation while a required source is unknown or denied', async () => {
    const pending = deferred<never>()
    const get = serve()
    get.mockImplementation(path => path === '/profile/me' ? pending.promise : Promise.resolve(response(data[path as string])))
    renderPage(<HelpPage />, '/help', '/help')
    const next = screen.getByRole('region', { name: 'Your next action' })
    expect(next).toHaveTextContent('Checking your profile')
    expect(within(next).queryByRole('link')).not.toBeInTheDocument()
    await act(async () => { pending.reject(failure()) })
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not check: profile')
    expect(next).toHaveTextContent('Your profile could not be checked')
    expect(steps()[0]).toHaveTextContent('Could not check')
    expect(steps()[0]).not.toHaveTextContent('Still needed')
  })

  it('removes a room shortcut immediately when private resources are invalidated and access fails', async () => {
    const get = serve()
    get.mockImplementation(async path => response(path === '/groups/mine' ? [group] : data[path as string]))
    renderPage(<HelpPage />, '/help', '/help')
    expect(await screen.findByRole('link', { name: 'Preview group study room' })).toHaveAttribute('href', '/groups/5/room')
    const denied = deferred<never>()
    get.mockImplementation(path => path === '/groups/mine' ? denied.promise : Promise.resolve(response(data[path as string])))
    act(() => refreshResources(true))
    expect(screen.queryByRole('link', { name: 'Preview group study room' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View your groups' })).toHaveAttribute('href', '/groups?view=mine')
    await act(async () => { denied.reject(failure()) })
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not check: groups')
    expect(steps()[4]).toHaveTextContent('Could not check')
  })

  it('opens a contextual topic at its focused heading and explains the merged product', async () => {
    serve()
    renderProtected(studentAccount, '/help#help-ai')
    await waitFor(() => expect(screen.getByRole('heading', { name: 'AI assistance and its limits' })).toHaveFocus())
    for (const name of ['Matching', 'AI assistance and its limits', 'Requests and privacy', 'Study groups', 'Study rooms', 'Study sessions', 'Notifications']) {
      expect(screen.getByRole('article', { name })).toBeInTheDocument()
    }
    expect(screen.getByRole('article', { name: 'AI assistance and its limits' })).toHaveTextContent('AI wording is labelled and may be inaccurate')
    expect(screen.getByRole('article', { name: 'Study rooms' })).toHaveTextContent('Opening a group or session link does not automatically add you to the private room')
    expect(screen.getByRole('article', { name: 'Study sessions' })).toHaveTextContent('downloads do not sync or prove attendance')
    fireEvent.click(screen.getByRole('link', { name: 'Sessions and calendar' }))
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Study sessions' })).toHaveFocus())
  })

  it.each([null, adminAccount])('keeps Help student-only for account %s', async account => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const { router } = renderProtected(account)
    await waitFor(() => expect(router.state.location.pathname).toBe(account ? '/admin/users' : '/login'))
    expect(screen.queryByRole('heading', { name: 'Help and getting started' })).not.toBeInTheDocument()
    expect(get.mock.calls.some(([path]) => Object.hasOwn(data, path))).toBe(false)
  })

  it('clears a previous account’s links and ignores its late profile response after an account change', async () => {
    const pending = deferred<never>()
    const get = serve()
    get.mockImplementation(path => path === '/profile/me' ? pending.promise : Promise.resolve(response(path === '/groups/mine' ? [group] : data[path as string])))
    const view = renderProtected(studentAccount)
    await screen.findByRole('link', { name: 'Preview group study room' })
    get.mockImplementation(async path => response(path === '/profile/me' ? { ...profile, id: 2, availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }] }
      : path === '/students/me/summary' ? { ...summary, pendingOutgoing: 0 } : data[path as string]))
    view.rerender(view.tree({ ...studentAccount, id: 2, name: 'Second student' }))
    expect(screen.queryByRole('link', { name: 'Preview group study room' })).not.toBeInTheDocument()
    await screen.findByRole('region', { name: 'Find a study buddy' })
    await act(async () => { pending.resolve(response(profile)) })
    expect(screen.getByRole('region', { name: 'Find a study buddy' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit weekly availability' })).toHaveAttribute('href', '/students/2/edit?tab=availability')
    expect(screen.queryByRole('link', { name: 'Continue profile setup' })).not.toBeInTheDocument()
  })

  it('filters questions and offers a way back from no results', () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    const faq = screen.getByRole('region', { name: 'Frequently asked questions' })
    expect(within(faq).getAllByRole('button', { expanded: false })).toHaveLength(FAQS.length)
    const search = screen.getByLabelText('Search questions')
    fireEvent.change(search, { target: { value: 'CALENDAR EXPORT' } })
    expect(within(faq).getAllByRole('button', { expanded: false }).map(button => button.textContent)).toEqual(['Does the calendar export stay up to date?'])
    expect(screen.getByText(`1 of ${FAQS.length} questions shown`)).toBeInTheDocument()
    fireEvent.change(search, { target: { value: 'quantum physics' } })
    expect(screen.getByRole('heading', { name: 'No answers match “quantum physics”' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(search).toHaveValue('')
    expect(search).toHaveFocus()
    expect(within(faq).getAllByRole('button', { expanded: false })).toHaveLength(FAQS.length)
  })

  it('opens and closes an answer with aria-expanded', () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    const toggle = screen.getByRole('button', { name: 'When can other students see my contact number?' })
    const answer = document.getElementById(toggle.getAttribute('aria-controls')!)!
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(answer).not.toBeVisible()
    fireEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(answer).toBeVisible()
    expect(answer).toHaveTextContent('information someone already saved cannot be recalled')
    fireEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
  })
})
