import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { response } from '../../test/renderApp'
import QuizBotLauncher from './QuizBotLauncher'
import { findCourse } from './findCourse'
import type { Quiz } from './api'

afterEach(() => vi.restoreAllMocks())

const course = { id: 4, code: 'IS442', name: 'Object Oriented Programming' }
function quiz(overrides: Partial<Quiz> = {}): Quiz {
  return {
    id: 7, scope: 'GROUP', course, groupId: 3, sessionId: null, title: 'week4.pdf', status: 'OPEN', createdBy: 1,
    createdAt: '2026-10-25T02:00:00Z', closesAt: '2026-10-25T02:15:00Z', closedAt: null, questionCount: 2,
    submittedCount: 1, myAttempt: 'NOT_STARTED', canHost: false, ...overrides,
  }
}
const questions = {
  quizId: 7, startedAt: '2026-10-25T02:01:00Z', closesAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(), questions: [
    { id: 101, prompt: 'What hides state?', options: ['Encapsulation', 'Inheritance', 'Polymorphism', 'Abstraction'] },
    { id: 102, prompt: 'What overrides at runtime?', options: ['Encapsulation', 'Inheritance', 'Polymorphism', 'Abstraction'] },
  ],
}
const slides = () => new File(['%PDF-1.7 week 4'], 'week4.pdf', { type: 'application/pdf' })

function aiUnavailable() {
  const headers = new AxiosHeaders()
  return new AxiosError('Service Unavailable', '503', { headers }, undefined, {
    status: 503, statusText: 'Service Unavailable', headers, config: { headers },
    data: { code: 'AI_UNAVAILABLE', message: 'The AI assistant is unavailable right now' },
  })
}

function openBot(home: Parameters<typeof QuizBotLauncher>[0]['home'], canStart: boolean) {
  const view = render(<QuizBotLauncher home={home} canStart={canStart} />)
  fireEvent.click(screen.getByRole('button', { name: 'Open Quiz Buddy' }))
  return view
}

function upload(files: File[]) {
  fireEvent.change(document.getElementById('quiz-bot-files')!, { target: { files } })
}
function type(text: string) {
  fireEvent.change(screen.getByLabelText('Message Quiz Buddy'), { target: { value: text } })
}
function send() {
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
}

