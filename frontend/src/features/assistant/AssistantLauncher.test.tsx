import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { response } from '../../test/renderApp'
import AssistantLauncher from './AssistantLauncher'
import type { AssistantReply } from './api'

afterEach(() => vi.restoreAllMocks())

function reply(overrides: Partial<AssistantReply>): AssistantReply {
  return { intent: 'UNKNOWN', text: '', filters: [], matches: [], slots: [], todos: [], action: null, ...overrides }
}

function openAssistant() {
  render(<MemoryRouter><AssistantLauncher /></MemoryRouter>)
  fireEvent.click(screen.getByRole('button', { name: 'Open Pixel, your study assistant' }))
}

function ask(text: string) {
  fireEvent.change(screen.getByLabelText('Message Pixel'), { target: { value: text } })
  fireEvent.click(screen.getByRole('button', { name: 'Send' }))
}

describe('Pixel, the study assistant', () => {
  it('opens from the corner button with a greeting and suggestions, and keeps the chat when closed', () => {
    openAssistant()

    expect(screen.getByText(/I'm Pixel, your study assistant/)).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Pixel' })).toBeTruthy()
    expect(screen.getByText('Your AI study assistant · Ask me anything')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'When can my group meet for 2 hours?' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Close Pixel' }))
    fireEvent.click(screen.getByRole('button', { name: 'Open Pixel, your study assistant' }))
    expect(screen.getByText(/I'm Pixel, your study assistant/)).toBeTruthy()
  })

  it('shows real match cards and the filters it understood', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(reply({
      intent: 'FIND_BUDDIES', text: 'I found 1 match for IS442. Here they are.', filters: ['IS442 Object Oriented Programming', 'Small group'],
      matches: [{ studentId: 9, name: 'Marcus Tan', programme: 'Information Systems', yearOfStudy: 2, quality: 'STRONG', sharedHoursPerWeek: 3 } as never],
      action: { kind: 'OPEN_PAGE', label: 'See all on Find buddies', link: '/find-buddies', prefill: {} },
    })))
    openAssistant()

    ask('find me an IS442 buddy who likes small groups')

    expect(await screen.findByText('I found 1 match for IS442. Here they are.')).toBeTruthy()
    expect(post).toHaveBeenCalledWith('/assistant/messages', { message: 'find me an IS442 buddy who likes small groups' }, expect.anything())
    expect(screen.getByRole('link', { name: 'Marcus Tan' }).getAttribute('href')).toBe('/students/9')
    expect(screen.getByText('Small group')).toBeTruthy()
    expect(screen.getByText(/3 shared hours a week/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'See all on Find buddies' }).getAttribute('href')).toBe('/find-buddies')
  })

  it('lists the group’s shared free times', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(reply({
      intent: 'WHEN_TO_MEET', text: 'All 4 members are free at this time each week.',
      slots: [{ dayOfWeek: 'THURSDAY', startTime: '14:00:00', endTime: '17:00:00' }],
    })))
    openAssistant()

    fireEvent.click(screen.getByRole('button', { name: 'When can my group meet for 2 hours?' }))

    expect(await screen.findByText('14:00–17:00')).toBeTruthy()
  })

  it('opens Quiz Buddy when asked to be quizzed', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(reply({
      intent: 'QUIZ_ME', text: 'Quiz Buddy writes quizzes from your own notes.',
      action: { kind: 'OPEN_QUIZ_BUDDY', label: 'Open Quiz Buddy', link: null, prefill: { topic: 'polymorphism' } },
    })))
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    openAssistant()

    ask('quiz me on polymorphism')
    fireEvent.click(await screen.findByRole('button', { name: 'Open Quiz Buddy' }))

    expect(screen.getByRole('dialog', { name: 'Quiz Buddy' })).toBeTruthy()
  })

  it('lists what is waiting with a link to each', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(reply({
      intent: 'MY_TO_DO', text: "Here's what's waiting for you:",
      todos: [{ text: '1 buddy request from Marcus Lim waiting for your reply', label: 'Review requests', link: '/connections?view=incoming' }],
    })))
    openAssistant()

    fireEvent.click(screen.getByRole('button', { name: 'What do I need to do?' }))

    expect(await screen.findByText('1 buddy request from Marcus Lim waiting for your reply')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Review requests ›' }).getAttribute('href')).toBe('/connections?view=incoming')
  })

  it('says plainly when the AI helper is unavailable, switched off or busy', async () => {
    const headers = new AxiosHeaders()
    vi.spyOn(api, 'post').mockRejectedValue(new AxiosError('Unavailable', '503', { headers }, undefined, {
      status: 503, statusText: 'Service Unavailable', headers, config: { headers }, data: { code: 'AI_UNAVAILABLE' },
    }))
    openAssistant()

    ask('hello')

    expect(await screen.findByText(/The AI helper isn't available right now — it may be switched off on this server or busy. You can still use the navigation menu/)).toBeTruthy()
  })
})
