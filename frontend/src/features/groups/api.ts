import { api } from '../../shared/api/client'
import type { StudyGoal, StudyMode, WeeklySlot } from '../../shared/api/types'

export type GroupViewer = { leader: boolean; member: boolean; requestId: number | null; requestStatus: 'PENDING' | 'ACCEPTED' | 'REJECTED' | null }
export type GroupSummary = { id: number; name: string; courseId: number; courseCode: string; courseName: string; leaderId: number; leaderName: string;
  preferredStudyMode: StudyMode | null; studyGoals: StudyGoal[]; maxGroupSize: number; memberCount: number; active: boolean; viewer: GroupViewer }
export type GroupMember = { studentId: number; name: string; leader: boolean; joinedAt: string }
export type GroupDetail = GroupSummary & { description: string | null; createdAt: string; availability: WeeklySlot[]; members: GroupMember[] }
export type GroupApplication = { id: number; groupId: number; groupName: string; studentId: number; studentName: string; message: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'; createdAt: string; respondedAt: string | null; groupActive: boolean }
export type GroupInput = { name: string; description: string | null; courseId: number; studyGoals: StudyGoal[]; preferredStudyMode: StudyMode | null; maxGroupSize: number; availability: WeeklySlot[] }
export type GroupFilters = { courseId?: string; studyGoal?: string; studyMode?: string }

export async function getGroups(filters: GroupFilters, signal?: AbortSignal) { return (await api.get<GroupSummary[]>('/groups', { params: Object.fromEntries(Object.entries(filters).filter(([, value]) => value)), signal })).data }
export async function getMyGroups(signal?: AbortSignal) { return (await api.get<GroupSummary[]>('/groups/mine', { signal })).data }
export async function getMyApplications(signal?: AbortSignal) { return (await api.get<GroupApplication[]>('/group-join-requests/mine', { signal })).data }
export async function getGroup(id: number, signal?: AbortSignal) { return (await api.get<GroupDetail>(`/groups/${id}`, { signal })).data }
export async function saveGroup(input: GroupInput, id?: number) { return id ? (await api.put<GroupDetail>(`/groups/${id}`, input)).data : (await api.post<GroupDetail>('/groups', input)).data }
export async function applyToGroup(id: number, message: string) { return (await api.post<GroupApplication>(`/groups/${id}/join-requests`, { message: message.trim() || null })).data }
export async function getApplicants(id: number, signal?: AbortSignal) { return (await api.get<GroupApplication[]>(`/groups/${id}/join-requests`, { signal })).data }
export async function decideApplication(groupId: number, requestId: number, decision: 'accept' | 'reject') { return (await api.post<GroupApplication>(`/groups/${groupId}/join-requests/${requestId}/${decision}`)).data }
export async function removeMember(groupId: number, studentId: number) { await api.delete(`/groups/${groupId}/members/${studentId}`) }
export async function closeGroup(id: number) { return (await api.post<GroupDetail>(`/groups/${id}/close`)).data }
