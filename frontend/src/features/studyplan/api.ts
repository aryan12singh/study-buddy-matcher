import { api } from '../../shared/api/client'
import type { Course, StudyGoal } from '../../shared/api/types'

export type StudyPlanScope = 'PERSONAL' | 'SESSION'
export type StudyMaterialSource = 'UPLOAD' | 'PASTE'

/** One line of the student's plan list: progress counts instead of the entries. */
export type StudyPlanSummary = {
  id: number; scope: StudyPlanScope; course: Course; firstBlockDate: string; blockCount: number
  examDate: string | null; studyGoal: StudyGoal | null; entryCount: number; completedCount: number; createdAt: string
}

/** Shown wherever Gemini's roadmap appears. */
export const AI_PLAN_LABEL = 'Roadmap written by Gemini; it may be inaccurate.'

export async function listStudyPlans(signal?: AbortSignal) {
  return (await api.get<StudyPlanSummary[]>('/study-plans', { signal })).data
}
