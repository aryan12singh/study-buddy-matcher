import { api } from '../../shared/api/client'
import type { StudyGoal, StudyMode, WeeklySlot } from '../../shared/api/types'
import type { Relationship } from '../students/api'

export type MatchingStrategyType = 'BALANCED' | 'AVAILABILITY_FIRST' | 'COURSE_FIRST'
export type MatchingCriterion = 'COURSE' | 'AVAILABILITY' | 'STUDY_MODE' | 'STUDY_GOAL' | 'GROUP_SIZE'
export type MatchingSettings = {
  activeStrategy: MatchingStrategyType
  weights: Record<MatchingStrategyType, Record<MatchingCriterion, number>>
  enabledCriteria: MatchingCriterion[]
  minScore: number
}
export type MatchingConfig = MatchingSettings & { version: number; usingDefaults: boolean }
export type MatchingConfigUpdate = MatchingSettings & { expectedVersion: number }
export type MatchSort = 'BEST_MATCH' | 'MOST_SHARED_HOURS' | 'SAME_YEAR_FIRST'

export type MatchQuality = 'STRONG' | 'GOOD' | 'FAIR'
export type MatchSearch = {
  courseId?: number
  studyGoal?: StudyGoal
  studyMode?: 'IN_PERSON' | 'ONLINE'
  groupSize?: 'ONE_TO_ONE' | 'SMALL_GROUP'
  minSharedHours?: number
  minQuality?: 'GOOD' | 'STRONG'
  sort?: MatchSort
  limit?: number
}
export type MatchResult = {
  studentId: number
  name: string
  school: string
  programme: string
  yearOfStudy: number
  studyMode: StudyMode | null
  studyGoals: StudyGoal[]
  quality: MatchQuality
  totalScore: number
  explanation: string
  /** Opaque server identity of this evaluation; required to request a matching explanation. */
  evaluationId: string
  relevantCourse: { id: number; code: string; name: string } | null
  /** Every criterion from 0 to 1, in the same order each time. `known` is false when either student left it empty. */
  breakdown: { criterion: MatchingCriterion; score: number; enabled: boolean; known: boolean }[]
  sharedHoursPerWeek: number
  /** The weekly blocks when both students are free, Monday first. */
  sharedSlots: WeeklySlot[]
  /** Where the searcher already stands with this student; decides whether a request can be sent. */
  relationship: Exclude<Relationship['state'], 'SELF'>
}

export async function getMatchingConfig(signal?: AbortSignal) { return (await api.get<MatchingConfig>('/admin/matching-config', { signal })).data }
export async function updateMatchingConfig(config: MatchingConfigUpdate) { return (await api.put<MatchingConfig>('/admin/matching-config', config)).data }
export async function restoreMatchingDefaults(expectedVersion: number) {
  return (await api.post<MatchingConfig>('/admin/matching-config/restore-defaults', { expectedVersion })).data
}
/** Ranked and bounded by the server using one saved configuration version. */
export type MatchSearchResult = { strategy: MatchingStrategyType; configurationVersion: number; sort: MatchSort; totalMatches: number; limit: number; matches: MatchResult[] }

export async function searchMatches(search: MatchSearch, signal?: AbortSignal) { return (await api.get<MatchSearchResult>('/matches', { params: search, signal })).data }
