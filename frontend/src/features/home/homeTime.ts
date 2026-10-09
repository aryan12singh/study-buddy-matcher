import { CAMPUS_TIMEZONE, campusDate } from '../../shared/api/types'

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

function format(value: Date, options: Intl.DateTimeFormatOptions) {
  return new Intl.DateTimeFormat('en-GB', { timeZone: CAMPUS_TIMEZONE, ...options }).format(value)
}

export function isToday(iso: string, now = new Date()) {
  return campusDate(iso) === campusDate(now.toISOString())
}

function isYesterday(iso: string, now: Date) {
  return campusDate(iso) === campusDate(new Date(now.getTime() - DAY).toISOString())
}

/** "Thursday, 9 October" in campus time. */
export function todayLabel(now = new Date()) {
  return `${format(now, { weekday: 'long' })}, ${format(now, { day: 'numeric', month: 'long' })}`
}

/** The campus weekday as stored on availability slots, e.g. "THURSDAY". */
export function campusWeekday(now = new Date()) {
  return format(now, { weekday: 'long' }).toUpperCase()
}

/** "Just now", "5 minutes ago", "2 hours ago", "Yesterday" or "6 Oct". */
export function timeAgo(iso: string, now = new Date()) {
  const elapsed = now.getTime() - new Date(iso).getTime()
  if (elapsed < MINUTE) return 'Just now'
  if (elapsed < HOUR) {
    const minutes = Math.floor(elapsed / MINUTE)
    return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'} ago`
  }
  if (isToday(iso, now)) {
    const hours = Math.floor(elapsed / HOUR)
    return `${hours} ${hours === 1 ? 'hour' : 'hours'} ago`
  }
  if (isYesterday(iso, now)) return 'Yesterday'
  return format(new Date(iso), { day: 'numeric', month: 'short' })
}

/** "14:20" today, "Yesterday, 18:42", otherwise "6 Oct, 21:10". */
export function feedTime(iso: string, now = new Date()) {
  const clock = format(new Date(iso), { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
  if (isToday(iso, now)) return clock
  if (isYesterday(iso, now)) return `Yesterday, ${clock}`
  return `${format(new Date(iso), { day: 'numeric', month: 'short' })}, ${clock}`
}
