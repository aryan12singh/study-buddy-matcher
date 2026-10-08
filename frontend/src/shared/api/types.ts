export type Role = 'STUDENT' | 'ADMIN'
export type StudyMode = 'IN_PERSON' | 'ONLINE' | 'EITHER'
export type StudyGoal = 'CONCEPT_REVIEW' | 'PROBLEM_SOLVING' | 'EXAM_PREPARATION' | 'PROJECT_DISCUSSION'
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
export type WeeklySlot = { dayOfWeek: DayOfWeek; startTime: string; endTime: string }
export type Course = { id: number; code: string; name: string }

export const STUDY_GOALS: StudyGoal[] = ['CONCEPT_REVIEW', 'PROBLEM_SOLVING', 'EXAM_PREPARATION', 'PROJECT_DISCUSSION']
export const STUDY_MODES: StudyMode[] = ['IN_PERSON', 'ONLINE', 'EITHER']
export const WEEK_DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
export const CAMPUS_TIMEZONE = 'Asia/Singapore'
const labels: Record<string, string> = { IN_PERSON: 'In person', ONLINE: 'Online', EITHER: 'In person or online', CONCEPT_REVIEW: 'Concept review',
  PROBLEM_SOLVING: 'Problem solving', EXAM_PREPARATION: 'Exam preparation', PROJECT_DISCUSSION: 'Project discussion',
  PENDING: 'Pending', ACCEPTED: 'Accepted', DECLINED: 'Declined', CANCELLED: 'Cancelled', REJECTED: 'Rejected', STUDENT: 'Student', ADMIN: 'Administrator' }
export function label(value: string | null | undefined) {
  if (!value) return 'Not specified'
  return labels[value] || value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}
export function formatTimestamp(value: string | null | undefined) {
  if (!value) return 'Never logged in'
  return new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeStyle: 'short', timeZone: CAMPUS_TIMEZONE }).format(new Date(value))
}
export function campusDate(value: string) {
  return new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: CAMPUS_TIMEZONE }).format(new Date(value))
}
