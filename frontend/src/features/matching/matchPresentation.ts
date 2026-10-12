import type { DayOfWeek, WeeklySlot } from '../../shared/api/types'
import { WEEK_DAYS } from '../../shared/api/types'
import type { MatchingCriterion, MatchQuality, MatchResult } from './api'

export const CRITERION_LABELS: Record<MatchingCriterion, string> = {
  COURSE: 'Course', AVAILABILITY: 'Availability', STUDY_MODE: 'Study mode', STUDY_GOAL: 'Study goal', GROUP_SIZE: 'Group size'
}

/**
 * A short word for one criterion's score. Availability shows the shared hours instead,
 * because "3 h" says more than a word. A score of 0.5 also means a preference is unset.
 */
export function criterionVerdict(criterion: MatchingCriterion, score: number, sharedHours: number) {
  if (criterion === 'AVAILABILITY') return `${sharedHours} h`
  if (score >= 1) return criterion === 'COURSE' ? 'Same course' : 'Matches'
  return score > 0 ? 'Partly' : 'Differs'
}

/** Parts of the day the heatmap groups shared time into, as minutes from midnight. */
export const DAY_PARTS = [
  { label: 'Morning', from: 0, to: 12 * 60 },
  { label: 'Afternoon', from: 12 * 60, to: 17 * 60 },
  { label: 'Evening', from: 17 * 60, to: 24 * 60 }
]

function minutesOf(time: string) {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

/** Shared minutes for each day and part of the day; a slot spanning two parts counts in both. */
export function sharedMinutesByDayPart(slots: WeeklySlot[]): Record<DayOfWeek, number[]> {
  const grid = Object.fromEntries(WEEK_DAYS.map(day => [day, DAY_PARTS.map(() => 0)])) as Record<DayOfWeek, number[]>
  for (const slot of slots) {
    // An end of 00:00 would be midnight at the end of the day.
    const start = minutesOf(slot.startTime), end = minutesOf(slot.endTime) || 24 * 60
    DAY_PARTS.forEach((part, index) => {
      grid[slot.dayOfWeek][index] += Math.max(0, Math.min(end, part.to) - Math.max(start, part.from))
    })
  }
  return grid
}

/** Heatmap shade: none, under an hour, under two hours, two hours or more. */
export function heatLevel(minutes: number) {
  if (minutes <= 0) return 0
  return minutes < 60 ? 1 : minutes < 120 ? 2 : 3
}

const QUALITY_ORDER: MatchQuality[] = ['STRONG', 'GOOD', 'FAIR']

export const QUALITY_GROUP_LABELS: Record<MatchQuality, string> = { STRONG: 'Strong matches', GOOD: 'Good matches', FAIR: 'Fair matches' }

/**
 * Splits ranked matches into strong, good and fair groups, leaving out empty ones.
 * Each group keeps the order the admin's strategy gave it.
 */
export function groupByQuality(matches: MatchResult[]) {
  return QUALITY_ORDER
    .map(quality => ({ quality, matches: matches.filter(match => match.quality === quality) }))
    .filter(group => group.matches.length > 0)
}
