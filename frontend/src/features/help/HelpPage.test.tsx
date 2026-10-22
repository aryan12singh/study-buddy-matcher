import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { matchRoutes } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { appRoutes } from '../../routes'
import { renderPage, response } from '../../test/renderApp'
import { FAQS } from './helpContent'
import HelpPage from './HelpPage'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const profile = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2,
  contactNumber: 'Synthetic contact 1', coursesTaken: [course], targetCourse: null, preferredStudyMode: 'ONLINE',
  groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: []
}
const summary = { activeConnections: 0, pendingIncoming: 0, pendingOutgoing: 1, acceptedGroups: 0 }

function serve(failing?: string) {
  return vi.spyOn(api, 'get').mockImplementation(async path => {
    if (path === failing) {
      throw new AxiosError('Server Error', 'ERR_BAD_RESPONSE', undefined, undefined, {
        config: { headers: new AxiosHeaders() }, status: 500, statusText: 'Server Error', headers: {}, data: { message: 'The action could not be completed; try again later' }
      })
    }
    return response(({ '/profile/me': profile, '/students/me/summary': summary } as Record<string, unknown>)[path as string])
  })
}

function startHere() { return screen.getByRole('region', { name: 'Start here' }) }

afterEach(() => vi.restoreAllMocks())

describe('help page', () => {
  it('links every first step to a real student route', async () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    const links = within(startHere()).getAllByRole('link')
    expect(links.map(link => link.getAttribute('href'))).toEqual([
      '/students/1/edit?tab=preferences', '/students/1/edit?tab=availability', '/find-buddies', '/connections?view=outgoing', '/groups'
    ])
    for (const link of links) {
      const [pathname] = link.getAttribute('href')!.split('?')
      const matched = matchRoutes(appRoutes, pathname) ?? []
      expect(matched.at(-1)?.route.path, pathname).not.toBe('*')
    }
    await screen.findByRole('img', { name: '3 of 5 steps done' })
  })

  it('ticks steps from the student’s own profile and activity', async () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    expect(within(startHere()).getByRole('status')).toHaveTextContent('Checking your progress')
    expect(await screen.findByRole('img', { name: '3 of 5 steps done' })).toBeInTheDocument()
    const steps = within(startHere()).getAllByRole('listitem')
    expect(steps[0]).toHaveTextContent('Done')
    expect(steps[1]).toHaveTextContent('Not done yet')
    expect(steps[3]).toHaveTextContent('Done')
    expect(steps[4]).toHaveTextContent('Not done yet')
  })

  it('keeps the steps usable and offers a retry when progress cannot load', async () => {
    const get = serve('/students/me/summary')
    renderPage(<HelpPage />, '/help', '/help')
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('We could not check your progress')
    expect(within(startHere()).getAllByRole('link')).toHaveLength(5)
    get.mockClear()
    fireEvent.click(within(alert).getByRole('button', { name: 'Check progress again' }))
    expect(get).toHaveBeenCalledWith('/students/me/summary', expect.anything())
    expect(get).not.toHaveBeenCalledWith('/profile/me', expect.anything())
  })

  it('explains each main area', () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    for (const name of ['Matching', 'Requests and privacy', 'Study groups', 'Study rooms', 'Study sessions', 'Notifications']) {
      expect(screen.getByRole('article', { name })).toBeInTheDocument()
    }
  })

  it('filters the questions and shows an empty state with a way back', () => {
    serve()
    renderPage(<HelpPage />, '/help', '/help')
    const faq = screen.getByRole('region', { name: 'Frequently asked questions' })
    expect(within(faq).getAllByRole('button', { expanded: false })).toHaveLength(FAQS.length)
    const search = screen.getByLabelText('Search questions')
    fireEvent.change(search, { target: { value: 'CALENDAR' } })
    expect(within(faq).getAllByRole('button', { expanded: false }).map(button => button.textContent))
      .toEqual(['Does the calendar export stay up to date?'])
    expect(screen.getByText(`1 of ${FAQS.length} questions shown`)).toBeInTheDocument()
    fireEvent.change(search, { target: { value: 'quantum physics' } })
    expect(screen.getByRole('heading', { name: 'No answers match “quantum physics”' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Clear search' }))
    expect(search).toHaveValue('')
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
    expect(answer).toHaveTextContent('Only after a match request between you is accepted.')
    fireEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
  })
})
