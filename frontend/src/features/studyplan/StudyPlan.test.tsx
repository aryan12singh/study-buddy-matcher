import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import StudyPlanPage from './StudyPlanPage'
import CreateStudyPlanPage from './CreateStudyPlanPage'
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

  it('uploads the chosen files with the details and returns to the list', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(plan))
    const { router } = open()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Hours per week'), { target: { value: '6' } })
    fireEvent.change(screen.getByLabelText('Study goal (optional)'), { target: { value: 'EXAM_PREPARATION' } })
    fireEvent.change(screen.getByLabelText('Lecture slides or notes'), { target: { files: [pdf()] } })
    expect(within(screen.getByRole('list', { name: 'Chosen files' })).getByText('week1.pdf')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/study-plan'))
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

  it('says plainly when Gemini could not draft the plan', async () => {
    vi.spyOn(api, 'post').mockRejectedValue(failure(503, 'The AI assistant is unavailable.', 'AI_UNAVAILABLE'))
    open('/study-plan/new?courseId=4&hoursPerWeek=6')
    fireEvent.click(await screen.findByLabelText('Paste topics'))
    fireEvent.change(screen.getByLabelText('Topics to cover'), { target: { value: 'Encapsulation' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create study plan' }))
    expect(await screen.findByText(/Gemini could not draft the roadmap just now, so no plan was saved/)).toBeInTheDocument()
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
