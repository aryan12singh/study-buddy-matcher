import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import type { StudySession } from '../sessions/api'
import SessionRoadmapPanel from './SessionRoadmapPanel'
import type { StudyPlan } from './api'

const course = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const session: StudySession = {
  id: 11, version: 3, scope: 'GROUP', groupId: 5, connectionId: null, title: 'Exam revision',
  startLocal: '2026-11-02T10:00', endLocal: '2026-11-02T11:00', startAt: '2026-11-02T02:00:00Z', endAt: '2026-11-02T03:00:00Z',
  timeZone: 'Asia/Singapore', occurrenceCount: 2, agenda: null, location: null, roomLink: null, status: 'SCHEDULED', canManage: true,
  occurrences: [{ startAt: '2026-11-02T02:00:00Z', endAt: '2026-11-02T03:00:00Z' }, { startAt: '2026-11-09T02:00:00Z', endAt: '2026-11-09T03:00:00Z' }],
  nextOccurrence: { startAt: '2026-11-02T02:00:00Z', endAt: '2026-11-02T03:00:00Z' }, updatedAt: '2026-10-20T00:00:00Z', cancelledAt: null,
}
const plan: StudyPlan = {
  id: 30, scope: 'SESSION', course, sessionId: 11, firstBlockDate: '2026-11-02', blockCount: 2, minutesPerBlock: 60,
  examDate: null, studyGoal: null, sourceKind: 'PASTE', sourceFilenames: [], sourceTopics: ['Polymorphism', 'Interfaces'], canManage: true,
  entries: [
    { id: 1, sequence: 1, scheduledDate: '2026-11-02', title: 'Polymorphism', plannedMinutes: 45, tasks: ['Walk through slide 12'], completed: false },
    { id: 2, sequence: 2, scheduledDate: '2026-11-09', title: 'Interfaces', plannedMinutes: 50, tasks: [], completed: false },
  ],
  createdAt: '2026-10-25T00:00:00Z', updatedAt: '2026-10-25T00:00:00Z',
}
function failure(status: number, message: string, code?: string) {
  return new AxiosError(message, 'ERR_BAD_RESPONSE', undefined, undefined,
    { config: { headers: new AxiosHeaders() }, status, statusText: 'Error', headers: {}, data: { message, code } })
}
/** Serves the session's roadmap (or 404 for none) and the course list. */
function serve(current: () => StudyPlan | null) {
  return vi.spyOn(api, 'get').mockImplementation(async path => {
    if (path === '/courses') return response([course])
    const found = current()
    if (!found) throw failure(404, 'Study session 11 has no study plan', 'NOT_FOUND')
    return response(found)
  })
}
function open(shown: StudySession = session) {
  return renderPage(<SessionRoadmapPanel session={shown} />)
}
function pasteTopics(text = 'Polymorphism\nInterfaces') {
  fireEvent.click(screen.getByLabelText('Paste topics'))
  fireEvent.change(screen.getByLabelText('Topics to cover'), { target: { value: text } })
}
afterEach(() => { vi.restoreAllMocks() })

