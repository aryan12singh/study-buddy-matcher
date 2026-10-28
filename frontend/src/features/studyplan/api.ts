import { api } from '../../shared/api/client'
import type { Course, StudyGoal } from '../../shared/api/types'

export type StudyPlanScope = 'PERSONAL' | 'SESSION'
export type StudyMaterialSource = 'UPLOAD' | 'PASTE'

/** One line of the student's plan list: progress counts instead of the entries. */
export type StudyPlanSummary = {
  id: number; scope: StudyPlanScope; course: Course; firstBlockDate: string; blockCount: number
  examDate: string | null; studyGoal: StudyGoal | null; entryCount: number; completedCount: number; createdAt: string
}
/** One topic of a plan, scheduled into week (or meeting) number `sequence`. */
export type StudyPlanEntry = {
  id: number; sequence: number; scheduledDate: string; title: string; plannedMinutes: number; tasks: string[]; completed: boolean
}
export type StudyPlan = {
  id: number; scope: StudyPlanScope; course: Course; sessionId: number | null; firstBlockDate: string; blockCount: number
  minutesPerBlock: number; examDate: string | null; studyGoal: StudyGoal | null; sourceKind: StudyMaterialSource
  sourceFilenames: string[]; sourceTopics: string[]; canManage: boolean; entries: StudyPlanEntry[]
  createdAt: string; updatedAt: string
}

/** What a plan is drafted from: uploaded files, or topics pasted into the form. */
export type StudyMaterial = { kind: 'files'; files: File[] } | { kind: 'topics'; text: string }
export type PersonalPlanInput = { courseId: number; hoursPerWeek: number; examDate: string | null; studyGoal: StudyGoal | null }

/** Shown wherever Gemini's roadmap appears. */
export const AI_PLAN_LABEL = 'Roadmap written by Gemini; it may be inaccurate.'
/** Matches app.ai.assistant.study-plan.max-weeks and max-hours-per-week; the server checks them again. */
export const MAX_WEEKS = 16
export const MAX_HOURS_PER_WEEK = 40
/** Reading several PDFs can take Gemini most of the server's 90-second limit, far past the client's usual timeout. */
const WRITING_TIMEOUT_MS = 120000

export async function listStudyPlans(signal?: AbortSignal) {
  return (await api.get<StudyPlanSummary[]>('/study-plans', { signal })).data
}

export async function getStudyPlan(planId: number, signal?: AbortSignal) {
  return (await api.get<StudyPlan>(`/study-plans/${planId}`, { signal })).data
}

export async function markEntry(planId: number, entryId: number, completed: boolean) {
  return (await api.put<StudyPlan>(`/study-plans/${planId}/entries/${entryId}/progress`, { completed })).data
}

export async function deleteStudyPlan(planId: number) {
  await api.delete(`/study-plans/${planId}`)
}

export async function createStudyPlan(input: PersonalPlanInput, material: StudyMaterial) {
  const form = new FormData()
  form.append('courseId', String(input.courseId))
  form.append('hoursPerWeek', String(input.hoursPerWeek))
  if (input.examDate) form.append('examDate', input.examDate)
  if (input.studyGoal) form.append('studyGoal', input.studyGoal)
  if (material.kind === 'files') material.files.forEach(file => form.append('files', file))
  else form.append('topicsText', material.text)
  return (await api.post<StudyPlan>('/study-plans', form, { timeout: WRITING_TIMEOUT_MS })).data
}
