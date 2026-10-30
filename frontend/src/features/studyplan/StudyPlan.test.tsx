import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import StudyPlanPage from './StudyPlanPage'
import CreateStudyPlanPage from './CreateStudyPlanPage'
import StudyPlanDetailPage from './StudyPlanDetailPage'
import type { StudyPlan, StudyPlanSummary } from './api'
import { initialStudyPlanForm, validateStudyPlanForm } from './studyPlanForm'
import type { StudyPlanForm } from './studyPlanForm'

const course = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const summary: StudyPlanSummary = {
  id: 7, scope: 'PERSONAL', course, firstBlockDate: '2026-10-28', blockCount: 4, examDate: '2026-11-20',
  studyGoal: 'EXAM_PREPARATION', entryCount: 8, completedCount: 3, createdAt: '2026-10-28T01:00:00Z',
}
const plan: StudyPlan = {
  id: 7, scope: 'PERSONAL', course, sessionId: null, firstBlockDate: '2026-10-28', blockCount: 4, minutesPerBlock: 360,
  examDate: '2026-11-20', studyGoal: null, sourceKind: 'PASTE', sourceFilenames: [], sourceTopics: ['Encapsulation'],
  canManage: true, entries: [], createdAt: '2026-10-28T01:00:00Z', updatedAt: '2026-10-28T01:00:00Z',
}
function failure(status: number, message: string, code?: string) {
  return new AxiosError(message, 'ERR_BAD_RESPONSE', undefined, undefined,
    { config: { headers: new AxiosHeaders() }, status, statusText: 'Error', headers: {}, data: { message, code } })
}
const valid: StudyPlanForm = {
  courseId: '4', hoursPerWeek: '6', examDate: '2026-11-20', studyGoal: '', materialKind: 'topics', files: [], topicsText: 'Encapsulation',
}
const pdf = () => new File(['%PDF-1.4'], 'week1.pdf', { type: 'application/pdf' })
// 9am on 28 October in Singapore.
beforeEach(() => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-28T01:00:00Z')) })
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })

describe('study plan list', () => {
  it('lists each plan with its course, progress, length and exam date', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([summary, { ...summary, id: 8, examDate: null, blockCount: 1, studyGoal: null }]))
    renderPage(<StudyPlanPage />)
    const list = await screen.findByRole('list', { name: 'Study plans' })
    const rows = within(list).getAllByRole('listitem')
    expect(rows).toHaveLength(2)
    expect(within(rows[0]).getByRole('link', { name: 'IS442 — Object Oriented Programming' })).toHaveAttribute('href', '/study-plan/7')
    expect(within(rows[0]).getByText('3 of 8 topics done')).toBeInTheDocument()
    expect(within(rows[0]).getByText('4 weeks')).toBeInTheDocument()
    expect(within(rows[0]).getByText('Exam preparation')).toBeInTheDocument()
    expect(within(rows[0]).getByText('Exam on 20 Nov 2026')).toBeInTheDocument()
    expect(within(rows[1]).getByText('This week only, no exam date')).toBeInTheDocument()
    expect(within(rows[1]).getByText('1 week')).toBeInTheDocument()
  })

  it('explains what a study plan is when the student has none', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    renderPage(<StudyPlanPage />)
    expect(await screen.findByRole('heading', { name: 'No study plans yet' })).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Study plans' })).not.toBeInTheDocument()
  })

  it('shows the error and loads again on retry', async () => {
    const get = vi.spyOn(api, 'get').mockRejectedValueOnce(failure(500, 'Study plans are unavailable.')).mockResolvedValue(response([summary]))
    renderPage(<StudyPlanPage />)
    expect(await screen.findByText('Study plans are unavailable.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('list', { name: 'Study plans' })).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/study-plans', expect.anything())
  })

  it('sends a link with plan details from Pixel on to the create form', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const { router } = renderPage(<StudyPlanPage />, '/study-plan?courseId=4&hoursPerWeek=6', '/study-plan')
    await waitFor(() => expect(router.state.location.pathname).toBe('/study-plan/new'))
    expect(router.state.location.search).toBe('?courseId=4&hoursPerWeek=6')
    expect(get).not.toHaveBeenCalledWith('/study-plans', expect.anything())
  })
})

