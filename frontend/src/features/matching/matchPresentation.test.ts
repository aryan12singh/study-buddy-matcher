import { describe, expect, it } from 'vitest'
import { criterionVerdict, heatLevel, sharedMinutesByDayPart } from './matchPresentation'

describe('sharedMinutesByDayPart', () => {
  it('splits a slot across the parts of the day it spans', () => {
    const grid = sharedMinutesByDayPart([{ dayOfWeek: 'WEDNESDAY', startTime: '11:00:00', endTime: '13:30:00' }])
    expect(grid.WEDNESDAY).toEqual([60, 90, 0])
    expect(grid.MONDAY).toEqual([0, 0, 0])
  })

  it('treats an end of midnight as the end of the day', () => {
    expect(sharedMinutesByDayPart([{ dayOfWeek: 'FRIDAY', startTime: '22:00', endTime: '00:00' }]).FRIDAY).toEqual([0, 0, 120])
  })
})

describe('heatLevel', () => {
  it('shades by how long the shared time is', () => {
    expect([0, 30, 60, 119, 120].map(heatLevel)).toEqual([0, 1, 2, 2, 3])
  })
})

describe('criterionVerdict', () => {
  it('describes a score in words, and availability in hours', () => {
    expect(criterionVerdict('COURSE', 1, 0)).toBe('Same course')
    expect(criterionVerdict('STUDY_MODE', 1, 0)).toBe('Matches')
    expect(criterionVerdict('STUDY_GOAL', 0.5, 0)).toBe('Partly')
    expect(criterionVerdict('GROUP_SIZE', 0, 0)).toBe('Differs')
    expect(criterionVerdict('AVAILABILITY', 0.2, 3.5)).toBe('3.5 h')
  })
})
