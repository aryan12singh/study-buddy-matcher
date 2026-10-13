import type { MyProfile } from '../profile/api'
import type { ProfileTab } from '../profile/profileForm'

export type ProfileStep = { key: string; title: string; detail: string; done: boolean; optional: boolean; tab: ProfileTab }

/** What a student still needs before matching has enough to work with. */
export function profileProgress(profile: MyProfile) {
  const steps: ProfileStep[] = [
    {
      key: 'preferences', title: 'Courses and study mode', detail: 'What you are taking and how you like to study',
      done: profile.coursesTaken.length > 0 && Boolean(profile.preferredStudyMode), optional: false, tab: 'preferences'
    },
    {
      key: 'availability', title: 'Weekly availability', detail: 'When you can usually study',
      done: profile.availability.length > 0, optional: false, tab: 'availability'
    },
    {
      key: 'target', title: 'Course you need a buddy for', detail: 'Optional',
      done: Boolean(profile.targetCourse), optional: true, tab: 'preferences'
    }
  ]
  const nextStep = steps.find(step => !step.optional && !step.done)
  return { steps, complete: !nextStep, nextStep }
}
