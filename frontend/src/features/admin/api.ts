import { api } from '../../shared/api/client'
import type { Role } from '../../shared/api/types'
export type AdminUser = { id: number; email: string; role: Role; name: string | null; active: boolean; createdAt: string; lastLoginAt: string | null }
export type UserUsage = { activeConnections: number; acceptedGroups: number; matchRequestsSent: number; groupsLed: number; groupsJoined: number }
export type AdminDetail = { account: AdminUser; usage: UserUsage | null; profile: { name: string; school: string; programme: string; yearOfStudy: number } | null }
export type AccountSummary = { total: number; active: number; inactive: number; students: number; admins: number }
export type AdminFilters = { role?: string; active?: string; search?: string }
export type UserInput = { email: string; name?: string; school?: string; programme?: string; yearOfStudy?: number; contactNumber?: string }
export type CreateUserInput = UserInput & { role: Role; password: string }
export async function getUsers(filters: AdminFilters, signal?: AbortSignal) { return (await api.get<AdminUser[]>('/admin/users', { params: Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '')), signal })).data }
export async function getAccountSummary(signal?: AbortSignal) { return (await api.get<AccountSummary>('/admin/users/summary', { signal })).data }
export async function getUser(id: number, signal?: AbortSignal) { return (await api.get<AdminDetail>(`/admin/users/${id}`, { signal })).data }
export async function createUser(input: CreateUserInput) { return (await api.post<AdminDetail>('/admin/users', input)).data }
export async function updateUser(id: number, input: UserInput) { return (await api.put<AdminDetail>(`/admin/users/${id}`, input)).data }
export async function changeUserStatus(id: number, action: 'deactivate' | 'reactivate') { return (await api.post<AdminDetail>(`/admin/users/${id}/${action}`)).data }
export async function deleteUser(id: number) { await api.delete(`/admin/users/${id}`) }
