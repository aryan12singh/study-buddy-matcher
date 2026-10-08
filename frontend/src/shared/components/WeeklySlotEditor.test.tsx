import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import type { WeeklySlot } from '../api/types'
import { validateSlots } from '../api/schedules'
import WeeklySlotEditor from './WeeklySlotEditor'

function Editor({ initial }: { initial: WeeklySlot[] }) {
  const [slots, setSlots] = useState(initial)
  return <><WeeklySlotEditor value={slots} onChange={setSlots} error={validateSlots(slots)} /><output aria-label="Saved input">{JSON.stringify(slots)}</output></>
}
const monday: WeeklySlot = { dayOfWeek: 'MONDAY', startTime: '18:00:00', endTime: '20:00:00' }

describe('weekly editor conveniences', () => {
  it('duplicates independently and previews edits without changing the original block', () => {
    render(<Editor initial={[monday]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Duplicate time block 1' }))
    fireEvent.change(screen.getByLabelText('Day 2'), { target: { value: 'WEDNESDAY' } })
    fireEvent.change(screen.getByLabelText('Start 2'), { target: { value: '19:00' } })
    const slots: WeeklySlot[] = JSON.parse(screen.getByLabelText('Saved input').textContent!)
    expect(slots[0]).toEqual(monday)
    expect(slots[1]).toEqual({ ...monday, dayOfWeek: 'WEDNESDAY', startTime: '19:00' })
    const preview = screen.getByRole('region', { name: 'Weekly schedule preview' })
    expect(within(preview).getByText('18:00–20:00')).toBeInTheDocument()
    expect(within(preview).getByText('19:00–20:00')).toBeInTheDocument()
  })
  it('copies to selected days, preserving an identical existing block once', () => {
    render(<Editor initial={[monday, { ...monday, dayOfWeek: 'TUESDAY', startTime: '18:00', endTime: '20:00' }]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy time block 1 to other days' }))
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByRole('button', { name: 'Copy time block' })).toBeDisabled()
    fireEvent.click(within(dialog).getByLabelText('Tuesday'))
    fireEvent.click(within(dialog).getByLabelText('Friday'))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Copy time block' }))
    const slots: WeeklySlot[] = JSON.parse(screen.getByLabelText('Saved input').textContent!)
    expect(slots).toHaveLength(3)
    expect(slots[2]).toEqual({ ...monday, dayOfWeek: 'FRIDAY' })
    expect(screen.getByText('Time block copied to 1 day.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
  it('keeps invalid copied time blocks subject to the existing validation', () => {
    render(<Editor initial={[{ ...monday, endTime: '17:00' }]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Duplicate time block 1' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Time block 1 must end after it starts.')
  })
  it('sorts the preview within a day while preserving input order', () => {
    const initial = [monday, { ...monday, startTime: '09:00', endTime: '10:00' }]
    render(<Editor initial={initial} />)
    const preview = screen.getByRole('region', { name: 'Weekly schedule preview' })
    const times = Array.from(preview.querySelectorAll('dd span')).map(item => item.textContent)
    expect(times).toEqual(['09:00–10:00', '18:00–20:00'])
    expect(JSON.parse(screen.getByLabelText('Saved input').textContent!)).toEqual(initial)
  })
})
