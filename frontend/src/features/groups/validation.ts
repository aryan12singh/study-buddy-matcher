import type { StudyGoal, WeeklySlot } from '../../shared/api/types'
import { validateSlots } from '../../shared/api/schedules'
/** Matches the default app.study-group.max-capacity; the server checks it again. */
export const MAX_GROUP_SIZE = 10
export type GroupForm = { name: string; description: string; courseId: string; preferredStudyMode: string; maxGroupSize: string; studyGoals: StudyGoal[]; availability: WeeklySlot[] }
export function validateGroupForm(form: GroupForm, memberCount: number) {
  const errors: Record<string, string> = {}
  if (!form.name.trim() || form.name.trim().length > 255) errors.name = 'Enter a name, up to 255 characters.'
  if (form.description.length > 4000) errors.description = 'Use up to 4,000 characters.'
  if (!Number.isSafeInteger(Number(form.courseId)) || Number(form.courseId) < 1) errors.courseId = 'Choose a course.'
  const size = Number(form.maxGroupSize)
  const smallest = Math.max(2, memberCount)
  if (!Number.isInteger(size) || size < smallest || size > MAX_GROUP_SIZE) errors.maxGroupSize = `Capacity must be a whole number from ${smallest} to ${MAX_GROUP_SIZE}. The leader counts as a member.`
  const slotsError = validateSlots(form.availability)
  if (slotsError) errors.availability = slotsError
  return errors
}
