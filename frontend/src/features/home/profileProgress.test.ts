import { describe, expect, it } from 'vitest'
import type { MyProfile } from '../profile/api'
import { profileProgress } from './profileProgress'

const course = { id: 1, code: 'IS442', name: 'Object Oriented Programming' }
const complete: MyProfile = {
  id: 1, email: 'priya@demo.example.test', name: 'Priya Nair', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 2,
  contactNumber: 'Synthetic contact 1', coursesTaken: [course], targetCourse: course, preferredStudyMode: 'ONLINE',
  groupSizePreference: 'EITHER', groupSizeMax: 5, studyGoals: [], availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }]
}

describe('profile progress', () => {
  it('is complete when courses, study mode and availability are set', () => {
    expect(profileProgress(complete).complete).toBe(true)
  })
  it('still counts as complete without the optional target course', () => {
    const progress = profileProgress({ ...complete, targetCourse: null })
    expect(progress.complete).toBe(true)
    expect(progress.steps.find(step => step.key === 'target')?.done).toBe(false)
  })
  it('points to the preferences tab first when courses are missing', () => {
    const progress = profileProgress({ ...complete, coursesTaken: [], availability: [] })
    expect(progress.complete).toBe(false)
    expect(progress.nextStep?.tab).toBe('preferences')
  })
  it('points to the availability tab when only weekly times are missing', () => {
    const progress = profileProgress({ ...complete, availability: [] })
    expect(progress.nextStep?.tab).toBe('availability')
  })
})
