import { api } from '../../shared/api/client'
import type { StudyGoal } from '../../shared/api/types'

export type RequestContext = { origin: 'PROFILE' | 'MATCHING'; courseId?: number | null; courseCode?: string | null; courseName?: string | null; studyGoal?: StudyGoal | null }
export type MatchRequest = { id: number; senderId: number; senderName: string; receiverId: number; receiverName: string; message: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED'; createdAt: string; respondedAt: string | null; context: RequestContext }
export type Connection = { id: number; otherStudentId: number; otherStudentName: string; createdAt: string }

export async function getMatchRequests(direction: 'incoming' | 'outgoing', signal?: AbortSignal) { return (await api.get<MatchRequest[]>(`/match-requests/${direction}`, { signal })).data }
export async function getConnections(signal?: AbortSignal) { return (await api.get<Connection[]>('/connections', { signal })).data }
export async function sendMatchRequest(receiverId: number, message: string, context: RequestContext) {
  return (await api.post<MatchRequest>('/match-requests', { receiverId, message: message.trim() || null, context: { origin: context.origin, courseId: context.courseId || null, studyGoal: context.studyGoal || null } })).data
}
export async function decideMatchRequest(id: number, decision: 'accept' | 'decline') { return (await api.post<MatchRequest>(`/match-requests/${id}/${decision}`)).data }
export async function endConnection(id: number) { await api.delete(`/connections/${id}`) }
