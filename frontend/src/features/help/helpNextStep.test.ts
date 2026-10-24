import { describe, expect, it } from 'vitest'
import type { MyProfile } from '../profile/api'
import type { GroupSummary } from '../groups/api'
import type { StudySession } from '../sessions/api'
import type { HelpProgress } from './startSteps'
import { startSteps } from './startSteps'
import { helpNextStep } from './helpNextStep'

const now = Date.parse('2026-10-23T00:00:00Z')
const profile: MyProfile = {
  id: 1, name: 'Priya', email: 'student@example.test', school: 'SCIS', programme: 'IS', yearOfStudy: 2,
  contactNumber: 'Synthetic contact', coursesTaken: [{ id: 1, code: 'IS442', name: 'OOP' }], targetCourse: null,
  preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [],
  availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }]
}
const group: GroupSummary = {
  id: 5, name: 'Revision crew', courseId: 1, courseCode: 'IS442', courseName: 'OOP', leaderId: 2, leaderName: 'Jamie',
  preferredStudyMode: null, studyGoals: [], maxGroupSize: 5, memberCount: 2, active: true,
  viewer: { leader: false, member: true, requestId: null, requestStatus: null }
}
const occurrence = { startAt: '2026-10-24T02:00:00Z', endAt: '2026-10-24T03:00:00Z' }
const session: StudySession = {
  id: 11, version: 1, scope: 'GROUP', groupId: 5, connectionId: null, title: 'Practice paper',
  startLocal: '2026-10-24T10:00', endLocal: '2026-10-24T11:00', ...occurrence, timeZone: 'Asia/Singapore', occurrenceCount: 1,
  agenda: null, location: null, roomLink: '/groups/5/room', status: 'SCHEDULED', canManage: false,
  occurrences: [occurrence], nextOccurrence: occurrence, updatedAt: '2026-10-23T00:00:00Z', cancelledAt: null
}
const loaded = <T,>(data: T) => ({ data, loading: false })
const sources = (): HelpProgress => ({ profile: loaded(profile), summary: loaded({ activeConnections: 0, pendingIncoming: 0, pendingOutgoing: 0, acceptedGroups: 0 }), groups: loaded([]), sessions: loaded([]) })

describe('current help progress and next action', () => {
  it('uses the same required profile order as Home before requests or sessions', () => {
    const data = sources()
    data.profile = loaded({ ...profile, coursesTaken: [], preferredStudyMode: null, availability: [] })
    data.summary.data!.pendingIncoming = 2
    expect(helpNextStep(1, data, now).step?.to).toBe('/students/1/edit?tab=preferences')
    data.profile = loaded({ ...profile, availability: [] })
    expect(helpNextStep(1, data, now).step?.to).toBe('/students/1/edit?tab=availability')
  })

  it('recommends a confirmed incoming reply without waiting for unrelated data', () => {
    const data = sources()
    data.summary.data!.pendingIncoming = 2
    data.groups = { loading: true }
    data.sessions = { loading: false, error: 'Unavailable' }
    expect(helpNextStep(1, data, now).step).toMatchObject({ title: 'Reply to 2 study buddy requests', to: '/connections?view=incoming' })
  })

  it.each(['profile', 'summary', 'groups', 'sessions'] as const)('waits for %s and distinguishes failure from missing activity', key => {
    const data = sources()
    data[key] = { loading: true }
    expect(helpNextStep(1, data, now).step).toBeUndefined()
    expect(helpNextStep(1, data, now).message).toContain('Checking')
    data[key] = { loading: false, error: 'Unavailable' }
    expect(helpNextStep(1, data, now).message).toContain('could not be checked')
    expect(startSteps(1, data, now).some(step => step.state === 'unavailable')).toBe(true)
  })

  it('never infers search or room visits from requests, membership or sessions', () => {
    const data = sources()
    data.summary.data!.pendingOutgoing = 4
    data.groups = loaded([group])
    data.sessions = loaded([session])
    const steps = startSteps(1, data, now)
    expect(steps.filter(step => step.state === 'confirmed').map(step => step.key)).toEqual(['preferences', 'availability', 'request', 'group', 'session'])
    expect(steps.filter(step => step.state === 'untracked').map(step => step.key)).toEqual(['search', 'room'])
    expect(helpNextStep(1, data, now).step?.to).toBe('/sessions/11')
  })

  it('excludes expired and cancelled dates while advancing weekly occurrences', () => {
    const data = sources()
    const past = { startAt: '2026-10-22T02:00:00Z', endAt: '2026-10-22T03:00:00Z' }
    data.sessions = loaded([{ ...session, id: 12, status: 'CANCELLED' }, { ...session, id: 13, occurrences: [past] }])
    expect(startSteps(1, data, now).find(step => step.key === 'session')?.state).toBe('missing')
    expect(helpNextStep(1, data, now).step?.to).toBe('/find-buddies')
    data.sessions = loaded([{ ...session, nextOccurrence: past, occurrences: [past, occurrence] }])
    expect(helpNextStep(1, data, now).step?.detail).toContain('24 Oct 2026')
    expect(startSteps(1, data, Date.parse(occurrence.endAt)).find(step => step.key === 'session')?.state).toBe('missing')
  })

  it('offers a leader the agenda and a member a preview, without granting strangers or closed groups access', () => {
    const data = sources()
    data.groups = loaded([group])
    expect(helpNextStep(1, data, now).step?.to).toBe('/groups/5/room')
    expect(startSteps(1, data, now).find(step => step.key === 'room')?.to).toBe('/groups/5/room')
    data.groups = loaded([{ ...group, viewer: { ...group.viewer, leader: true } }])
    expect(helpNextStep(1, data, now).step?.to).toBe('/groups/5/sessions')
    data.groups = loaded([{ ...group, active: false }, { ...group, id: 6, viewer: { ...group.viewer, member: false, requestStatus: 'PENDING' } }])
    expect(helpNextStep(1, data, now).step?.to).toBe('/find-buddies')
    expect(startSteps(1, data, now).find(step => step.key === 'room')?.to).toBe('/groups?view=mine')
  })

  it('suggests buddy planning or groups only from confirmed current relationships', () => {
    const data = sources()
    data.summary.data!.activeConnections = 1
    expect(helpNextStep(1, data, now).step?.to).toBe('/connections?view=connected')
    data.summary.data!.activeConnections = 0
    data.summary.data!.pendingOutgoing = 1
    expect(helpNextStep(1, data, now).step?.title).toBe('Explore study groups while you wait')
    data.summary.data!.pendingOutgoing = 0
    expect(startSteps(1, data, now).find(step => step.key === 'request')?.stateLabel).toBe('No pending sent request or active connection')
  })
})
