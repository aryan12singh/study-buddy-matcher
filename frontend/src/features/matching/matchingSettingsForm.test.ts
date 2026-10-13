import { describe, expect, it } from 'vitest'
import type { MatchingConfig, MatchingCriterion } from './api'
import { formatPercentage, formatTotal, rebalance, toConfig, toForm, validateWeights, weightTotal } from './matchingSettingsForm'

const saved: Record<MatchingCriterion, string> = { COURSE: '0.3', AVAILABILITY: '0.3', STUDY_MODE: '0.2', STUDY_GOAL: '0.1', GROUP_SIZE: '0.1' }

describe('matching settings form precision', () => {
  const config: MatchingConfig = {
    activeStrategy: 'BALANCED', minScore: 0, version: 1, usingDefaults: true,
    enabledCriteria: ['COURSE', 'AVAILABILITY', 'STUDY_MODE', 'STUDY_GOAL', 'GROUP_SIZE'],
    weights: {
      BALANCED: { COURSE: 0.004, AVAILABILITY: 0.996, STUDY_MODE: 0, STUDY_GOAL: 0, GROUP_SIZE: 0 },
      AVAILABILITY_FIRST: { AVAILABILITY: 0, COURSE: 1 / 3, STUDY_MODE: 1 / 3, STUDY_GOAL: 1 / 3, GROUP_SIZE: 0 },
      COURSE_FIRST: { COURSE: 0, AVAILABILITY: 0.25, STUDY_MODE: 0.25, STUDY_GOAL: 0.25, GROUP_SIZE: 0.25 }
    }
  }

  it('round trips saved fractional weights without changing them', () => {
    const form = toForm(config)
    expect(form.weights.BALANCED.COURSE).toBe('0.004')
    expect(form.weights.BALANCED.AVAILABILITY).toBe('0.996')
    expect(form.weights.AVAILABILITY_FIRST.COURSE).toBe(String(1 / 3))
    expect(toConfig(form)).toEqual({ activeStrategy: config.activeStrategy, weights: config.weights, minScore: config.minScore, enabledCriteria: config.enabledCriteria })
    Object.values(form.weights).forEach(weights => { expect(validateWeights(weights)).toEqual({}) })
  })

  it('shows the actual invalid total even when the difference is less than one hundredth', () => {
    expect(formatTotal(1.004)).toBe('1.004')
  })

  it('keeps fractional percentage hints readable without changing the weight', () => {
    expect(formatPercentage('0.004')).toBe('0.4%')
    expect(formatPercentage(String(1 / 3))).toBe('33.3%')
    expect(formatPercentage('0.000004')).toBe('0.0004%')
  })
})

describe('rebalancing matching weights', () => {
  it('keeps the total at 1 and the other weights in proportion', () => {
    const next = rebalance(saved, 'STUDY_GOAL', '0.5')
    expect(next.STUDY_GOAL).toBe('0.5')
    expect(Number(next.COURSE)).toBeCloseTo(1 / 6)
    expect(Number(next.AVAILABILITY)).toBeCloseTo(1 / 6)
    expect(Number(next.STUDY_MODE)).toBeCloseTo(1 / 9)
    expect(Number(next.GROUP_SIZE)).toBeCloseTo(1 / 18)
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

  it('preserves a fine-grained edit and scales the remainder without whole-percent rounding', () => {
    const next = rebalance(saved, 'COURSE', '0.004')
    expect(next.COURSE).toBe('0.004')
    expect(Number(next.AVAILABILITY)).toBeCloseTo(0.996 * 3 / 7, 14)
    expect(Number(next.STUDY_MODE)).toBeCloseTo(0.996 * 2 / 7, 14)
    expect(Number(next.STUDY_GOAL)).toBeCloseTo(0.996 / 7, 14)
    expect(Number(next.GROUP_SIZE)).toBeCloseTo(0.996 / 7, 14)
    expect(weightTotal(next)).toBeCloseTo(1, 14)
    expect(validateWeights(next)).toEqual({})
  })

  it('retains a small existing criterion when another weight changes', () => {
    const next = rebalance({ ...saved, COURSE: '0.004', AVAILABILITY: '0.996', STUDY_MODE: '0', STUDY_GOAL: '0', GROUP_SIZE: '0' }, 'AVAILABILITY', '0.995')
    expect(next.AVAILABILITY).toBe('0.995')
    expect(Number(next.COURSE)).toBeCloseTo(0.005, 14)
    expect(Number(next.COURSE)).toBeGreaterThan(0)
    expect(weightTotal(next)).toBeCloseTo(1, 14)
    expect(validateWeights(next)).toEqual({})
  })

  it('shares a fractional remainder equally when the other criteria are zero', () => {
    const next = rebalance({ COURSE: '1', AVAILABILITY: '0', STUDY_MODE: '0', STUDY_GOAL: '0' }, 'COURSE', '0.004')
    expect(next.COURSE).toBe('0.004')
    expect(Number(next.AVAILABILITY)).toBeCloseTo(0.332, 14)
    expect(next.STUDY_MODE).toBe(next.AVAILABILITY)
    expect(next.STUDY_GOAL).toBe(next.AVAILABILITY)
    expect(validateWeights(next)).toEqual({})
  })

  it('leaves the others alone while a value is half typed or invalid', () => {
    for (const text of ['', '0.', '0.004.', '1e', '-1', '10', 'abc', 'Infinity']) {
      expect(rebalance(saved, 'COURSE', text)).toEqual({ ...saved, COURSE: text })
    }
  })

  it('still rejects weights that do not add up to 1, as the backend does', () => {
    expect(validateWeights({ ...saved, COURSE: '0.5' }).weights).toBe('The weights must add up to 1. They add up to 1.2.')
    expect(validateWeights(saved)).toEqual({})
  })
})
