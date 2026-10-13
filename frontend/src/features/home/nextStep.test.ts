import { describe, expect, it } from 'vitest'
import type { MatchRequest } from '../connections/api'
import type { MyProfile } from '../profile/api'
import { nextStep, setupProgress } from './nextStep'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const complete = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2,
  contactNumber: 'Synthetic contact 1', coursesTaken: [course], targetCourse: null, preferredStudyMode: 'ONLINE',
  groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }]
} as unknown as MyProfile

function request(id: number, senderName: string) {
  return { id, senderId: id, senderName, status: 'PENDING', context: { origin: 'PROFILE' } } as MatchRequest
}

describe('home next step', () => {
  it('counts sign-up plus the required profile steps', () => {
    expect(setupProgress(complete)).toEqual({ done: 3, total: 3 })
    expect(setupProgress({ ...complete, availability: [] })).toEqual({ done: 2, total: 3 })
  })
  it('asks for the first missing profile step before anything else', () => {
    const step = nextStep(1, { ...complete, coursesTaken: [] }, [request(2, 'Jamie')])
    expect(step?.title).toBe('Add your courses and study mode')
    expect(step?.to).toBe('/students/1/edit?tab=preferences')
    expect(step?.progress).toEqual({ done: 2, total: 3 })
  })
  it('asks for availability once preferences are done', () => {
    expect(nextStep(1, { ...complete, availability: [] })?.to).toBe('/students/1/edit?tab=availability')
  })
  it('names the students waiting when the profile is complete', () => {
    expect(nextStep(1, complete, [request(2, 'Jamie')])?.detail).toMatch(/^Jamie is waiting\./)
    expect(nextStep(1, complete, [request(2, 'Jamie'), request(3, 'Alex')])?.title).toBe('Reply to 2 study buddy requests')
    expect(nextStep(1, complete, [request(2, 'Jamie'), request(3, 'Alex'), request(4, 'Sam')])?.detail).toMatch(/^Jamie, Alex and 1 other are waiting\./)
  })
  it('has nothing to suggest when the profile is complete and nobody is waiting', () => {
    expect(nextStep(1, complete, [])).toBeNull()
  })
})
