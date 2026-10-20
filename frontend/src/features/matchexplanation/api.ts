import { api } from '../../shared/api/client'
import type { MatchSearch } from '../matching/api'

export type ExplanationSource = 'AI' | 'STANDARD'
export type MatchExplanation = { candidateId: number; evaluationId: string; text: string; source: ExplanationSource }

/** Sends the server-issued card identity and search context; the server rejects stale evaluations. */
export async function fetchMatchExplanation(candidateId: number, evaluationId: string, search: MatchSearch, signal?: AbortSignal) {
  const { courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality } = search
  const params = Object.fromEntries(Object.entries({ evaluationId, courseId, studyGoal, studyMode, groupSize, minSharedHours, minQuality })
    .filter(([, value]) => value !== undefined))
  return (await api.get<MatchExplanation>(`/matches/${candidateId}/explanation`, { params, signal })).data
}