describe('session roadmap panel', () => {
  it('shows members each meeting’s topics without letting them change anything', async () => {
    serve(() => ({ ...plan, canManage: false }))
    open({ ...session, canManage: false })
    const meetings = within(await screen.findByRole('list', { name: 'Roadmap' })).getAllByRole('heading', { level: 3 })
    expect(meetings.map(meeting => meeting.textContent)).toEqual([
      'Meeting 1 · on 2 Nov 2026 · 45 min of 1 h planned', 'Meeting 2 · on 9 Nov 2026 · 50 min of 1 h planned',
    ])
    expect(screen.getByRole('checkbox', { name: /Polymorphism/ })).toBeDisabled()
    expect(screen.getByText('Roadmap written by Gemini; it may be inaccurate.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Replace roadmap' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete roadmap' })).not.toBeInTheDocument()
  })

  it('tells a member with no roadmap who can create one', async () => {
    serve(() => null)
    open({ ...session, canManage: false })
    expect(await screen.findByText('The group leader can create one for everyone.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Plan these meetings' })).not.toBeInTheDocument()
  })

  it('offers no planning for a cancelled session', async () => {
    serve(() => null)
    open({ ...session, status: 'CANCELLED', nextOccurrence: null })
    expect(await screen.findByText('This session has no upcoming meetings to plan.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Plan these meetings' })).not.toBeInTheDocument()
  })

  it('lets the leader plan a group session from pasted topics, for the group’s course', async () => {
    let current: StudyPlan | null = null
    serve(() => current)
    const post = vi.spyOn(api, 'post').mockImplementation(async () => { current = plan; return response(plan) })
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Plan these meetings' }))
    expect(screen.queryByLabelText('Course')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Create session roadmap' }))
    expect(await screen.findByText('Choose at least one file.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()

    pasteTopics()
    fireEvent.click(screen.getByRole('button', { name: 'Create session roadmap' }))
    expect(await screen.findByText('Session roadmap saved. Everyone in the session can see it.')).toBeInTheDocument()
    expect(await screen.findByRole('list', { name: 'Roadmap' })).toBeInTheDocument()
    const [path, body] = post.mock.calls[0] as [string, FormData]
    expect(path).toBe('/sessions/11/study-plan')
    expect(body.get('topicsText')).toBe('Polymorphism\nInterfaces')
    expect(body.has('courseId')).toBe(false)
  })

  it('asks a buddy which course the session is for', async () => {
    serve(() => null)
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(plan))
    open({ ...session, scope: 'BUDDY', groupId: null, connectionId: 8 })
    fireEvent.click(await screen.findByRole('button', { name: 'Plan these meetings' }))
    expect(await screen.findByLabelText('Course')).toHaveValue('')
    pasteTopics()
    fireEvent.click(screen.getByRole('button', { name: 'Create session roadmap' }))
    expect(await screen.findByText('Choose the course this session is for.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create session roadmap' }))
    await waitFor(() => expect(post).toHaveBeenCalled())
    expect((post.mock.calls[0][1] as FormData).get('courseId')).toBe('4')
  })

  it('replaces the roadmap only after confirming', async () => {
    serve(() => plan)
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...plan, id: 31 }))
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Replace roadmap' }))
    pasteTopics('Generics')
    fireEvent.click(screen.getByRole('button', { name: 'Replace roadmap' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Replace roadmap' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Replace roadmap' }))
    await waitFor(() => expect(post).toHaveBeenCalledOnce())
    expect((post.mock.calls[0][1] as FormData).get('topicsText')).toBe('Generics')
  })

  it('says plainly when Gemini could not draft the roadmap', async () => {
    serve(() => null)
    vi.spyOn(api, 'post').mockRejectedValue(failure(503, 'The AI assistant is unavailable.', 'AI_UNAVAILABLE'))
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Plan these meetings' }))
    pasteTopics()
    fireEvent.click(screen.getByRole('button', { name: 'Create session roadmap' }))
    expect(await screen.findByText(/Gemini could not draft the roadmap just now, so nothing changed/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create session roadmap' })).toBeEnabled()
  })

  it('warns when the session changed after the roadmap was made', async () => {
    serve(() => plan)
    open({ ...session, updatedAt: '2026-10-26T00:00:00Z' })
    expect(await screen.findByText(/This session was changed after the roadmap was made/)).toBeInTheDocument()
  })

  it('ticks a topic off and deletes the roadmap after confirming', async () => {
    let current: StudyPlan | null = plan
    serve(() => current)
    const put = vi.spyOn(api, 'put').mockResolvedValue(response({
      ...plan, entries: plan.entries.map(entry => entry.id === 1 ? { ...entry, completed: true } : entry),
    }))
    const remove = vi.spyOn(api, 'delete').mockImplementation(async () => { current = null; return response(undefined) })
    open()
    fireEvent.click(await screen.findByRole('checkbox', { name: /Polymorphism/ }))
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Polymorphism/ })).toBeChecked())
    expect(put).toHaveBeenCalledWith('/study-plans/30/entries/1/progress', { completed: true })

    fireEvent.click(screen.getByRole('button', { name: 'Delete roadmap' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete roadmap' }))
    expect(await screen.findByText('No roadmap for this session yet')).toBeInTheDocument()
    expect(remove).toHaveBeenCalledWith('/study-plans/30')
  })
})
