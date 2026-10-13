import type { WeeklySlot } from '../api/types'

export type SlotEdge = 'startTime' | 'endTime'
/** Text in one time box before it becomes a valid HH:MM time. */
type Draft = { text: string; finished: boolean }
export type WeeklySlotDrafts = Record<number, Partial<Record<SlotEdge, Draft>>>
const LAST_MINUTE = 24 * 60 - 1

export function minutesOf(time: string) {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function timeOf(minutes: number) {
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
}

/** Reads 24-hour input as HH:MM: "18:00", "1800" and, once finished, "930" or "9". */
export function parseTime(text: string, finished: boolean) {
  const match = text.match(/^(\d{1,2}):(\d{2})$/) || text.match(/^(\d{2})(\d{2})$/)
    || (finished ? text.match(/^(\d)(\d{2})$/) || text.match(/^(\d{1,2})()$/) : null)
  if (!match) return undefined
  const hours = Number(match[1]), minutes = Number(match[2] || 0)
  return hours <= 23 && minutes <= 59 ? timeOf(hours * 60 + minutes) : undefined
}

/** Moving the start to or past the end pushes the end one hour later. */
export function changedTime(slot: WeeklySlot, edge: SlotEdge, time: string): WeeklySlot {
  if (edge === 'endTime') return { ...slot, endTime: time }
  const start = minutesOf(time)
  const endTime = minutesOf(slot.endTime) > start ? slot.endTime : timeOf(Math.min(start + 60, LAST_MINUTE))
  return { ...slot, startTime: time, endTime }
}

/** Submit also finishes shorthand input when Enter is pressed without leaving the time box. */
export function finishSlotDrafts(value: WeeklySlot[], drafts: WeeklySlotDrafts) {
  const remaining: WeeklySlotDrafts = {}
  const slots = value.map((slot, index) => {
    let next = slot
    for (const edge of ['startTime', 'endTime'] as SlotEdge[]) {
      const draft = drafts[index]?.[edge]
      if (!draft) continue
      const time = parseTime(draft.text, true)
      if (time) next = changedTime(next, edge, time)
      else remaining[index] = { ...remaining[index], [edge]: { ...draft, finished: true } }
    }
    return next
  })
  return { slots, drafts: remaining }
}
