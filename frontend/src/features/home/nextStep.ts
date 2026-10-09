import type { MatchRequest } from '../connections/api'
import type { MyProfile } from '../profile/api'
import { profileProgress } from './profileProgress'

export type NextStep = {
  title: string
  detail: string
  action: string
  to: string
  progress?: { done: number; total: number }
}

const STEP_COPY: Record<string, { title: string; detail: string }> = {
  preferences: { title: 'Add your courses and study mode', detail: 'Matching compares these with other students. It takes about a minute.' },
  availability: { title: 'Add your weekly availability', detail: 'Matching looks for overlap with the times you can usually study.' }
}

/** "About you" is done at sign-up, so setup counts it plus the required profile steps. */
export function setupProgress(profile: MyProfile) {
  const required = profileProgress(profile).steps.filter(step => !step.optional)
  return { done: 1 + required.filter(step => step.done).length, total: 1 + required.length }
}

function names(requests: MatchRequest[]) {
  const shown = requests.slice(0, 2).map(request => request.senderName)
  const others = requests.length - shown.length
  if (others > 0) return `${shown.join(', ')} and ${others} ${others === 1 ? 'other' : 'others'}`
  return shown.join(' and ')
}

/** The single most useful thing to do now: finish the profile, then reply to requests, else nothing. */
export function nextStep(studentId: number, profile?: MyProfile, pending?: MatchRequest[]): NextStep | null {
  if (profile) {
    const progress = profileProgress(profile)
    if (!progress.complete && progress.nextStep) {
      return {
        ...STEP_COPY[progress.nextStep.key],
        action: 'Continue setup ›',
        to: `/students/${studentId}/edit?tab=${progress.nextStep.tab}`,
        progress: setupProgress(profile)
      }
    }
  }
  if (pending?.length) {
    const count = pending.length
    return {
      title: `Reply to ${count} study buddy ${count === 1 ? 'request' : 'requests'}`,
      detail: `${names(pending)} ${count === 1 ? 'is' : 'are'} waiting. Accepting shares your contact numbers with each other.`,
      action: 'Review requests ›',
      to: '/connections?view=incoming'
    }
  }
  return null
}
