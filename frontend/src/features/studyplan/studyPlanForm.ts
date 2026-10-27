import type { Course, StudyGoal } from '../../shared/api/types'
import { STUDY_GOALS } from '../../shared/api/types'
import { checkFiles, MAX_NOTES_CHARACTERS } from '../quiz/api'
import { MAX_HOURS_PER_WEEK, MAX_WEEKS } from './api'
import type { PersonalPlanInput, StudyMaterial } from './api'
import { addDays } from './planDates'

/** The form as typed: every field a string until it is checked. */
export type StudyPlanForm = {
  courseId: string; hoursPerWeek: string; examDate: string; studyGoal: string
  materialKind: StudyMaterial['kind']; files: File[]; topicsText: string
}

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
    studyGoal: '', materialKind: 'files', files: [], topicsText: '',
  }
}

/** The last exam date a plan can reach: a plan covers at most MAX_WEEKS weeks from today. */
export function latestExamDate(today: string) {
  return addDays(today, MAX_WEEKS * 7)
}

/** What is wrong with each field, by the field's name; empty when the form can be sent. */
export function validateStudyPlanForm(form: StudyPlanForm, today: string) {
  const errors: Record<string, string> = {}
  if (!Number.isSafeInteger(Number(form.courseId)) || Number(form.courseId) < 1) errors.courseId = 'Choose a course.'
  const hours = Number(form.hoursPerWeek)
  if (!form.hoursPerWeek.trim() || !Number.isInteger(hours) || hours < 1 || hours > MAX_HOURS_PER_WEEK) {
    errors.hoursPerWeek = `Enter whole hours from 1 to ${MAX_HOURS_PER_WEEK}.`
  }
  if (form.examDate && form.examDate <= today) errors.examDate = 'Choose an exam date after today.'
  else if (form.examDate > latestExamDate(today)) errors.examDate = `A plan covers at most ${MAX_WEEKS} weeks. Choose an exam date sooner.`
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

/** The checked form as the request sends it. */
export function planRequest(form: StudyPlanForm): { input: PersonalPlanInput; material: StudyMaterial } {
  return {
    input: {
      courseId: Number(form.courseId), hoursPerWeek: Number(form.hoursPerWeek), examDate: form.examDate || null,
      studyGoal: STUDY_GOALS.includes(form.studyGoal as StudyGoal) ? form.studyGoal as StudyGoal : null,
    },
    material: form.materialKind === 'files' ? { kind: 'files', files: form.files } : { kind: 'topics', text: form.topicsText.trim() },
  }
}
