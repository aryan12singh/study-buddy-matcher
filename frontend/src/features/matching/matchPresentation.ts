import type { DayOfWeek, WeeklySlot } from '../../shared/api/types'
import { WEEK_DAYS } from '../../shared/api/types'
import type { MatchingCriterion } from './api'

export const CRITERION_LABELS: Record<MatchingCriterion, string> = {
  COURSE: 'Course', AVAILABILITY: 'Availability', STUDY_MODE: 'Study mode', STUDY_GOAL: 'Study goal', GROUP_SIZE: 'Group size'
}

/**
 * A short word for one criterion's score. Availability shows the shared hours instead,
 * because "3 h" says more than a word. A preference either student left empty is
 * "Not shared" rather than a score, so missing data never reads as partial agreement.
 */
export function criterionVerdict(criterion: MatchingCriterion, score: number, sharedHours: number, known = true) {
  if (!known) return 'Not shared'
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
