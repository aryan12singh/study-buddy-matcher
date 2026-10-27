import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import StudyPlanPage from './StudyPlanPage'
import type { StudyPlanSummary } from './api'

const course = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
const summary: StudyPlanSummary = {
  id: 7, scope: 'PERSONAL', course, firstBlockDate: '2026-10-28', blockCount: 4, examDate: '2026-11-20',
  studyGoal: 'EXAM_PREPARATION', entryCount: 8, completedCount: 3, createdAt: '2026-10-28T01:00:00Z',
}
function failure(status: number, message: string) {
  return new AxiosError(message, 'ERR_BAD_RESPONSE', undefined, undefined,
    { config: { headers: new AxiosHeaders() }, status, statusText: 'Error', headers: {}, data: { message } })
}
afterEach(() => { vi.restoreAllMocks() })

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
})
