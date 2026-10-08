import type { WeeklySlot } from './types'
export function validateSlots(slots: WeeklySlot[]) {
  for (let index = 0; index < slots.length; index++) {
    const slot = slots[index]
    const seconds = (time: string) => time.split(':').reduce((total, part, partIndex) => total + Number(part) * [3600, 60, 1][partIndex], 0)
    if (!slot.startTime || !slot.endTime || seconds(slot.startTime) >= seconds(slot.endTime)) return `Time block ${index + 1} must end after it starts.`
  }
  return undefined
}