describe('Quiz Buddy', () => {
  it('reads a course and focus from the message sent with the files, and writes the quiz straight away', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url: string) => response(url === '/courses' ? [course] : []))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(quiz({ scope: 'PERSONAL', groupId: null, title: 'interfaces', questionCount: 10 })))
    openBot({ scope: 'PERSONAL' }, true)
    await act(async () => {})

    upload([slides()])
    expect(screen.getByRole('list', { name: 'Attached files' }).textContent).toContain('week4.pdf')
    type('IS442, focus on interfaces')
    send()

    expect(await screen.findByText(/Your quiz on “interfaces” is ready: 10 questions/)).toBeTruthy()
    const [path, form] = post.mock.calls[0] as [string, FormData]
    expect(path).toBe('/quizzes')
    expect(form.get('courseId')).toBe('4')
    expect(form.get('focus')).toBe('interfaces')
    expect((form.getAll('files')[0] as File).name).toBe('week4.pdf')
    expect(screen.getByRole('button', { name: 'Start quiz' })).toBeTruthy()
  })

  it('asks for whatever is missing and understands typed answers', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url: string) => response(url === '/courses' ? [course] : []))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(quiz({ scope: 'PERSONAL', groupId: null })))
    openBot({ scope: 'PERSONAL' }, true)

    upload([slides()])
    send()
    expect(await screen.findByText(/Which course are these notes for/)).toBeTruthy()
    type('IS999')
    send()
    expect(screen.getByText(/I couldn't find a course called “IS999”/)).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'IS442' }))
    expect(screen.getByText(/Anything I should focus on/)).toBeTruthy()
    type('skip')
    send()

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1))
    const [, form] = post.mock.calls[0] as [string, FormData]
    expect(form.get('courseId')).toBe('4')
    expect(form.get('focus')).toBeNull()
  })

  it('refuses a file it cannot read before sending anything', () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const post = vi.spyOn(api, 'post')
    openBot({ scope: 'PERSONAL' }, true)

    upload([new File(['slides'], 'week4.pptx')])

    expect(screen.getByText(/week4\.pptx is not a PDF, \.txt or \.md file/)).toBeTruthy()
    expect(post).not.toHaveBeenCalled()
  })

  it('takes pasted notes for a group quiz, says plainly when Gemini is unavailable, and tries again', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const post = vi.spyOn(api, 'post').mockRejectedValueOnce(aiUnavailable()).mockResolvedValueOnce(response(quiz({ title: 'Pasted notes' })))
    openBot({ scope: 'GROUP', groupId: 3 }, true)

    type('hi')
    send()
    expect(screen.getByText(/That looks a bit short to quiz on/)).toBeTruthy()
    type('Polymorphism lets a subclass override a method of its parent class.')
    send()
    expect(screen.getByText(/Anything I should focus on/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Cover everything' }))

    expect(await screen.findByText(/I couldn't reach Gemini just now/)).toBeTruthy()
    const [path, form] = post.mock.calls[0] as [string, FormData]
    expect(path).toBe('/groups/3/quizzes')
    expect(form.get('notesText')).toBe('Polymorphism lets a subclass override a method of its parent class.')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText(/Your quiz on “Pasted notes” is ready/)).toBeTruthy()
    expect(post).toHaveBeenCalledTimes(2)
  })

  it('matches a course by code or by name', () => {
    expect(findCourse('is 442 please', [course])).toBe(course)
    expect(findCourse('object oriented programming', [course])).toBe(course)
    expect(findCourse('IS212', [course])).toBeNull()
  })

  it('lets a member who cannot start quizzes take the group’s quizzes', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response([quiz()]))
    openBot({ scope: 'GROUP', groupId: 3 }, false)

    expect(await screen.findByText('week4.pdf')).toBeTruthy()
    expect(get).toHaveBeenCalledWith('/groups/3/quizzes', expect.anything())
    expect(screen.queryByRole('button', { name: 'Upload notes' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Take quiz' })).toBeTruthy()
  })

  it('explains an empty quiz list', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    openBot({ scope: 'PERSONAL' }, true)

    expect(await screen.findByText('No quizzes yet')).toBeTruthy()
  })

  it('needs every question answered, then shows the score and rank', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([quiz()]))
    const post = vi.spyOn(api, 'post').mockImplementation(async (url: string) =>
      response(url === '/quizzes/7/start' ? questions : { score: 1, total: 2, timeTakenSeconds: 95, rank: 2 }))
    openBot({ scope: 'GROUP', groupId: 3 }, false)
    fireEvent.click(await screen.findByRole('button', { name: 'Take quiz' }))
    await screen.findByText('1. What hides state?')

    fireEvent.click(screen.getAllByLabelText('Encapsulation')[0])
    fireEvent.click(screen.getByRole('button', { name: 'Submit answers' }))
    expect(screen.getByText(/Answer every question before submitting/)).toBeTruthy()
    expect(post).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getAllByLabelText('Inheritance')[1])
    fireEvent.click(screen.getByRole('button', { name: 'Submit answers' }))

    await waitFor(() => expect(post).toHaveBeenCalledWith('/quizzes/7/attempts', {
      answers: [{ questionId: 101, optionIndex: 0 }, { questionId: 102, optionIndex: 1 }],
    }))
    expect(await screen.findByText('#2')).toBeTruthy()
    expect(screen.getByText(/Time taken 1:35/)).toBeTruthy()
  })

  it('counts down a timed group quiz and stops a submit once time is up', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([quiz()]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...questions, closesAt: new Date(Date.now() - 1000).toISOString() }))
    openBot({ scope: 'GROUP', groupId: 3 }, false)
    fireEvent.click(await screen.findByRole('button', { name: 'Take quiz' }))

    expect(await screen.findByText(/Time's up: this quiz has closed/)).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Submit answers' }) as HTMLButtonElement).disabled).toBe(true)
    expect(post).toHaveBeenCalledTimes(1)
  })

  it('shows the time left on a quiz that closes within the hour', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([quiz()]))
    vi.spyOn(api, 'post').mockResolvedValue(response({ ...questions, closesAt: new Date(Date.now() + 10 * 60 * 1000).toISOString() }))
    openBot({ scope: 'GROUP', groupId: 3 }, false)
    fireEvent.click(await screen.findByRole('button', { name: 'Take quiz' }))

    expect(await screen.findByText(/left to submit/)).toBeTruthy()
  })

  it('highlights the signed-in member on the leaderboard', async () => {
    vi.spyOn(api, 'get').mockImplementation(async (url: string) => response(url === '/quizzes/7/leaderboard'
      ? { quizId: 7, status: 'CLOSED', submittedCount: 2, notSubmittedCount: 1, entries: [
          { rank: 1, studentId: 2, displayName: 'Marcus', score: 2, total: 2, timeTakenSeconds: 60, me: false },
          { rank: 2, studentId: 1, displayName: 'Priya', score: 1, total: 2, timeTakenSeconds: 95, me: true }] }
      : [quiz({ status: 'CLOSED' })]))
    openBot({ scope: 'GROUP', groupId: 3 }, false)
    fireEvent.click(await screen.findByRole('button', { name: 'View leaderboard' }))

    expect(await screen.findByText('2 of 3 submitted · final results')).toBeTruthy()
    expect(screen.getByText('Priya').closest('tr')?.className).toBe('is-me')
  })
})
