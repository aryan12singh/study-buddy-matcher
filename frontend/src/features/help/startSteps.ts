import type { MyProfile } from '../profile/api'
import type { StudentSummary } from '../students/api'
import type { GroupSummary } from '../groups/api'
import type { StudySession } from '../sessions/api'
import { nextSessionOccurrence } from '../sessions/sessionPresentation'
import { profileProgress } from '../home/profileProgress'

export type ProgressSource<T> = { data?: T; loading: boolean; error?: string }
export type HelpProgress = {
  profile: ProgressSource<MyProfile>
  summary: ProgressSource<StudentSummary>
  groups: ProgressSource<GroupSummary[]>
  sessions: ProgressSource<StudySession[]>
}
export type StartStep = {
  key: string
  title: string
  detail: string
  action: string
  to: string
  state: 'confirmed' | 'missing' | 'loading' | 'unavailable' | 'unknown' | 'untracked'
  stateLabel: string
}

function checked<T>(source: ProgressSource<T>, complete: (data: T) => boolean, yes: string, no: string): Pick<StartStep, 'state' | 'stateLabel'> {
  if (source.error) return { state: 'unavailable', stateLabel: 'Could not check' }
  if (source.data !== undefined) return complete(source.data)
    ? { state: 'confirmed', stateLabel: yes } : { state: 'missing', stateLabel: no }
  return source.loading ? { state: 'loading', stateLabel: 'Checking current records' } : { state: 'unknown', stateLabel: 'Not checked' }
}

/** Only active, accepted memberships may supply a private room or group agenda link. */
export function openMemberships(groups?: GroupSummary[]) {
  return groups?.filter(group => group.active && group.viewer.member) ?? []
}

/** These are current records, not a lifetime checklist of searches, requests or attendance. */
export function startSteps(studentId: number, sources: HelpProgress, now: number): StartStep[] {
  const { profile, summary, groups, sessions } = sources
  const member = !groups.error ? openMemberships(groups.data)[0] : undefined
  return [
    {
      key: 'preferences', title: 'Add your courses and study mode',
      detail: 'Matching compares the courses you take and how you like to study. A target course and study goals can help you choose a search.',
      action: 'Edit courses and study mode', to: `/students/${studentId}/edit?tab=preferences`,
      ...checked(profile, data => profileProgress(data).steps.find(step => step.key === 'preferences')!.done, 'Saved in your profile', 'Still needed in your profile')
    },
    {
      key: 'availability', title: 'Set your weekly availability',
      detail: 'Mark the hours you can usually study, in Singapore time. Shared hours contribute to matching and session suggestions.',
      action: 'Edit weekly availability', to: `/students/${studentId}/edit?tab=availability`,
      ...checked(profile, data => profileProgress(data).steps.find(step => step.key === 'availability')!.done, 'Saved in your profile', 'Still needed in your profile')
    },
    {
      key: 'search', title: 'Compare study buddies',
      detail: 'Choose a course or study goal, compare the score breakdown and shared hours, then open a profile. This guide does not record searches.',
      action: 'Find study buddies', to: '/find-buddies', state: 'untracked', stateLabel: 'Search history is not tracked'
    },
    {
      key: 'request', title: 'Send or answer a buddy request',
      detail: 'Send a short request from a match or profile, or answer an incoming request. Acceptance shares contact numbers only between the two connected buddies.',
      action: 'View buddy requests', to: '/connections?view=incoming',
      ...checked(summary, data => data.pendingOutgoing + data.activeConnections > 0, 'A sent request or active connection exists', 'No pending sent request or active connection')
    },
    {
      key: 'group', title: 'Join or create a study group',
      detail: 'Apply to an open course group and wait for its leader to accept you, or create your own. A pending application does not grant room access.',
      action: 'Browse study groups', to: '/groups',
      ...checked(groups, data => openMemberships(data).length > 0, 'You belong to an open group', 'No current open-group membership')
    },
    {
      key: 'room', title: 'Open your group’s study room',
      detail: 'From Your groups, open an accepted group’s room preview, then choose Join study room. Share its focus timer and opt into music or ambience when ready. Visits are not tracked here.',
      action: member ? 'Preview group study room' : 'View your groups', to: member ? `/groups/${member.id}/room` : '/groups?view=mine',
      state: 'untracked', stateLabel: 'Room visits are not tracked'
    },
    {
      key: 'session', title: 'Make a dated study plan',
      detail: 'A group leader plans from the group’s session agenda; connected buddies plan from Connected buddies. Pick a suggested or manual time, then export the saved session to your calendar. An export is a snapshot, not a live sync.',
      action: 'View your study sessions', to: '/sessions',
      ...checked(sessions, data => data.some(session => nextSessionOccurrence(session, now)), 'An ongoing or upcoming session exists', 'No ongoing or upcoming session')
    }
  ]
}
