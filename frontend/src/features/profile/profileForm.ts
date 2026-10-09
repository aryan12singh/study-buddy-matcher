import type { StudyGoal, StudyMode, WeeklySlot } from '../../shared/api/types'
import { validateSlots } from '../../shared/api/schedules'
import type { GroupSizePreference, MyProfile, UpdateProfileRequest } from './api'

/** Form state keeps raw input (strings) until it is saved. */
export type ProfileForm = {
  name: string
  school: string
  programme: string
  yearOfStudy: string
  contactNumber: string
  courseIds: number[]
  targetCourseId: string
  preferredStudyMode: StudyMode | ''
  groupSizePreference: GroupSizePreference | ''
  studyGoals: StudyGoal[]
}

export const ABOUT_FIELDS = ['name', 'school', 'programme', 'yearOfStudy', 'contactNumber']

export type ProfileTab = 'about' | 'preferences' | 'availability'
export const PROFILE_TABS: { value: ProfileTab; label: string }[] = [
  { value: 'about', label: 'About you' },
  { value: 'preferences', label: 'Study preferences' },
  { value: 'availability', label: 'Weekly availability' }
]

export function isProfileTab(value: string | null): value is ProfileTab {
  return PROFILE_TABS.some(tab => tab.value === value)
}

export function toForm(profile: MyProfile): ProfileForm {
  return {
    name: profile.name,
    school: profile.school,
    programme: profile.programme,
    yearOfStudy: String(profile.yearOfStudy),
    contactNumber: profile.contactNumber,
    courseIds: profile.coursesTaken.map(course => course.id).sort((left, right) => left - right),
    targetCourseId: profile.targetCourse ? String(profile.targetCourse.id) : '',
    preferredStudyMode: profile.preferredStudyMode ?? '',
    groupSizePreference: profile.groupSizePreference,
    studyGoals: [...profile.studyGoals].sort()
  }
}

export function toRequest(form: ProfileForm): UpdateProfileRequest {
  return {
    name: form.name.trim(),
    school: form.school.trim(),
    programme: form.programme.trim(),
    yearOfStudy: Number(form.yearOfStudy),
    contactNumber: form.contactNumber.trim(),
    courseIds: form.courseIds,
    targetCourseId: form.targetCourseId ? Number(form.targetCourseId) : null,
    preferredStudyMode: form.preferredStudyMode as StudyMode,
    groupSizePreference: form.groupSizePreference as GroupSizePreference,
    studyGoals: form.studyGoals
  }
}

/** Mirrors the server rules so most mistakes are caught before saving; the server still has the final say (e.g. the course limit). */
export function validateProfile(form: ProfileForm) {
  const errors: Record<string, string> = {}
  for (const field of ['name', 'school', 'programme', 'contactNumber'] as const) {
    if (!form[field].trim() || form[field].trim().length > 255) errors[field] = 'Enter a value, up to 255 characters.'
  }
  if (!Number.isInteger(Number(form.yearOfStudy)) || Number(form.yearOfStudy) < 1) errors.yearOfStudy = 'Enter a whole year of study of at least 1.'
  if (!form.courseIds.length) errors.courseIds = 'Choose at least one course you are currently taking.'
  if (!form.preferredStudyMode) errors.preferredStudyMode = 'Choose how you prefer to study.'
  if (!form.groupSizePreference) errors.groupSizePreference = 'Choose a preferred group size.'
  return errors
}

export function validateAvailability(slots: WeeklySlot[]) {
  const invalid = validateSlots(slots)
  if (invalid) return invalid
  const ordered = [...slots].sort((left, right) => left.dayOfWeek.localeCompare(right.dayOfWeek) || left.startTime.localeCompare(right.startTime))
  for (let index = 1; index < ordered.length; index++) {
    const previous = ordered[index - 1], current = ordered[index]
    if (previous.dayOfWeek === current.dayOfWeek && current.startTime.slice(0, 5) < previous.endTime.slice(0, 5)) {
      return 'Time blocks on the same day cannot overlap.'
    }
  }
  return undefined
}

export function sameSlots(left: WeeklySlot[], right: WeeklySlot[]) {
  const key = (slots: WeeklySlot[]) => slots.map(slot => `${slot.dayOfWeek} ${slot.startTime.slice(0, 5)} ${slot.endTime.slice(0, 5)}`).sort().join('|')
  return key(left) === key(right)
}
