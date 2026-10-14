import type { StudySession, SessionOccurrence } from './api'

export function occurrenceLabel(occurrence: SessionOccurrence, timeZone: string) {
  const format = new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeStyle: 'short', timeZone })
  return `${format.format(new Date(occurrence.startAt))} – ${format.format(new Date(occurrence.endAt))}`
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
