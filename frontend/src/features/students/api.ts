import { api } from '../../shared/api/client'
import type { Course, StudyGoal, StudyMode, WeeklySlot } from '../../shared/api/types'
export type Relationship = { state: 'SELF' | 'STRANGER' | 'INCOMING_PENDING' | 'OUTGOING_PENDING' | 'CONNECTED'; requestId: number | null; connectionId: number | null }
export type PublicProfile = { id: number; name: string; school: string; programme: string; yearOfStudy: number; coursesTaken: Course[]; targetCourse: Course | null;
  preferredStudyMode: StudyMode | null; studyGoals: StudyGoal[]; preferredGroupSizeMin: number | null; preferredGroupSizeMax: number | null;
  availability: WeeklySlot[]; relationship: Relationship }
export type ConnectedProfile = PublicProfile & { contactNumber: string }
export type StudentProfile = PublicProfile | ConnectedProfile
export async function getStudentProfile(id: number, signal?: AbortSignal) { return (await api.get<StudentProfile>(`/students/${id}/profile`, { signal })).data }
export type StudentSummary = { activeConnections: number; pendingIncoming: number; pendingOutgoing: number; acceptedGroups: number }
export async function getStudentSummary(signal?: AbortSignal) { return (await api.get<StudentSummary>('/students/me/summary', { signal })).data }
