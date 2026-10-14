import { api } from '../../shared/api/client'
import type { Course, StudyGoal, StudyMode, WeeklySlot } from '../../shared/api/types'

export type GroupSizePreference = 'ONE_TO_ONE' | 'SMALL_GROUP' | 'EITHER'
export const GROUP_SIZE_PREFERENCES: GroupSizePreference[] = ['ONE_TO_ONE', 'SMALL_GROUP', 'EITHER']

export type MyProfile = {
  id: number
  email: string
  name: string
  school: string
  programme: string
  yearOfStudy: number
  contactNumber: string
  coursesTaken: Course[]
  targetCourse: Course | null
  preferredStudyMode: StudyMode | null
  groupSizePreference: GroupSizePreference
  groupSizeMax: number
  studyGoals: StudyGoal[]
  availability: WeeklySlot[]
}

export type UpdateProfileRequest = {
  name: string
  school: string
  programme: string
  yearOfStudy: number
  contactNumber: string
  courseIds: number[]
  targetCourseId: number | null
  preferredStudyMode: StudyMode
  groupSizePreference: GroupSizePreference
  studyGoals: StudyGoal[]
}

export async function getMyProfile(signal?: AbortSignal) {
  return (await api.get<MyProfile>('/profile/me', { signal })).data
}

export async function updateMyProfile(profile: UpdateProfileRequest) {
  return (await api.put<MyProfile>('/profile/me', profile)).data
}

export async function replaceMyAvailability(slots: WeeklySlot[]) {
  return (await api.put<WeeklySlot[]>('/profile/me/availability', { slots })).data
}
