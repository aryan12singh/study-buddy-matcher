import type { StudySession, SessionOccurrence } from './api'
import { CAMPUS_TIMEZONE } from '../../shared/api/types'

export function occurrenceLabel(occurrence: SessionOccurrence) {
  const format = new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeStyle: 'short', timeZone: CAMPUS_TIMEZONE })
  return `${format.format(new Date(occurrence.startAt))} – ${format.format(new Date(occurrence.endAt))}`
}
/** The occurrence list remains authoritative when a cached nextOccurrence has ended. */
export function nextSessionOccurrence(session: StudySession, now: number) {
  if (session.status === 'CANCELLED') return null
  return session.occurrences.filter(occurrence => Date.parse(occurrence.endAt) > now)
    .sort((left, right) => Date.parse(left.startAt) - Date.parse(right.startAt))[0] ?? null
}
export function sessionStatusLabel(session: StudySession, now: number) {
  if (session.status === 'CANCELLED') return 'Cancelled'
  const occurrence = nextSessionOccurrence(session, now)
  return !occurrence ? 'Past session' : Date.parse(occurrence.startAt) <= now ? 'Ongoing' : 'Upcoming'
}
export function displayedOccurrence(session: StudySession, now: number) {
  return nextSessionOccurrence(session, now) ?? session.occurrences.at(-1) ?? { startAt: session.startAt, endAt: session.endAt }
}
export function sessionParent(session: StudySession) {
  return session.scope === 'GROUP' ? `/groups/${session.groupId}/sessions` : `/connections/${session.connectionId}/sessions`
}
export function safeRoomLink(session: StudySession) {
  if (session.scope === 'GROUP' && session.roomLink === `/groups/${session.groupId}/room`) return session.roomLink
  try {
    const url = new URL(session.roomLink ?? '')
    if (url.protocol === 'https:' && !url.username && !url.password && !url.search && !url.hash) return url.href
  } catch { /* Invalid legacy links are not made clickable. */ }
  return null
}
