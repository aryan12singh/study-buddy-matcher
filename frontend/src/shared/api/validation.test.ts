import { describe, expect, it } from 'vitest'
import { validateAccountFields } from '../auth/validation'
import { validateSlots } from './schedules'
import { REFRESH_INTERVAL_MS } from './useResource'

const student = { email: 'student@example.test', password: 'a-long-passphrase', name: 'Student', school: 'SCIS', programme: 'IS', yearOfStudy: '1', contactNumber: '+65 9999 1111' }
describe('form contract limits', () => {
  it('honors BCrypt byte length for unicode passwords', () => {
    expect(validateAccountFields({ ...student, password: '界'.repeat(25) }, true, true).password).toBeDefined()
    expect(validateAccountFields({ ...student, password: '界'.repeat(24) }, true, true).password).toBeUndefined()
  })
  it('allows blank contact replacement on edit and enforces public field widths', () => {
    expect(validateAccountFields({ ...student, contactNumber: '' }, true, false)).toEqual({})
    expect(validateAccountFields({ ...student, name: 'a'.repeat(256) }, true, false).name).toBeDefined()
  })
  it('rejects equal time boundaries regardless of precision and permits optional/overlapping slots', () => {
    expect(validateSlots([])).toBeUndefined()
    expect(validateSlots([{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '09:00:00' }])).toBeDefined()
    expect(validateSlots([{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '11:00' }, { dayOfWeek: 'MONDAY', startTime: '10:00', endTime: '12:00' }])).toBeUndefined()
  })
  it('bounds background refresh to 15 seconds through 5 minutes', () => {
    expect(REFRESH_INTERVAL_MS).toBeGreaterThanOrEqual(15000)
    expect(REFRESH_INTERVAL_MS).toBeLessThanOrEqual(300000)
  })
})
