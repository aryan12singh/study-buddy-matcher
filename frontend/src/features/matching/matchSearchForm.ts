import type { StudyGoal } from '../../shared/api/types'
import type { MatchQuality, MatchSearch } from './api'

/** Select values are kept as text; an empty string means "any". */
export type MatchSearchForm = { courseId: string; studyGoal: string; studyMode: string; groupSize: string; minSharedHours: string; minQuality: string }

export const EMPTY_SEARCH: MatchSearchForm = { courseId: '', studyGoal: '', studyMode: '', groupSize: '', minSharedHours: '', minQuality: '' }

export const STUDY_MODE_FILTERS = [
  { value: '', label: 'Any study mode' },
  { value: 'IN_PERSON', label: 'In person' },
  { value: 'ONLINE', label: 'Online' }
]

export const GROUP_SIZE_FILTERS = [
  { value: '', label: 'Any group size' },
  { value: 'ONE_TO_ONE', label: 'One-to-one (2 people)' },
  { value: 'SMALL_GROUP', label: 'Small group (3 or more)' }
]

export const SHARED_HOURS_FILTERS = [
  { value: '', label: 'Any' },
  { value: '1', label: 'At least 1 hour a week' },
  { value: '2', label: 'At least 2 hours a week' },
  { value: '4', label: 'At least 4 hours a week' }
]

export const QUALITY_FILTERS = [
  { value: '', label: 'Any' },
  { value: 'GOOD', label: 'Good or better' },
  { value: 'STRONG', label: 'Strong only' }
]

export const QUALITY_LABELS: Record<MatchQuality, string> = { STRONG: 'Strong match', GOOD: 'Good match', FAIR: 'Fair match' }
/** Green, amber and grey, so the three labels can be told apart at a glance and not only by their text. */
export const QUALITY_TONES: Record<MatchQuality, 'good' | 'pending' | 'neutral'> = { STRONG: 'good', GOOD: 'pending', FAIR: 'neutral' }

/** A search starts from a course or a study goal; the other filters are optional. */
export function validateSearch(form: MatchSearchForm): Record<string, string> {
  return form.courseId || form.studyGoal ? {} : { 'match-course': 'Choose a course or a study goal to search.' }
}

export function toSearch(form: MatchSearchForm): MatchSearch {
  return {
    ...(form.courseId ? { courseId: Number(form.courseId) } : {}),
    ...(form.studyGoal ? { studyGoal: form.studyGoal as StudyGoal } : {}),
    ...(form.studyMode ? { studyMode: form.studyMode as MatchSearch['studyMode'] } : {}),
    ...(form.groupSize ? { groupSize: form.groupSize as MatchSearch['groupSize'] } : {}),
    ...(form.minSharedHours ? { minSharedHours: Number(form.minSharedHours) } : {}),
    ...(form.minQuality ? { minQuality: form.minQuality as MatchSearch['minQuality'] } : {})
  }
}

export function sharedHoursText(hours: number) {
  if (hours <= 0) return 'No shared free time in your weekly schedules'
  return `${hours} ${hours === 1 ? 'hour' : 'hours'} of shared free time a week`
}
