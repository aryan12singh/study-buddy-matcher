/**
 * A plan's dates are calendar days (yyyy-MM-dd) with no time or zone, so they are read and shown
 * as UTC days: reading them in the browser's zone could shift them by one.
 */
export function formatPlanDate(day: string) {
  return new Intl.DateTimeFormat('en-SG', { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(`${day}T00:00:00Z`))
}

/** "1 week", or "4 weeks" for a longer plan. */
export function weekCountLabel(blockCount: number) {
  return blockCount === 1 ? '1 week' : `${blockCount} weeks`
}