describe('study plan form checks', () => {
  const today = '2026-10-28'

  it('accepts a complete form with pasted topics or a file', () => {
    expect(validateStudyPlanForm(valid, today)).toEqual({})
    expect(validateStudyPlanForm({ ...valid, examDate: '' }, today)).toEqual({})
    expect(validateStudyPlanForm({ ...valid, materialKind: 'files', files: [pdf()], topicsText: '' }, today)).toEqual({})
  })

  it('says what is wrong next to each field', () => {
    const errors = validateStudyPlanForm({ ...valid, courseId: '', hoursPerWeek: '1.5', topicsText: '  ' }, today)
    expect(Object.keys(errors).sort()).toEqual(['courseId', 'hoursPerWeek', 'topicsText'])
    expect(validateStudyPlanForm({ ...valid, hoursPerWeek: '41' }, today).hoursPerWeek).toBe('Enter whole hours from 1 to 40.')
    expect(validateStudyPlanForm({ ...valid, hoursPerWeek: '0' }, today).hoursPerWeek).toBeDefined()
    expect(validateStudyPlanForm({ ...valid, topicsText: 'x'.repeat(4001) }, today).topicsText).toMatch(/4,000/)
  })

  it('takes an exam from tomorrow to 16 weeks ahead', () => {
    expect(validateStudyPlanForm({ ...valid, examDate: today }, today).examDate).toBe('Choose an exam date after today.')
    expect(validateStudyPlanForm({ ...valid, examDate: '2026-10-29' }, today).examDate).toBeUndefined()
    expect(validateStudyPlanForm({ ...valid, examDate: '2027-02-17' }, today).examDate).toBeUndefined()
    expect(validateStudyPlanForm({ ...valid, examDate: '2027-02-18' }, today).examDate).toMatch(/16 weeks/)
  })

  it('checks uploaded files before sending them', () => {
    expect(validateStudyPlanForm({ ...valid, materialKind: 'files' }, today).files).toBe('Choose at least one file.')
    const slides = new File(['x'], 'slides.pptx')
    expect(validateStudyPlanForm({ ...valid, materialKind: 'files', files: [slides] }, today).files).toMatch(/not a PDF/)
  })

  it('fills in only the details from the link that are usable', () => {
    const filled = initialStudyPlanForm(new URLSearchParams('courseId=4&examDate=2026-11-20&hoursPerWeek=6'), [course])
    expect(filled).toMatchObject({ courseId: '4', examDate: '2026-11-20', hoursPerWeek: '6' })
    const unusable = initialStudyPlanForm(new URLSearchParams('courseId=99&examDate=soon&hoursPerWeek=lots'), [course])
    expect(unusable).toMatchObject({ courseId: '', examDate: '', hoursPerWeek: '' })
  })
})

