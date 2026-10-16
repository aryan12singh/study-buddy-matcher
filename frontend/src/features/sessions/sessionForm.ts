import type { StudySession, SessionInput } from './api'
import type { WeeklySlot } from '../../shared/api/types'
import { CAMPUS_TIMEZONE, WEEK_DAYS } from '../../shared/api/types'

export type SessionForm = Omit<SessionInput, 'occurrenceCount' | 'agenda' | 'location' | 'roomLink'> & {
  occurrenceCount: string; agenda: string; location: string; roomLink: string
}
export function localDateTime(value: Date, timeZone = CAMPUS_TIMEZONE) {
  const values = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(value).map(part => [part.type, part.value]))
  return `${values.year}-${values.month}-${values.day}T${values.hour}:${values.minute}`
}
export function initialSessionForm(session?: StudySession, groupId?: number): SessionForm {
  const start = new Date(Date.now() + 60 * 60 * 1000), end = new Date(start.getTime() + 60 * 60 * 1000)
  return session ? { title: session.title, startLocal: session.startLocal.slice(0, 16), endLocal: session.endLocal.slice(0, 16),
    timeZone: session.timeZone, occurrenceCount: String(session.occurrenceCount), agenda: session.agenda ?? '', location: session.location ?? '', roomLink: session.roomLink ?? '' }
    : { title: '', startLocal: localDateTime(start), endLocal: localDateTime(end), timeZone: CAMPUS_TIMEZONE, occurrenceCount: '1', agenda: '', location: '', roomLink: groupId ? `/groups/${groupId}/room` : '' }
}
function validLocal(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) return false
  const parsed = new Date(`${value}:00Z`)
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 16) === value
}
export function validateSessionForm(form: SessionForm, groupId?: number) {
  const errors: Record<string, string> = {}
  if (!form.title.trim() || form.title.trim().length > 255) errors.title = 'Enter a title of 1–255 characters.'
  if (!validLocal(form.startLocal)) errors.startLocal = 'Enter a complete date and time to the minute.'
  if (!validLocal(form.endLocal)) errors.endLocal = 'Enter a complete date and time to the minute.'
  if (!errors.startLocal && !errors.endLocal && form.endLocal <= form.startLocal) errors.endLocal = 'End time must be after the start time.'
  try { new Intl.DateTimeFormat('en', { timeZone: form.timeZone.trim() }).format() }
  catch { errors.timeZone = 'Enter a valid IANA timezone, such as Asia/Singapore.' }
  if (!form.timeZone.trim() || form.timeZone.length > 100) errors.timeZone = 'Enter a valid IANA timezone of at most 100 characters.'
  const count = Number(form.occurrenceCount)
  if (!Number.isInteger(count) || count < 1 || count > 52) errors.occurrenceCount = 'Choose 1–52 weekly dates; 1 means a one-off session.'
  if (form.agenda.length > 4000) errors.agenda = 'Keep the agenda within 4000 characters.'
  if (form.location.length > 255) errors.location = 'Keep the location within 255 characters.'
  const link = form.roomLink.trim()
  if (link && link !== (groupId ? `/groups/${groupId}/room` : undefined)) {
    try {
      const url = new URL(link)
      if (url.protocol !== 'https:' || url.username || url.password || url.search || url.hash) throw new Error('invalid')
    } catch { errors.roomLink = 'Use a public HTTPS link without credentials, query parameters or fragments, or this group’s room path.' }
  }
  if (link.length > 2048) errors.roomLink = 'Keep the link within 2048 characters.'
  return errors
}
export function sessionInput(form: SessionForm): SessionInput {
  return { ...form, title: form.title.trim(), timeZone: form.timeZone.trim(), occurrenceCount: Number(form.occurrenceCount),
    agenda: form.agenda.trim() || null, location: form.location.trim() || null, roomLink: form.roomLink.trim() || null }
}
export function suggestionDurationMinutes(value: string) {
  const duration = Number(value)
  return Number.isSafeInteger(duration) && duration > 0 ? duration : null
}
/** Weekly availability is campus-local, independently of the session's selected timezone. */
export function nextSuggestedTimes(slot: WeeklySlot, durationMinutes = 60, now = new Date()) {
  const startMinutes = Number(slot.startTime.slice(0, 2)) * 60 + Number(slot.startTime.slice(3, 5))
  const endMinutes = Number(slot.endTime.slice(0, 2)) * 60 + Number(slot.endTime.slice(3, 5))
  if (!Number.isSafeInteger(durationMinutes) || durationMinutes <= 0 || endMinutes - startMinutes < durationMinutes) return null
  const localNow = localDateTime(now)
  const date = localNow.slice(0, 10)
  const day = new Date(`${date}T00:00:00Z`)
  const target = (WEEK_DAYS.indexOf(slot.dayOfWeek) + 1) % 7
  const offset = (target - day.getUTCDay() + 7) % 7
  day.setUTCDate(day.getUTCDate() + offset)
  if (`${day.toISOString().slice(0, 10)}T${slot.startTime.slice(0, 5)}` <= localNow) day.setUTCDate(day.getUTCDate() + 7)
  const nextDate = day.toISOString().slice(0, 10)
  const startLocal = `${nextDate}T${slot.startTime.slice(0, 5)}`
  // This date is a civil-time calculation; campus time has no daylight-saving changes.
  const end = new Date(`${startLocal}:00Z`)
  end.setUTCMinutes(end.getUTCMinutes() + durationMinutes)
  return { startLocal, endLocal: end.toISOString().slice(0, 16), timeZone: CAMPUS_TIMEZONE }
}
