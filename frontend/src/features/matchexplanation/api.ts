import { api } from '../../shared/api/client'
import type { MatchSearch } from '../matching/api'

export type ExplanationSource = 'AI' | 'STANDARD'
export type MatchExplanation = { candidateId: number; text: string; source: ExplanationSource }

/** Sends the search context that produced the card, so the explanation describes the same score. */
export async function fetchMatchExplanation(candidateId: number, search: MatchSearch, signal?: AbortSignal) {
  const { courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality } = search
  const params = Object.fromEntries(Object.entries({ courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality })
    .filter(([, value]) => value !== undefined))
  return (await api.get<MatchExplanation>(`/matches/${candidateId}/explanation`, { params, signal })).data
}