describe('creating a study plan', () => {
  function open(path = '/study-plan/new') {
    vi.spyOn(api, 'get').mockResolvedValue(response([course]))
    return renderPage(<CreateStudyPlanPage />, path, '/study-plan/new')
  }

  it('starts from the details in the link', async () => {
    open('/study-plan/new?courseId=4&examDate=2026-11-20&hoursPerWeek=6')
    expect(await screen.findByLabelText('Course')).toHaveValue('4')
    expect(screen.getByLabelText('Hours per week')).toHaveValue(6)
    expect(screen.getByLabelText('Exam date (optional)')).toHaveValue('2026-11-20')
  })

  it('shows what is missing and sends nothing', async () => {
    const post = vi.spyOn(api, 'post')
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Create study plan' }))
    expect(await screen.findByText('Choose a course.')).toBeInTheDocument()
    expect(screen.getByText('Choose at least one file.')).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
  })

  it('uploads the chosen files with the details and opens the new plan', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(plan))
    const { router } = open()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Hours per week'), { target: { value: '6' } })
    fireEvent.change(screen.getByLabelText('Study goal (optional)'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.change(screen.getByLabelText('Lecture slides or notes'), { target: { files: [pdf()] } })
    expect(within(screen.getByRole('list', { name: 'Chosen files' })).getByText('week1.pdf')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/study-plan/7'))
    const [path, body] = post.mock.calls[0] as [string, FormData]
    expect(path).toBe('/study-plans')
    expect(body.get('courseId')).toBe('4')
    expect(body.get('hoursPerWeek')).toBe('6')
    expect(body.get('studyGoal')).toBe('EXAM_PREPARATION')
    expect(body.has('examDate')).toBe(false)
    expect(body.getAll('files').map(file => (file as File).name)).toEqual(['week1.pdf'])
    expect(body.has('topicsText')).toBe(false)
    expect(router.state.location.state).toMatchObject({ notice: 'Study plan created.' })
  })

  it('sends pasted topics instead of files', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(plan))
    open('/study-plan/new?courseId=4&hoursPerWeek=6')
    fireEvent.click(await screen.findByLabelText('Paste topics'))
    fireEvent.change(screen.getByLabelText('Topics to cover'), { target: { value: ' Encapsulation\nInheritance ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    await waitFor(() => expect(post).toHaveBeenCalled())
    const body = post.mock.calls[0][1] as FormData
    expect(body.get('topicsText')).toBe('Encapsulation\nInheritance')
    expect(body.has('files')).toBe(false)
  })

  it('says plainly when the AI helper could not draft the plan', async () => {
    vi.spyOn(api, 'post').mockRejectedValue(failure(503, 'The AI assistant is unavailable.', 'AI_UNAVAILABLE'))
    open('/study-plan/new?courseId=4&hoursPerWeek=6')
    fireEvent.click(await screen.findByLabelText('Paste topics'))
    fireEvent.change(screen.getByLabelText('Topics to cover'), { target: { value: 'Encapsulation' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    expect(await screen.findByText(/The AI helper isn't available right now — it may be switched off on this server or busy. No plan was saved/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create study plan' })).toBeEnabled()
  })

  it('shows the server’s message next to the field it names', async () => {
    vi.spyOn(api, 'post').mockRejectedValue(new AxiosError('Bad request', 'ERR_BAD_REQUEST', undefined, undefined, {
      config: { headers: new AxiosHeaders() }, status: 400, statusText: 'Bad Request', headers: {},
      data: { message: 'Check the highlighted fields.', fieldErrors: { examDate: 'The exam must be within 16 weeks.' } },
    }))
    open('/study-plan/new?courseId=4&hoursPerWeek=6')
    fireEvent.click(await screen.findByLabelText('Paste topics'))
    fireEvent.change(screen.getByLabelText('Topics to cover'), { target: { value: 'Encapsulation' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    expect(await screen.findByText('The exam must be within 16 weeks.')).toBeInTheDocument()
  })
})

describe('a study plan’s roadmap', () => {
  const entry = (id: number, sequence: number, title: string, completed = false) => ({
    id, sequence, scheduledDate: sequence === 1 ? '2026-10-28' : '2026-11-04', title, plannedMinutes: 90,
    tasks: [`Re-read the ${title} slides`], completed,
  })
  const roadmap: StudyPlan = {
    ...plan, studyGoal: 'EXAM_PREPARATION', sourceKind: 'UPLOAD', sourceFilenames: ['week1.pdf', 'week2.pdf'],
    sourceTopics: ['Encapsulation', 'Inheritance', 'Polymorphism'],
    entries: [entry(1, 1, 'Encapsulation', true), entry(2, 1, 'Inheritance'), entry(3, 2, 'Polymorphism')],
  }
  function open(data: StudyPlan = roadmap, path = '/study-plan/7') {
    vi.spyOn(api, 'get').mockResolvedValue(response(data))
    return renderPage(<StudyPlanDetailPage />, path, '/study-plan/:id')
  }

  it('shows each week with its topics, time, tasks and progress', async () => {
    open()
    const weeks = within(await screen.findByRole('list', { name: 'Roadmap' })).getAllByRole('heading', { level: 3 })
    expect(weeks.map(week => week.textContent)).toEqual([
      'Week 1 · from 28 Oct 2026 · 3 h of 6 h planned', 'Week 2 · from 4 Nov 2026 · 1 h 30 min of 6 h planned',
    ])
    expect(screen.getByRole('checkbox', { name: /Encapsulation/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /Polymorphism/ })).not.toBeChecked()
    expect(within(screen.getByRole('list', { name: 'Tasks for Inheritance' })).getByText('Re-read the Inheritance slides')).toBeInTheDocument()
    expect(screen.getByText('1 of 3 topics done')).toBeInTheDocument()
    expect(screen.getByText(/Drafted from week1.pdf, week2.pdf/)).toBeInTheDocument()
    expect(screen.getByText('Roadmap written by Gemini; it may be inaccurate.')).toBeInTheDocument()
  })

  it('ticks a topic off and shows the saved plan', async () => {
    const put = vi.spyOn(api, 'put').mockResolvedValue(response({
      ...roadmap, entries: roadmap.entries.map(each => each.id === 3 ? { ...each, completed: true } : each),
    }))
    open()
    fireEvent.click(await screen.findByRole('checkbox', { name: /Polymorphism/ }))
    await waitFor(() => expect(screen.getByRole('checkbox', { name: /Polymorphism/ })).toBeChecked())
    expect(put).toHaveBeenCalledWith('/study-plans/7/entries/3/progress', { completed: true })
    expect(screen.getByText('2 of 3 topics done')).toBeInTheDocument()
  })

  it('keeps the topic as it was when saving fails', async () => {
    vi.spyOn(api, 'put').mockRejectedValue(failure(500, 'Progress could not be saved.'))
    open()
    fireEvent.click(await screen.findByRole('checkbox', { name: /Encapsulation/ }))
    expect(await screen.findByText('Progress could not be saved.')).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /Encapsulation/ })).toBeChecked()
  })

  it('deletes the plan only after confirming', async () => {
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    const { router } = open()
    fireEvent.click(await screen.findByRole('button', { name: 'Delete study plan' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(remove).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Delete study plan' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete study plan' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/study-plan'))
    expect(remove).toHaveBeenCalledWith('/study-plans/7')
    expect(router.state.location.state).toMatchObject({ notice: 'Study plan deleted.' })
  })

  it('offers no ticking or deleting to someone who cannot manage the plan', async () => {
    open({ ...roadmap, canManage: false })
    expect(await screen.findByRole('checkbox', { name: /Polymorphism/ })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Delete study plan' })).not.toBeInTheDocument()
  })

  it('says so when the plan cannot be found or the link is wrong', async () => {
    vi.spyOn(api, 'get').mockRejectedValue(failure(404, 'Study plan not found.'))
    renderPage(<StudyPlanDetailPage />, '/study-plan/99', '/study-plan/:id')
    expect(await screen.findByText('Study plan not found.')).toBeInTheDocument()
    vi.restoreAllMocks()
    const get = vi.spyOn(api, 'get')
    renderPage(<StudyPlanDetailPage />, '/study-plan/abc', '/study-plan/:id')
    expect(await screen.findByText('This study plan link is invalid.')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })
})
