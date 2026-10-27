import type { NextStep } from '../home/nextStep'
import { nextStep } from '../home/nextStep'
import { nextSessionOccurrence, occurrenceLabel } from '../sessions/sessionPresentation'
import { openMemberships } from './startSteps'
import type { HelpProgress, ProgressSource } from './startSteps'

type Recommendation = { step: NextStep; message?: never } | { step?: never; message: string }

function unchecked<T>(source: ProgressSource<T>, name: string): Recommendation | undefined {
  if (source.error) return { message: `${name} could not be checked, so we cannot choose your next action yet. Retry below, or use any of the guide links.` }
  if (source.data === undefined) return { message: source.loading
    ? `Checking ${name.toLowerCase()} before choosing your next action…`
    : `${name} has not been checked. You can still use the guide links.` }
}

/** Higher-priority confirmed actions do not wait for unrelated sources to load. */
export function helpNextStep(studentId: number, sources: HelpProgress, now: number): Recommendation {
  const { profile, summary, groups, sessions } = sources
  const profileUnknown = unchecked(profile, 'Your profile')
  if (profileUnknown) return profileUnknown
  const setup = nextStep(studentId, profile.data)
  if (setup) return { step: { ...setup, action: 'Continue profile setup' } }

  const activityUnknown = unchecked(summary, 'Requests and connections')
  if (activityUnknown) return activityUnknown
  if (summary.data!.pendingIncoming > 0) {
    const count = summary.data!.pendingIncoming
    return { step: {
      title: `Reply to ${count} study buddy ${count === 1 ? 'request' : 'requests'}`,
      detail: 'Review who is asking to connect. Accepting shares your contact numbers with each other.',
      action: 'Review incoming requests', to: '/connections?view=incoming'
    } }
  }

  const sessionsUnknown = unchecked(sessions, 'Your study sessions')
  if (sessionsUnknown) return sessionsUnknown
  const nextSession = sessions.data!.filter(session => nextSessionOccurrence(session, now))
    .sort((left, right) => Date.parse(nextSessionOccurrence(left, now)!.startAt) - Date.parse(nextSessionOccurrence(right, now)!.startAt))[0]
  if (nextSession) return { step: {
    title: 'Review your next study session',
    detail: `${nextSession.title}: ${occurrenceLabel(nextSessionOccurrence(nextSession, now)!)}. Check the plan and export it if needed.`,
    action: 'View study session', to: `/sessions/${nextSession.id}`
  } }

  const groupsUnknown = unchecked(groups, 'Your study groups')
  if (groupsUnknown) return groupsUnknown
  const memberships = openMemberships(groups.data)
  const ledGroup = memberships.find(group => group.viewer.leader)
  if (ledGroup) return { step: {
    title: 'Plan a session for your group', detail: `${ledGroup.name} has a private room ready. Add a dated plan from its agenda so members know when to meet.`,
    action: 'Open group session agenda', to: `/groups/${ledGroup.id}/sessions`
  } }
  if (memberships.length) return { step: {
    title: 'Preview your group’s study room', detail: `Open ${memberships[0].name}’s room, then choose Join study room when ready. Ask the group leader to add a dated session from the agenda.`,
    action: 'Preview group study room', to: `/groups/${memberships[0].id}/room`
  } }
  if (summary.data!.activeConnections > 0) return { step: {
    title: 'Plan a session with a connected buddy', detail: 'Choose a connected buddy and use Plan study sessions to agree a date and time.',
    action: 'View connected buddies', to: '/connections?view=connected'
  } }
  if (summary.data!.pendingOutgoing > 0) return { step: {
    title: 'Explore study groups while you wait', detail: 'Your sent buddy request is awaiting a reply. You can also apply to a course group or create one.',
    action: 'Browse study groups', to: '/groups'
  } }
  return { step: {
    title: 'Find a study buddy', detail: 'Your courses, study mode and weekly availability are saved. Choose a course or study goal to compare matches.',
    action: 'Find study buddies', to: '/find-buddies'
  } }
}
