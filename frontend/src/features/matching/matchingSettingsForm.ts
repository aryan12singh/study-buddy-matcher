import type { MatchingConfig, MatchingCriterion, MatchingStrategyType } from './api'

export const CRITERIA: { key: MatchingCriterion; label: string }[] = [
  { key: 'COURSE', label: 'Course' },
  { key: 'AVAILABILITY', label: 'Availability overlap' },
  { key: 'STUDY_MODE', label: 'Study mode' },
  { key: 'STUDY_GOAL', label: 'Study goals' },
  { key: 'GROUP_SIZE', label: 'Group size' }
]

export const STRATEGIES: { value: MatchingStrategyType; label: string; description: string }[] = [
  { value: 'BALANCED', label: 'Balanced', description: 'Ranks by the weighted total of every criterion.' },
  { value: 'AVAILABILITY_FIRST', label: 'Availability first', description: 'Ranks by shared free time, then by the weighted total of the other criteria.' },
  { value: 'COURSE_FIRST', label: 'Course first', description: 'Ranks by course match, then by the weighted total of the other criteria.' }
]

/**
 * One strategy's weights, kept as text while editing so a half-typed value is
 * not lost. Only the criteria the strategy weights are present.
 */
export type StrategyWeights = Partial<Record<MatchingCriterion, string>>
export type MatchingSettingsForm = { activeStrategy: MatchingStrategyType; weights: Record<MatchingStrategyType, StrategyWeights> }

export function toForm(config: MatchingConfig): MatchingSettingsForm {
  const weights = {} as Record<MatchingStrategyType, StrategyWeights>
  STRATEGIES.forEach(({ value }) => {
    weights[value] = {}
    // Shown to two decimal places, a whole percentage, however precisely the server stored it.
    weightedCriteria(config.weights[value]).forEach(({ key }) => { weights[value][key] = String(Math.round((config.weights[value][key] ?? 0) * 100) / 100) })
  })
  return { activeStrategy: config.activeStrategy, weights }
}

export function toConfig(form: MatchingSettingsForm): MatchingConfig {
  const weights = {} as MatchingConfig['weights']
  STRATEGIES.forEach(({ value }) => {
    weights[value] = {}
    weightedCriteria(form.weights[value]).forEach(({ key }) => { weights[value][key] = Number(form.weights[value][key]) })
  })
  return { activeStrategy: form.activeStrategy, weights }
}

/** The criteria a strategy weights, in display order. */
export function weightedCriteria(weights: Partial<Record<MatchingCriterion, unknown>>) {
  return CRITERIA.filter(({ key }) => key in weights)
}

/** The criteria a strategy leaves out because it ranks by them first. */
export function rankedFirstCriteria(weights: StrategyWeights) {
  return CRITERIA.filter(({ key }) => !(key in weights))
}

function parsedWeight(text: string | undefined) {
  return text === undefined || text.trim() === '' ? NaN : Number(text)
}

/** The same allowance for rounding in typed decimals as the backend's MatchingWeights.SUM_TOLERANCE. */
const SUM_TOLERANCE = 0.001

function isValidWeight(weight: number) {
  return Number.isFinite(weight) && weight >= 0 && weight <= 1
}

/** The sum of the weights, or null while any of them is not a number from 0 to 1. */
export function weightTotal(weights: StrategyWeights): number | null {
  const values = weightedCriteria(weights).map(({ key }) => parsedWeight(weights[key]))
  return values.every(isValidWeight) ? values.reduce((sum, value) => sum + value, 0) : null
}

export function addsUpToOne(total: number) {
  return Math.abs(total - 1) <= SUM_TOLERANCE
}

/** Mirrors the backend rules: every weight is a number from 0 to 1, and together they add up to 1. */
export function validateWeights(weights: StrategyWeights): Record<string, string> {
  const errors: Record<string, string> = {}
  weightedCriteria(weights).forEach(({ key, label }) => {
    if (!isValidWeight(parsedWeight(weights[key]))) errors[`weight-${key}`] = `Enter a weight for ${label.toLowerCase()} from 0 to 1.`
  })
  const total = weightTotal(weights)
  if (total !== null && !addsUpToOne(total)) {
    errors.weights = `The weights must add up to 1. They add up to ${formatTotal(total)}.`
  }
  return errors
}

/**
 * Sets one weight and scales the strategy's other weights to fill what is left,
 * to two decimal places (a whole percentage),
 * keeping their proportions to each other, so they always add up to 1. If the
 * others are all zero, the remainder is shared equally. A value that is not yet
 * a number from 0 to 1, such as a half-typed "0.", is kept as typed and nothing
 * else moves.
 */
export function rebalance(weights: StrategyWeights, changed: MatchingCriterion, text: string): StrategyWeights {
  const next = { ...weights, [changed]: text }
  const value = parsedWeight(text)
  if (!isValidWeight(value) || text.trim().endsWith('.')) return next
  const others = weightedCriteria(weights).map(({ key }) => key).filter(key => key !== changed)
  if (!others.length) return next
  const current = others.map(key => { const weight = parsedWeight(weights[key]); return isValidWeight(weight) ? weight : 0 })
  const othersTotal = current.reduce((sum, weight) => sum + weight, 0)
  // Work in whole hundredths: round each share down, then hand the hundredths left over
  // to the shares that lost the most in rounding, so the total is exactly 1.
  const remainder = Math.round((1 - value) * 100)
  const exact = current.map(weight => othersTotal > 0 ? weight / othersTotal * remainder : remainder / others.length)
  const hundredths = exact.map(Math.floor)
  const leftOver = remainder - hundredths.reduce((sum, share) => sum + share, 0)
  exact.map((share, index) => ({ index, lost: share - hundredths[index] }))
    .sort((left, right) => right.lost - left.lost)
    .slice(0, leftOver)
    .forEach(({ index }) => { hundredths[index] += 1 })
  others.forEach((key, index) => { next[key] = String(hundredths[index] / 100) })
  return next
}

export function formatTotal(total: number) {
  return String(Math.round(total * 100) / 100)
}
