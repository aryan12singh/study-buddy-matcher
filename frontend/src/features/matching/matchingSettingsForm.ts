import type { MatchingConfig, MatchingCriterion, MatchingStrategyType, MatchingSettings } from './api'

export const CRITERIA: { key: MatchingCriterion; label: string }[] = [
  { key: 'COURSE', label: 'Course' },
  { key: 'AVAILABILITY', label: 'Availability overlap' },
  { key: 'STUDY_MODE', label: 'Study mode' },
  { key: 'STUDY_GOAL', label: 'Study goals' },
  { key: 'GROUP_SIZE', label: 'Group size' }
]

export const STRATEGIES: { value: MatchingStrategyType; label: string; description: string }[] = [
  { value: 'BALANCED', label: 'Balanced', description: 'Ranks by the weighted total of enabled criteria.' },
  { value: 'AVAILABILITY_FIRST', label: 'Availability first', description: 'Ranks by availability overlap, then by the weighted total. Falls back to the total if availability is disabled.' },
  { value: 'COURSE_FIRST', label: 'Course first', description: 'Ranks by course match, then by the weighted total. Falls back to the total if course is disabled.' }
]

/**
 * One strategy's weights, kept as text while editing so a half-typed value is
 * not lost. All five criteria are present, including a strategy's priority criterion.
 */
export type StrategyWeights = Partial<Record<MatchingCriterion, string>>
export type MatchingSettingsForm = { activeStrategy: MatchingStrategyType; weights: Record<MatchingStrategyType, StrategyWeights>; enabledCriteria: MatchingCriterion[]; minScore: string }

export function toForm(config: MatchingConfig): MatchingSettingsForm {
  const weights = {} as Record<MatchingStrategyType, StrategyWeights>
  STRATEGIES.forEach(({ value }) => {
    weights[value] = {}
    weightedCriteria(config.weights[value]).forEach(({ key }) => { weights[value][key] = String(config.weights[value][key] ?? 0) })
  })
  return { activeStrategy: config.activeStrategy, weights, enabledCriteria: [...config.enabledCriteria], minScore: String(config.minScore) }
}

export function toConfig(form: MatchingSettingsForm): MatchingSettings {
  const weights = {} as MatchingSettings['weights']
  STRATEGIES.forEach(({ value }) => {
    weights[value] = {} as Record<MatchingCriterion, number>
    weightedCriteria(form.weights[value]).forEach(({ key }) => { weights[value][key] = Number(form.weights[value][key]) })
  })
  return { activeStrategy: form.activeStrategy, weights, enabledCriteria: [...form.enabledCriteria], minScore: Number(form.minScore) }
}

/** The criteria a strategy weights, in display order. */
export function weightedCriteria(weights: Partial<Record<MatchingCriterion, unknown>>) {
  return CRITERIA.filter(({ key }) => key in weights)
}

function parsedWeight(text: string | undefined) {
  return text === undefined || text.trim() === '' ? NaN : Number(text)
}

/** The same allowance for rounding in typed decimals as the backend's MatchingWeights.SUM_TOLERANCE. */
const SUM_TOLERANCE = 0.001
const percentageFormat = new Intl.NumberFormat('en', { style: 'percent', maximumSignificantDigits: 3 })

/** A readable share for the hint; the input and saved weight retain their precision. */
export function formatPercentage(weight: string | undefined) {
  return percentageFormat.format(Number(weight))
}

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
export function validateWeights(weights: StrategyWeights, enabled = CRITERIA.map(criterion => criterion.key)): Record<string, string> {
  const errors: Record<string, string> = {}
  weightedCriteria(weights).forEach(({ key, label }) => {
    if (!isValidWeight(parsedWeight(weights[key]))) errors[`weight-${key}`] = `Enter a weight for ${label.toLowerCase()} from 0 to 1.`
  })
  if (!enabled.length) errors.weights = 'Enable at least one matching criterion.'
  weightedCriteria(weights).filter(({ key }) => !enabled.includes(key)).forEach(({ key }) => {
    if (Number(weights[key]) !== 0) errors[`weight-${key}`] = 'Disabled criteria must have a weight of 0.'
  })
  const total = weightTotal(weights)
  if (enabled.length && total !== null && !addsUpToOne(total)) {
    errors.weights = `The weights must add up to 1. They add up to ${formatTotal(total)}.`
  }
  return errors
}

/**
 * Sets one weight and scales the strategy's other weights to fill what is left,
 * keeping their proportions to each other, so they always add up to 1. If the
 * others are all zero, the remainder is shared equally. A value that is not yet
 * a number from 0 to 1, such as a half-typed "0.", is kept as typed and nothing
 * else moves.
 */
export function rebalance(weights: StrategyWeights, changed: MatchingCriterion, text: string, enabled = CRITERIA.map(criterion => criterion.key)): StrategyWeights {
  const next = { ...weights, [changed]: text }
  const value = parsedWeight(text)
  if (!isValidWeight(value) || text.trim().endsWith('.')) return next
  const others = weightedCriteria(weights).map(({ key }) => key).filter(key => key !== changed && enabled.includes(key))
  if (!others.length) return next
  const current = others.map(key => { const weight = parsedWeight(weights[key]); return isValidWeight(weight) ? weight : 0 })
  const othersTotal = current.reduce((sum, weight) => sum + weight, 0)
  const remainder = 1 - value
  others.forEach((key, index) => {
    next[key] = String(othersTotal > 0 ? current[index] / othersTotal * remainder : remainder / others.length)
  })
  return next
}

export function formatTotal(total: number) {
  // Totals display through the validation tolerance without floating-point
  // noise. This does not change the precision of individual weights.
  return addsUpToOne(total) ? '1' : String(Number(total.toFixed(-Math.log10(SUM_TOLERANCE))))
}

/** Changing enabled criteria adjusts every strategy, preserving the remaining proportions. */
export function toggleCriterion(form: MatchingSettingsForm, criterion: MatchingCriterion): MatchingSettingsForm {
  const enabledCriteria = form.enabledCriteria.includes(criterion)
    ? form.enabledCriteria.filter(key => key !== criterion)
    : CRITERIA.map(({ key }) => key).filter(key => key === criterion || form.enabledCriteria.includes(key))
  const weights = { ...form.weights }
  STRATEGIES.forEach(({ value }) => {
    const current = form.weights[value]
    const total = enabledCriteria.reduce((sum, key) => sum + (isValidWeight(parsedWeight(current[key])) ? Number(current[key]) : 0), 0)
    weights[value] = Object.fromEntries(CRITERIA.map(({ key }) => [key, !enabledCriteria.includes(key) ? '0'
      : String(total > 0 ? (isValidWeight(parsedWeight(current[key])) ? Number(current[key]) : 0) / total : 1 / enabledCriteria.length)]))
  })
  return { ...form, enabledCriteria, weights }
}

export function validateSettings(form: MatchingSettingsForm): Record<string, string> {
  const errors = validateWeights(form.weights[form.activeStrategy], form.enabledCriteria)
  const otherInvalid = STRATEGIES.find(({ value }) => value !== form.activeStrategy && Object.keys(validateWeights(form.weights[value], form.enabledCriteria)).length)
  if (otherInvalid && !errors.weights) errors.weights = `Check the ${otherInvalid.label} weights: enabled criteria must have valid weights that add up to 1.`
  if (!isValidWeight(parsedWeight(form.minScore))) errors.minScore = 'Enter a minimum compatibility score from 0 to 1.'
  return errors
}
