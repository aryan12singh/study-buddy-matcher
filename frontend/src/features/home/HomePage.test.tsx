import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import HomePage from './HomePage'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const completeProfile = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2,
  contactNumber: 'Synthetic contact 1', coursesTaken: [course], targetCourse: course, preferredStudyMode: 'ONLINE',
  groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }]
}
const activeData: Record<string, unknown> = {
  '/profile/me': completeProfile,
  '/students/me/summary': { activeConnections: 3, pendingIncoming: 1, pendingOutgoing: 1, acceptedGroups: 1 },
  '/match-requests/incoming': [
    { id: 7, senderId: 2, senderName: 'Jamie Lee', receiverId: 1, receiverName: 'Priya', message: 'Want to revise IS442?', status: 'PENDING', createdAt: '2026-10-08T06:20:00Z', respondedAt: null, context: { origin: 'MATCHING', courseCode: 'IS442' } },
    { id: 8, senderId: 3, senderName: 'Alex Tan', receiverId: 1, receiverName: 'Priya', message: null, status: 'DECLINED', createdAt: '2026-10-07T06:20:00Z', respondedAt: null, context: { origin: 'PROFILE' } }
  ],
  '/groups/mine': [
    { id: 5, name: 'OOP study crew', courseId: 1, courseCode: 'IS442', courseName: 'Object Oriented Programming', leaderId: 2, leaderName: 'Jamie Lee',
      preferredStudyMode: null, studyGoals: [], maxGroupSize: 5, memberCount: 3, active: true, viewer: { leader: false, member: true, requestId: null, requestStatus: null } }
  ],
  '/notifications': [
    { id: 11, type: 'MATCH_REQUEST_RECEIVED', message: 'Jamie Lee sent you a study buddy request.', read: false, createdAt: '2026-10-08T06:20:00Z',
      resourceType: 'MATCH_REQUEST', resourceId: 7, eventKey: null, requestDirection: 'INCOMING' }
  ]
}
const newStudentData: Record<string, unknown> = {
  '/profile/me': { ...completeProfile, coursesTaken: [], preferredStudyMode: null, availability: [], targetCourse: null },
  '/students/me/summary': { activeConnections: 0, pendingIncoming: 0, pendingOutgoing: 0, acceptedGroups: 0 },
  '/match-requests/incoming': [], '/groups/mine': [], '/notifications': []
}

function serve(data: Record<string, unknown>, failing?: string) {
  return vi.spyOn(api, 'get').mockImplementation(async path => {
    if (path === failing) {
      throw new AxiosError('Server Error', 'ERR_BAD_RESPONSE', undefined, undefined, {
        config: { headers: new AxiosHeaders() }, status: 500, statusText: 'Server Error', headers: {}, data: { message: 'The action could not be completed; try again later' }
      })
    }
    return response(data[path as string])
  })
}

afterEach(() => vi.restoreAllMocks())

describe('student home', () => {
  it('shows a complete profile, linked activity counts and short previews', async () => {
    serve(activeData)
    renderPage(<HomePage />, '/home', '/home')
    expect(await screen.findByRole('heading', { name: 'Welcome back, Priya' })).toBeInTheDocument()
    expect(await screen.findByText('Profile complete')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit my profile' })).toHaveAttribute('href', '/students/1/edit')
    const stats = await screen.findByRole('navigation', { name: 'Your activity' })
    expect(within(stats).getByRole('link', { name: '1 Requests waiting for you' })).toHaveAttribute('href', '/connections?view=incoming')
    expect(within(stats).getByRole('link', { name: '1 Study groups' })).toHaveAttribute('href', '/groups?view=mine')
    expect(await screen.findByRole('link', { name: 'OOP study crew' })).toHaveAttribute('href', '/groups/5')
    expect(screen.getByRole('link', { name: 'All my groups ›' })).toHaveAttribute('href', '/groups?view=mine')
    expect(await screen.findByText('Jamie Lee sent you a study buddy request.')).toBeInTheDocument()
    expect(screen.getByText('09:00–10:00')).toBeInTheDocument()
  })
  it('previews pending requests only and points to the connections page to reply', async () => {
    serve(activeData)
    renderPage(<HomePage />, '/home', '/home')
    const requests = await screen.findByRole('region', { name: /Waiting for your reply/ })
    expect(await within(requests).findByRole('link', { name: 'Jamie Lee' })).toHaveAttribute('href', '/students/2')
    expect(within(requests).getByText('From a matching search')).toBeInTheDocument()
    expect(within(requests).getByText('“Want to revise IS442?”')).toBeInTheDocument()
    expect(within(requests).getByRole('link', { name: 'Review request ›' })).toHaveAttribute('href', '/connections?view=incoming')
    expect(screen.queryByText('Alex Tan')).not.toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Next step' })).toHaveTextContent('Reply to 1 study buddy request')
  })
  it('links to the matching search on the Connections page', async () => {
    serve(activeData)
    renderPage(<HomePage />, '/home', '/home')
    const finder = screen.getByRole('region', { name: /Find study buddies/ })
    expect(within(finder).getByText('See ranked matches')).toBeInTheDocument()
    expect(within(finder).queryByText('Coming soon')).not.toBeInTheDocument()
    expect(within(finder).getByRole('link', { name: 'Search for study buddies' })).toHaveAttribute('href', '/connections?view=find')
  })
  it('guides a new student to finish their profile and explains every empty list', async () => {
    serve(newStudentData)
    renderPage(<HomePage />, '/home', '/home')
    expect(await screen.findByRole('heading', { name: 'Welcome, Priya' })).toBeInTheDocument()
    expect(screen.getByText('Profile 1 of 3 steps')).toBeInTheDocument()
    const next = screen.getByRole('region', { name: 'Next step' })
    expect(within(next).getByText('Add your courses and study mode')).toBeInTheDocument()
    expect(within(next).getByRole('link', { name: 'Continue setup ›' })).toHaveAttribute('href', '/students/1/edit?tab=preferences')
    expect(screen.getByRole('link', { name: 'Add weekly times ›' })).toHaveAttribute('href', '/students/1/edit?tab=availability')
    expect(await screen.findByText('No requests waiting')).toBeInTheDocument()
    expect(await screen.findByText('You are not in a group yet')).toBeInTheDocument()
    expect(await screen.findByText('You are all caught up')).toBeInTheDocument()
  })
  it('keeps the rest of the page working when one section fails', async () => {
    serve(activeData, '/notifications')
    renderPage(<HomePage />, '/home', '/home')
    const activity = await screen.findByRole('region', { name: /Recent activity/ })
    expect(await within(activity).findByRole('button', { name: 'Try again' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'OOP study crew' })).toBeInTheDocument()
    expect(await screen.findByText('Profile complete')).toBeInTheDocument()
  })
})
