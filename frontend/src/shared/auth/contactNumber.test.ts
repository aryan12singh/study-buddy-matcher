import { describe, expect, it } from 'vitest'
import { normalizeContactNumber, validateContactNumber } from './contactNumber'
import { validateAccountFields } from './validation'
import { toRequest, validateProfile } from '../../features/profile/profileForm'
import type { ProfileForm } from '../../features/profile/profileForm'

const account = { email: 'student@example.test', password: 'contact-test-password', name: 'Student', school: 'SCIS', programme: 'IS', yearOfStudy: '2', contactNumber: '+12025550100' }
const profile: ProfileForm = { ...account, courseIds: [1], targetCourseId: '', preferredStudyMode: 'ONLINE', groupSizePreference: 'EITHER', studyGoals: [] }

describe('shared contact format contract', () => {
  it('accepts local Singapore prefixes, common separators, and both international digit bounds in every form', () => {
    const accepted = [
      ['6 (123) 45-67', '+6561234567'], ['8.123.4567', '+6581234567'], ['\t9 123 4567\n', '+6591234567'], ['+65 9123 4567', '+6591234567'],
      [' +1 (202) 555-0100 ', '+12025550100'], ['+1234567', '+1234567'], ['+123456789012345', '+123456789012345'],
      [' '.repeat(239) + '+123456789012345', '+123456789012345']
    ]
    for (const [contactNumber, saved] of accepted) {
      expect(validateContactNumber(contactNumber), contactNumber).toBeUndefined()
      expect(normalizeContactNumber(contactNumber)).toBe(saved)
      expect(validateAccountFields({ ...account, contactNumber }, true, true).contactNumber).toBeUndefined()
      expect(validateAccountFields({ ...account, contactNumber }, true, false).contactNumber).toBeUndefined()
      expect(validateProfile({ ...profile, contactNumber }).contactNumber).toBeUndefined()
      expect(toRequest({ ...profile, contactNumber }).contactNumber).toBe(saved)
    }
  })
  it('rejects invalid local and international shapes, extensions, letters and oversized raw input in every form', () => {
    const invalid = ['---()...', '12345678', '71234567', '9123456', '912345678', '+123456', '+1234567890123456',
      '+01234567', '65 9123 4567', '++6591234567', '659+1234567', '9123ABCD', '+12025550100 ext 2', '+12025550100x2',
      '+1/202/555/0100', '９１２３４５６７', ' '.repeat(240) + '+123456789012345',
      '+65999999', '+65 9123 456', '+65 9123 45678', '+65 1234 5678']
    for (const contactNumber of invalid) {
      expect(validateContactNumber(contactNumber), contactNumber).toBeDefined()
      expect(validateAccountFields({ ...account, contactNumber }, true, true).contactNumber).toBeDefined()
      expect(validateAccountFields({ ...account, contactNumber }, true, false).contactNumber).toBeDefined()
      expect(validateProfile({ ...profile, contactNumber }).contactNumber).toBeDefined()
    }
  })
  it('requires contact for registration and own profile while permitting an omitted admin replacement', () => {
    for (const contactNumber of ['', ' \t ']) {
      expect(validateAccountFields({ ...account, contactNumber }, true, true).contactNumber).toBeDefined()
      expect(validateProfile({ ...profile, contactNumber }).contactNumber).toBeDefined()
      expect(validateAccountFields({ ...account, contactNumber }, true, false).contactNumber).toBeUndefined()
    }
    expect(validateAccountFields({ ...account, contactNumber: 'unused contact' }, false, true).contactNumber).toBeUndefined()
  })
})
