import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import type { WeeklySlot } from '../api/types'
import { validateSlots } from '../api/schedules'
import WeeklySlotEditor from './WeeklySlotEditor'
import type { WeeklySlotDrafts } from './weeklySlotDrafts'

function Editor({ initial, allowOverlaps = false }: { initial: WeeklySlot[]; allowOverlaps?: boolean }) {
  const [slots, setSlots] = useState(initial)
  const [drafts, setDrafts] = useState<WeeklySlotDrafts>({})
  return <>
    <WeeklySlotEditor value={slots} onChange={setSlots} drafts={drafts} onDraftsChange={setDrafts}
      error={validateSlots(slots)} allowOverlaps={allowOverlaps} />
    <output aria-label="Saved input">{JSON.stringify(slots)}</output>
    <button onClick={() => { setSlots(initial); setDrafts({}) }}>Reset schedule</button>
  </>
}
const saved = (): WeeklySlot[] => JSON.parse(screen.getByLabelText('Saved input').textContent!)
const monday: WeeklySlot = { dayOfWeek: 'MONDAY', startTime: '18:00:00', endTime: '20:00:00' }

describe('weekly editor conveniences', () => {
  it('duplicates to the next free day, directly below, without changing the original', () => {
    const friday: WeeklySlot = { dayOfWeek: 'FRIDAY', startTime: '12:00', endTime: '14:00' }
    render(<Editor initial={[monday, friday]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Duplicate time 1' }))
    expect(saved()).toEqual([monday, { ...monday, dayOfWeek: 'TUESDAY' }, friday])
    expect(screen.getByText('Copied to Tuesday. Change the day if needed.')).toHaveAttribute('role', 'status')
    expect(screen.getByLabelText('Day 2')).toHaveFocus()
    const preview = screen.getByRole('region', { name: 'Weekly schedule preview' })
    expect(within(preview).getAllByText('18:00–20:00')).toHaveLength(2)
  })
  it('skips days that already have a clashing block when duplicating', () => {
    render(<Editor initial={[monday, { ...monday, dayOfWeek: 'TUESDAY', startTime: '19:00', endTime: '21:00' }]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Duplicate time 1' }))
    expect(saved()[1].dayOfWeek).toBe('WEDNESDAY')
  })
  it('copies to selected days, preserving an identical existing block once', () => {
    render(<Editor initial={[monday, { ...monday, dayOfWeek: 'TUESDAY', startTime: '18:00', endTime: '20:00' }]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy time block 1 to other days' }))
    const dialog = screen.getByRole('dialog')
    expect(within(dialog).getByRole('button', { name: 'Copy time block' })).toBeDisabled()
    fireEvent.click(within(dialog).getByLabelText('Tuesday'))
    fireEvent.click(within(dialog).getByLabelText('Friday'))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Copy time block' }))
    expect(saved()).toHaveLength(3)
    expect(saved()[2]).toEqual({ ...monday, dayOfWeek: 'FRIDAY' })
    expect(screen.getByText('Time block copied to 1 day.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
  it('shows saved times as 24-hour HH:MM and keeps the end after a moved start', () => {
    render(<Editor initial={[monday]} />)
    expect(screen.getByLabelText('Start 1')).toHaveValue('18:00')
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '21:00' } })
    expect(saved()[0]).toEqual({ dayOfWeek: 'MONDAY', startTime: '21:00', endTime: '22:00' })
    fireEvent.change(screen.getByLabelText('End 1'), { target: { value: '22:45' } })
    expect(saved()[0].endTime).toBe('22:45')
  })
  it('accepts times typed without a colon', () => {
    render(<Editor initial={[monday]} />)
    fireEvent.change(screen.getByLabelText('End 1'), { target: { value: '2030' } })
    expect(saved()[0].endTime).toBe('20:30')
    expect(screen.getByLabelText('End 1')).toHaveValue('20:30')
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '930' } })
    fireEvent.blur(screen.getByLabelText('Start 1'))
    expect(saved()[0].startTime).toBe('09:30')
  })
  it('waits until the box is left before calling a time invalid, then does not save it', () => {
    render(<Editor initial={[monday]} />)
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '25:00' } })
    expect(screen.queryByText(/is not a valid start time/)).not.toBeInTheDocument()
    fireEvent.blur(screen.getByLabelText('Start 1'))
    expect(screen.getByText('25:00 is not a valid start time. Use 24-hour time from 00:00 to 23:59, e.g. 18:00 for 6 pm.')).toBeInTheDocument()
    expect(screen.getByLabelText('Start 1')).toHaveAttribute('aria-invalid', 'true')
    expect(saved()[0].startTime).toBe('18:00:00')
  })
  it('shows an overlap on both clashing rows', () => {
    render(<Editor initial={[monday, { ...monday, startTime: '19:00', endTime: '21:00' }]} />)
    expect(screen.getByText('This clashes with Monday 19:00–21:00. Change the times or remove one of them.')).toBeInTheDocument()
    expect(screen.getByText('This clashes with Monday 18:00–20:00. Change the times or remove one of them.')).toBeInTheDocument()
  })
  it('allows overlapping group meeting times without marking their rows invalid', () => {
    render(<Editor initial={[monday, { ...monday, startTime: '19:00', endTime: '21:00' }]} allowOverlaps />)
    expect(screen.queryByText(/This clashes with/)).not.toBeInTheDocument()
    expect(screen.getByLabelText('Start 1')).not.toHaveAttribute('aria-invalid')
    expect(screen.getByLabelText('Start 2')).not.toHaveAttribute('aria-invalid')
  })
  it('keeps a later invalid draft on its own row when an earlier row is duplicated', () => {
    const friday: WeeklySlot = { ...monday, dayOfWeek: 'FRIDAY' }
    render(<Editor initial={[monday, friday]} />)
    fireEvent.change(screen.getByLabelText('Start 2'), { target: { value: '25:00' } })
    fireEvent.blur(screen.getByLabelText('Start 2'))
    fireEvent.click(screen.getByRole('button', { name: 'Duplicate time 1' }))
    expect(screen.getByLabelText('Start 2')).toHaveValue('18:00')
    expect(screen.getByLabelText('Start 3')).toHaveValue('25:00')
    expect(screen.getByLabelText('Start 3')).toHaveAttribute('aria-invalid', 'true')
    expect(saved()[2]).toEqual(friday)
  })
  it('preserves an invalid draft when another row is removed, then removes it with its own row', () => {
    const friday: WeeklySlot = { ...monday, dayOfWeek: 'FRIDAY' }
    render(<Editor initial={[monday, friday]} />)
    fireEvent.change(screen.getByLabelText('End 2'), { target: { value: '' } })
    fireEvent.blur(screen.getByLabelText('End 2'))
    fireEvent.click(screen.getByRole('button', { name: 'Remove time 1' }))
    expect(screen.getByLabelText('End 1')).toHaveValue('')
    expect(screen.getByText('Enter the end time.')).toBeInTheDocument()
    expect(saved()).toEqual([friday])
    fireEvent.click(screen.getByRole('button', { name: 'Remove time 1' }))
    fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    expect(screen.getByLabelText('End 1')).toHaveValue('10:00')
    expect(screen.queryByText('Enter the end time.')).not.toBeInTheDocument()
  })
  it('keeps other drafts when copying to days and requires correcting a draft before copying its row', () => {
    render(<Editor initial={[monday, { ...monday, dayOfWeek: 'FRIDAY' }]} />)
    fireEvent.change(screen.getByLabelText('Start 2'), { target: { value: '25:00' } })
    fireEvent.blur(screen.getByLabelText('Start 2'))
    expect(screen.getByRole('button', { name: 'Duplicate time 2' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Copy time block 2 to other days' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Copy time block 1 to other days' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByLabelText('Tuesday'))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Copy time block' }))
    expect(screen.getByLabelText('Start 2')).toHaveValue('25:00')
    fireEvent.change(screen.getByLabelText('Start 2'), { target: { value: '18:30' } })
    expect(screen.getByRole('button', { name: 'Duplicate time 2' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Copy time block 2 to other days' })).toBeEnabled()
  })
  it('clears an invalid draft when the owning form resets its schedule', () => {
    render(<Editor initial={[monday]} />)
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '25:00' } })
    fireEvent.blur(screen.getByLabelText('Start 1'))
    fireEvent.click(screen.getByRole('button', { name: 'Reset schedule' }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('18:00')
    expect(screen.queryByText(/is not a valid start time/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Duplicate time 1' })).toBeEnabled()
  })
  it('keeps the summary error and a row error for a block that ends before it starts', () => {
    render(<Editor initial={[{ ...monday, endTime: '17:00' }]} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Time block 1 must end after it starts.')
    expect(screen.getByText('This ends at 17:00, before it starts at 18:00. To study past midnight, end at 23:59 and add a new time on the next day from 00:00.')).toBeInTheDocument()
  })
  it('sorts the preview within a day while preserving input order', () => {
    const initial = [monday, { ...monday, startTime: '09:00', endTime: '10:00' }]
    render(<Editor initial={initial} />)
    const preview = screen.getByRole('region', { name: 'Weekly schedule preview' })
    const times = Array.from(preview.querySelectorAll('dd span')).map(item => item.textContent)
    expect(times).toEqual(['09:00–10:00', '18:00–20:00'])
    expect(saved()).toEqual(initial)
  })
})
