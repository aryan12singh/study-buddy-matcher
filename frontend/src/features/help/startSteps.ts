import type { MyProfile } from '../profile/api'
import type { StudentSummary } from '../students/api'
import { profileProgress } from '../home/profileProgress'

export type StartStep = {
  key: string
  title: string
  detail: string
  action: string
  to: string
  /** Undefined while the student's progress is unknown (loading or failed). */
  done?: boolean
}

function stepDone(profile: MyProfile | undefined, key: string) {
  if (!profile) return undefined
  return profileProgress(profile).steps.find(step => step.key === key)?.done
}

/**
 * The first five things a new student does, in order. Completion is read from the
 * student's own profile and activity summary; a step with no reliable signal stays unknown.
 */
export function startSteps(studentId: number, profile?: MyProfile, summary?: StudentSummary): StartStep[] {
  const reachedOut = summary ? summary.pendingOutgoing + summary.activeConnections > 0 : undefined
  return [
    {
      key: 'preferences',
      title: 'Add your courses and study mode',
      detail: 'Matching compares the courses you take and how you like to study with other students.',
      action: 'Add courses and study mode',
      to: `/students/${studentId}/edit?tab=preferences`,
      done: stepDone(profile, 'preferences')
    },
    {
      key: 'availability',
      title: 'Set your weekly availability',
      detail: 'Mark the hours you can usually study. Shared hours count towards every match score.',
      action: 'Set weekly availability',
      to: `/students/${studentId}/edit?tab=availability`,
      done: stepDone(profile, 'availability')
    },
    {
      key: 'search',
      title: 'Find study buddies',
      detail: 'Search by course and compare scored matches, with a breakdown of every criterion.',
      action: 'Find buddies',
      to: '/find-buddies',
      done: reachedOut
    },
    {
      key: 'request',
      title: 'Send a match request',
      detail: 'Open a match and send a short request. Once accepted, you can see each other’s contact numbers.',
      action: 'View my requests',
      to: '/connections?view=outgoing',
      done: reachedOut
    },
    {
      key: 'group',
      title: 'Join or create a study group',
      detail: 'Apply to an open group for your course, or start your own and approve who joins.',
      action: 'Browse study groups',
      to: '/groups',
      done: summary ? summary.acceptedGroups > 0 : undefined
    }
  ]
}
