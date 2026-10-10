import { api } from '../../shared/api/client'
import type { StudyGoal } from '../../shared/api/types'

export type MatchingStrategyType = 'BALANCED' | 'AVAILABILITY_FIRST' | 'COURSE_FIRST'
export type MatchingCriterion = 'COURSE' | 'AVAILABILITY' | 'STUDY_MODE' | 'STUDY_GOAL' | 'GROUP_SIZE'
/** Each strategy's weights; a strategy leaves out the criterion it ranks by first. */
export type MatchingConfig = { activeStrategy: MatchingStrategyType; weights: Record<MatchingStrategyType, Partial<Record<MatchingCriterion, number>>> }

export type MatchQuality = 'STRONG' | 'GOOD' | 'FAIR'
export type MatchSearch = {
  courseId?: number
  studyGoal?: StudyGoal
  studyMode?: 'IN_PERSON' | 'ONLINE'
  groupSize?: 'ONE_TO_ONE' | 'SMALL_GROUP'
  minSharedHours?: number
  minQuality?: 'GOOD' | 'STRONG'
}
export type MatchResult = {
  studentId: number
  name: string
  school: string
  programme: string
  yearOfStudy: number
  quality: MatchQuality
  sharedHoursPerWeek: number
}

export async function getMatchingConfig(signal?: AbortSignal) { return (await api.get<MatchingConfig>('/admin/matching-config', { signal })).data }
export async function updateMatchingConfig(config: MatchingConfig) { return (await api.put<MatchingConfig>('/admin/matching-config', config)).data }
export async function searchMatches(search: MatchSearch, signal?: AbortSignal) { return (await api.get<MatchResult[]>('/matches', { params: search, signal })).data }
