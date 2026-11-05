import { api } from '../../shared/api/client'
import { CAMPUS_TIMEZONE, type Course } from '../../shared/api/types'
import type { StudySession } from '../sessions/api'

export type QuizScope = 'GROUP' | 'PERSONAL'
export type QuizStatus = 'OPEN' | 'CLOSED'
export type QuizAttemptState = 'NOT_STARTED' | 'STARTED' | 'SUBMITTED'

export type Quiz = {
  id: number; scope: QuizScope; course: Course; groupId: number | null; sessionId: number | null
  title: string; status: QuizStatus; createdBy: number | null; createdAt: string; closesAt: string | null
  closedAt: string | null; questionCount: number; submittedCount: number; myAttempt: QuizAttemptState; canHost: boolean
}
/** The questions as sent while the quiz is open: never the correct options. */
export type QuizQuestions = { quizId: number; startedAt: string; closesAt: string | null; questions: { id: number; prompt: string; options: string[] }[] }
export type QuizAnswer = { questionId: number; optionIndex: number }
export type AttemptResult = { score: number; total: number; timeTakenSeconds: number; rank: number }
export type LeaderboardEntry = { rank: number; studentId: number; displayName: string; score: number; total: number; timeTakenSeconds: number; me: boolean }
export type Leaderboard = { quizId: number; status: QuizStatus; submittedCount: number; notSubmittedCount: number; entries: LeaderboardEntry[] }
export type QuizReview = {
  quizId: number; score: number | null; total: number | null
  questions: { id: number; prompt: string; options: string[]; correctOption: number; chosenOption: number | null; explanation: string }[]
}

/** Where a quiz panel lives: a group's study room, or the student's own study corner. */
export type QuizHome = { scope: 'GROUP'; groupId: number } | { scope: 'PERSONAL' }

/** Shown wherever Gemini's questions or explanations appear. */
export const AI_QUIZ_LABEL = 'Questions written by Gemini; they may be inaccurate.'

/** Matches app.ai.assistant.quiz.max-focus-characters. */
export const MAX_FOCUS_CHARACTERS = 200
/**
 * Reading a PDF and writing ten questions can take Gemini far longer than the client's usual
 * timeout; this waits a little longer than the server's own 90-second limit on Gemini.
 */
const WRITING_TIMEOUT_MS = 120000

/** What the quiz is written from: uploaded files, or notes pasted into the chat. */
export type QuizMaterial = { kind: 'files'; files: File[] } | { kind: 'notes'; text: string }

function materialForm(material: QuizMaterial, focus: string) {
  const form = new FormData()
  if (material.kind === 'files') material.files.forEach(file => form.append('files', file))
  else form.append('notesText', material.text)
  if (focus.trim()) form.append('focus', focus.trim())
  return form
}

export async function listQuizzes(home: QuizHome, signal?: AbortSignal) {
  const path = home.scope === 'GROUP' ? `/groups/${home.groupId}/quizzes` : '/quizzes'
  return (await api.get<Quiz[]>(path, { signal })).data
}

/** @param sessionId one of the group's scheduled sessions the quiz is for, or null for none */
export async function startGroupQuiz(groupId: number, material: QuizMaterial, focus: string, sessionId: number | null = null) {
  const form = materialForm(material, focus)
  if (sessionId !== null) form.append('sessionId', String(sessionId))
  return (await api.post<Quiz>(`/groups/${groupId}/quizzes`, form, { timeout: WRITING_TIMEOUT_MS })).data
}

/** The group's sessions a quiz can be for: scheduled, with a meeting still to come. */
export function upcomingSessions(sessions: StudySession[]) {
  return sessions.filter(session => session.status === 'SCHEDULED' && session.nextOccurrence !== null)
}

/** "Week 4 revision · Thu 30 Oct", so two sessions with the same title can be told apart. */
export function sessionLabel(session: StudySession) {
  const next = session.nextOccurrence
  return next ? `${session.title} · ${new Date(next.startAt).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short', timeZone: CAMPUS_TIMEZONE })}` : session.title
}

export async function startPersonalQuiz(courseId: number, material: QuizMaterial, focus: string) {
  const form = materialForm(material, focus)
  form.append('courseId', String(courseId))
  return (await api.post<Quiz>('/quizzes', form, { timeout: WRITING_TIMEOUT_MS })).data
}

export async function beginAttempt(quizId: number) {
  return (await api.post<QuizQuestions>(`/quizzes/${quizId}/start`)).data
}

export async function submitAnswers(quizId: number, answers: QuizAnswer[]) {
  return (await api.post<AttemptResult>(`/quizzes/${quizId}/attempts`, { answers })).data
}

export async function getLeaderboard(quizId: number, signal?: AbortSignal) {
  return (await api.get<Leaderboard>(`/quizzes/${quizId}/leaderboard`, { signal })).data
}

export async function closeQuiz(quizId: number) {
  return (await api.post<Quiz>(`/quizzes/${quizId}/close`)).data
}

export async function getReview(quizId: number, signal?: AbortSignal) {
  return (await api.get<QuizReview>(`/quizzes/${quizId}/review`, { signal })).data
}

/** 95 seconds reads as "1:35". */
export function formatDuration(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}
