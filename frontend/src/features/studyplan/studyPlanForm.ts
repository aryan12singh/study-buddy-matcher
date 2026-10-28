import type { Course, StudyGoal } from '../../shared/api/types'
import { STUDY_GOALS } from '../../shared/api/types'
import { checkFiles, MAX_NOTES_CHARACTERS } from '../quiz/api'
import { MAX_HOURS_PER_WEEK, MAX_WEEKS } from './api'
import type { PersonalPlanInput, SessionPlanInput, StudyMaterial } from './api'
import { addDays } from './planDates'

/** The material half of either plan form: files to upload or topics to paste. */
export type MaterialForm = { materialKind: StudyMaterial['kind']; files: File[]; topicsText: string }

/** The personal plan form as typed: every field a string until it is checked. */
export type StudyPlanForm = MaterialForm & { courseId: string; hoursPerWeek: string; examDate: string; studyGoal: string }

/** The session plan form; the meetings and their length come from the session. */
export type SessionPlanForm = MaterialForm & { courseId: string; studyGoal: string }

export const EMPTY_MATERIAL: MaterialForm = { materialKind: 'files', files: [], topicsText: '' }

/**
 * An empty form, filled in from the details Pixel heard (`?courseId=4&examDate=2026-11-20&hoursPerWeek=6`).
 * A course that does not exist is left out rather than guessed.
 */
export function initialStudyPlanForm(search: URLSearchParams, courses: Course[]): StudyPlanForm {
  const courseId = search.get('courseId') ?? ''
  return {
    courseId: courses.some(course => String(course.id) === courseId) ? courseId : '',
    hoursPerWeek: /^\d+$/.test(search.get('hoursPerWeek') ?? '') ? search.get('hoursPerWeek')! : '',
    examDate: /^\d{4}-\d{2}-\d{2}$/.test(search.get('examDate') ?? '') ? search.get('examDate')! : '',
    studyGoal: '', ...EMPTY_MATERIAL,
  }
}

/** The last exam date a plan can reach: a plan covers at most MAX_WEEKS weeks from today. */
export function latestExamDate(today: string) {
  return addDays(today, MAX_WEEKS * 7)
}

/** What is wrong with each field, by the field's name; empty when the form can be sent. */
export function validateStudyPlanForm(form: StudyPlanForm, today: string) {
  const errors: Record<string, string> = {}
  if (!isCourseId(form.courseId)) errors.courseId = 'Choose a course.'
  const hours = Number(form.hoursPerWeek)
  if (!form.hoursPerWeek.trim() || !Number.isInteger(hours) || hours < 1 || hours > MAX_HOURS_PER_WEEK) {
    errors.hoursPerWeek = `Enter whole hours from 1 to ${MAX_HOURS_PER_WEEK}.`
  }
  if (form.examDate && form.examDate <= today) errors.examDate = 'Choose an exam date after today.'
  else if (form.examDate > latestExamDate(today)) errors.examDate = `A plan covers at most ${MAX_WEEKS} weeks. Choose an exam date sooner.`
  return { ...errors, ...materialErrors(form) }
}

/** As for a personal plan, but only a buddy session asks for its course. */
export function validateSessionPlanForm(form: SessionPlanForm, needsCourse: boolean) {
  const errors: Record<string, string> = {}
  if (needsCourse && !isCourseId(form.courseId)) errors.courseId = 'Choose the course this session is for.'
  return { ...errors, ...materialErrors(form) }
}

function materialErrors(form: MaterialForm) {
  const errors: Record<string, string> = {}
  if (form.materialKind === 'files') {
    const problem = checkFiles(form.files)
    if (problem) errors.files = problem
  } else if (!form.topicsText.trim()) {
    errors.topicsText = 'Paste the topics to cover, such as the lecture titles.'
  } else if (form.topicsText.length > MAX_NOTES_CHARACTERS) {
    errors.topicsText = `Use up to ${MAX_NOTES_CHARACTERS.toLocaleString('en-SG')} characters, or upload the notes as a .txt file.`
  }
  return errors
}

function isCourseId(value: string) {
  return Number.isSafeInteger(Number(value)) && Number(value) >= 1
}

/** The checked personal plan form as the request sends it. */
export function planRequest(form: StudyPlanForm): { input: PersonalPlanInput; material: StudyMaterial } {
  return {
    input: { courseId: Number(form.courseId), hoursPerWeek: Number(form.hoursPerWeek), examDate: form.examDate || null, studyGoal: goalOf(form.studyGoal) },
    material: materialOf(form),
  }
}

/** The checked session plan form as the request sends it. */
export function sessionPlanRequest(form: SessionPlanForm, needsCourse: boolean): { input: SessionPlanInput; material: StudyMaterial } {
  return { input: { courseId: needsCourse ? Number(form.courseId) : null, studyGoal: goalOf(form.studyGoal) }, material: materialOf(form) }
}

function goalOf(value: string) {
  return STUDY_GOALS.includes(value as StudyGoal) ? value as StudyGoal : null
}

function materialOf(form: MaterialForm): StudyMaterial {
  return form.materialKind === 'files' ? { kind: 'files', files: form.files } : { kind: 'topics', text: form.topicsText.trim() }
}
