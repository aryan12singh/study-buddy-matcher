import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import GroupsPage from './GroupsPage'
import GroupCard from './GroupCard'

const course = { id: 3, code: 'IS442', name: 'Object Oriented Programming' }
afterEach(() => vi.restoreAllMocks())

describe('group browsing state', () => {
  it('uses deep-linked filters and clears individual/all filters without losing unrelated URL values', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : []))
    const { router } = renderPage(<GroupsPage />, '/groups?courseId=3&studyGoal=PROBLEM_SOLVING&studyMode=IN_PERSON&ref=demo', '/groups')
    await screen.findByRole('button', { name: 'Remove Course: IS442 filter' })
    expect(get).toHaveBeenCalledWith('/groups', expect.objectContaining({ params: { courseId: '3', studyGoal: 'PROBLEM_SOLVING', studyMode: 'IN_PERSON' } }))
    fireEvent.click(screen.getByRole('button', { name: 'Remove Goal: Problem solving filter' }))
    expect(router.state.location.search).not.toContain('studyGoal')
    expect(router.state.location.search).toContain('ref=demo')
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
    await waitFor(() => expect(get).toHaveBeenCalledWith('/groups', expect.objectContaining({ params: {} })))
    expect(router.state.location.search).toBe('?ref=demo')
    expect(await screen.findByRole('link', { name: 'Start a study group' })).toHaveAttribute('href', '/groups/new')
  })
  it('restores tab and filters on browser Back and refetches the correct API', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : []))
    const { router } = renderPage(<GroupsPage />, '/groups?courseId=3', '/groups')
    await screen.findByRole('button', { name: 'Remove Course: IS442 filter' })
    fireEvent.click(screen.getByRole('button', { name: 'Your groups' }))
    await screen.findByText('No groups joined yet')
    expect(router.state.location.search).toContain('view=mine')
    await act(async () => { await router.navigate(-1) })
    expect(screen.getByRole('button', { name: 'Browse groups' })).toHaveAttribute('aria-pressed', 'true')
    expect(await screen.findByRole('button', { name: 'Remove Course: IS442 filter' })).toBeInTheDocument()
    expect(get).toHaveBeenCalledWith('/groups', expect.objectContaining({ params: { courseId: '3' } }))
  })
  it('does not send invalid deep-linked filter values to the API', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : []))
    renderPage(<GroupsPage />, '/groups?view=secret&courseId=-1&studyGoal=UNKNOWN&studyMode=UNKNOWN', '/groups')
    await screen.findByText('No groups match these filters')
    expect(get).toHaveBeenCalledWith('/groups', expect.objectContaining({ params: {} }))
    expect(screen.queryByRole('group', { name: 'Active filters' })).not.toBeInTheDocument()
  })
  it('renders truthful closed/full capacity and offers no management for a closed group', () => {
    const group = { id: 5, name: 'OOP crew', courseId: 3, courseCode: 'IS442', courseName: 'OOP', leaderId: 1, leaderName: 'Priya',
      preferredStudyMode: null, studyGoals: [], maxGroupSize: 2, memberCount: 2, active: false,
      viewer: { leader: true, member: true, requestId: null, requestStatus: null } } as const
    renderPage(<GroupCard group={{ ...group, studyGoals: [] }} />)
    expect(screen.getByRole('meter', { name: 'Group capacity' })).toHaveAttribute('aria-valuetext', '2 of 2 members, group closed')
    expect(screen.queryByRole('link', { name: 'Manage group' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View group' })).toBeInTheDocument()
    expect(screen.queryByText('places available')).not.toBeInTheDocument()
  })
})
