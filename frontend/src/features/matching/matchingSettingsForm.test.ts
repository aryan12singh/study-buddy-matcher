import { describe, expect, it } from 'vitest'
import type { MatchingCriterion } from './api'
import { rebalance, validateWeights, weightTotal } from './matchingSettingsForm'

const saved: Record<MatchingCriterion, string> = { COURSE: '0.3', AVAILABILITY: '0.3', STUDY_MODE: '0.2', STUDY_GOAL: '0.1', GROUP_SIZE: '0.1' }

describe('rebalancing matching weights', () => {
  it('keeps the total at 1 and the other weights in proportion', () => {
    const next = rebalance(saved, 'STUDY_GOAL', '0.5')
    // 0.5 left for 3 : 3 : 2 : 1 is exactly 16.7, 16.7, 11.1 and 5.6 hundredths; rounded down that is 0.48,
    // and the two hundredths left go to the shares that lost the most, keeping the equal ones equal.
    expect(next).toEqual({ COURSE: '0.17', AVAILABILITY: '0.17', STUDY_MODE: '0.11', STUDY_GOAL: '0.5', GROUP_SIZE: '0.05' })
    expect(weightTotal(next)).toBeCloseTo(1)
  })

  it('shares the remainder equally when every other weight is zero', () => {
    const allCourse = { COURSE: '1', AVAILABILITY: '0', STUDY_MODE: '0', STUDY_GOAL: '0', GROUP_SIZE: '0' }
    const next = rebalance(allCourse, 'COURSE', '0.6')
    expect(next).toEqual({ COURSE: '0.6', AVAILABILITY: '0.1', STUDY_MODE: '0.1', STUDY_GOAL: '0.1', GROUP_SIZE: '0.1' })
  })

  it('sets every other weight to zero when one takes the whole score', () => {
    expect(weightTotal(rebalance(saved, 'COURSE', '1'))).toBe(1)
    expect(rebalance(saved, 'COURSE', '1').AVAILABILITY).toBe('0')
  })

  it('leaves the others alone while a value is half typed or invalid', () => {
    for (const text of ['', '0.', '-1', '10', 'abc']) {
      expect(rebalance(saved, 'COURSE', text)).toEqual({ ...saved, COURSE: text })
    }
  })

  it('still rejects weights that do not add up to 1, as the backend does', () => {
    expect(validateWeights({ ...saved, COURSE: '0.5' }).weights).toBe('The weights must add up to 1. They add up to 1.2.')
    expect(validateWeights(saved)).toEqual({})
  })
})
