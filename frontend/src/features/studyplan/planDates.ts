/**
 * A plan's dates are calendar days (yyyy-MM-dd) with no time or zone, so they are read and shown
 * as UTC days: reading them in the browser's zone could shift them by one.
 */
export function formatPlanDate(day: string) {
  return new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(`${day}T00:00:00Z`))
}

/** The calendar day `days` after `day`, both yyyy-MM-dd. */
export function addDays(day: string, days: number) {
  const date = new Date(`${day}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

/** 45 reads as "45 min", 90 as "1 h 30 min" and 360 as "6 h". */
export function formatMinutes(minutes: number) {
  const hours = Math.floor(minutes / 60), rest = minutes % 60
  if (!hours) return `${rest} min`
  return rest ? `${hours} h ${rest} min` : `${hours} h`
}

/** "1 week", or "4 weeks" for a longer plan. */
export function weekCountLabel(blockCount: number) {
  return blockCount === 1 ? '1 week' : `${blockCount} weeks`
}
