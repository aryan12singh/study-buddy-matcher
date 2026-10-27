import { useEffect, useId, useState } from 'react'
import type { ReactNode } from 'react'
import type { WeeklySlot } from '../api/types'
import { label, WEEK_DAYS } from '../api/types'
import Button from './Button'
import Dialog from './Dialog'
import WeeklySchedule from './WeeklySchedule'
import { changedTime, minutesOf, parseTime } from './weeklySlotDrafts'
import type { SlotEdge, WeeklySlotDrafts } from './weeklySlotDrafts'

function overlaps(left: WeeklySlot, right: WeeklySlot) {
  return left.dayOfWeek === right.dayOfWeek
    && minutesOf(left.startTime) < minutesOf(right.endTime) && minutesOf(right.startTime) < minutesOf(left.endTime)
}

function describe(slot: WeeklySlot) {
  return `${label(slot.dayOfWeek)} ${slot.startTime.slice(0, 5)}–${slot.endTime.slice(0, 5)}`
}

/** The next day (wrapping round the week) where a copy of this time clashes with nothing. */
function nextFreeDay(slots: WeeklySlot[], slot: WeeklySlot) {
  const from = WEEK_DAYS.indexOf(slot.dayOfWeek)
  for (let step = 1; step < WEEK_DAYS.length; step++) {
    const candidate = { ...slot, dayOfWeek: WEEK_DAYS[(from + step) % WEEK_DAYS.length] }
    if (!slots.some(other => overlaps(candidate, other))) return candidate.dayOfWeek
  }
  return slot.dayOfWeek
}

/**
 * Weekly times typed as 24-hour HH:MM, so every browser shows the same format
 * as the preview. Problems are shown on the row they belong to; the `error`
 * prop is the screen's own summary, shown under the list. `empty` is what the
 * screen wants shown inside the editor while there are no times.
 */
export default function WeeklySlotEditor({ value, onChange, drafts, onDraftsChange, error, disabled, empty, allowOverlaps = false }: {
  value: WeeklySlot[]
  onChange: (slots: WeeklySlot[]) => void
  drafts: WeeklySlotDrafts
  onDraftsChange: (drafts: WeeklySlotDrafts) => void
  error?: string
  disabled?: boolean
  empty?: ReactNode
  allowOverlaps?: boolean
}) {
  const prefix = useId()
  // The owning form keeps drafts so invalid text is unsaved work, including across tabs.
  const [added, setAdded] = useState<{ index: number; message: string } | null>(null)
  const [copying, setCopying] = useState<{ slot: WeeklySlot; days: WeeklySlot['dayOfWeek'][] } | null>(null)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (added) document.getElementById(`${prefix}-${added.index}-day`)?.focus()
  }, [added, prefix])

  function replace(slots: WeeklySlot[]) {
    setAdded(null)
    setNotice('')
    onChange(slots)
  }
  /** Adds the chosen time to every ticked day, keeping an identical existing time only once. */
  function copyDays() {
    if (!copying || disabled) return
    const additions = copying.days.filter(day => !value.some(slot => slot.dayOfWeek === day &&
      slot.startTime.slice(0, 5) === copying.slot.startTime.slice(0, 5) && slot.endTime.slice(0, 5) === copying.slot.endTime.slice(0, 5)))
      .map(dayOfWeek => ({ ...copying.slot, dayOfWeek }))
    replace([...value, ...additions])
    setNotice(additions.length ? `Time block copied to ${additions.length} ${additions.length === 1 ? 'day' : 'days'}.` : 'Selected days already have this time block.')
    setCopying(null)
  }
  function update(index: number, next: Partial<WeeklySlot>) {
    replace(value.map((slot, item) => item === index ? { ...slot, ...next } : slot))
  }
  function commit(index: number, edge: SlotEdge, time: string) {
    replace(value.map((slot, item) => item === index ? changedTime(slot, edge, time) : slot))
  }
  function setDraft(index: number, edge: SlotEdge, draft?: WeeklySlotDrafts[number][SlotEdge]) {
    const row = { ...drafts[index] }
    if (draft) row[edge] = draft
    else delete row[edge]
    const next = { ...drafts }
    if (Object.keys(row).length) next[index] = row
    else delete next[index]
    onDraftsChange(next)
  }
  function edit(index: number, edge: SlotEdge, raw: string) {
    const text = raw.replace(/[^\d:]/g, '').slice(0, 5)
    const time = parseTime(text, false)
    if (time) {
      setDraft(index, edge)
      commit(index, edge, time)
    } else {
      setDraft(index, edge, { text, finished: false })
    }
  }
  function finish(index: number, edge: SlotEdge) {
    const draft = drafts[index]?.[edge]
    if (!draft) return
    const time = parseTime(draft.text, true)
    if (time) {
      setDraft(index, edge)
      commit(index, edge, time)
    } else {
      setDraft(index, edge, { ...draft, finished: true })
    }
  }
  function duplicate(index: number) {
    if (drafts[index]) return
    const copy = { ...value[index], dayOfWeek: nextFreeDay(value, value[index]) }
    onDraftsChange(Object.fromEntries(Object.entries(drafts).map(([row, draft]) => [
      Number(row) > index ? Number(row) + 1 : Number(row), draft
    ])))
    onChange([...value.slice(0, index + 1), copy, ...value.slice(index + 1)])
    setAdded({
      index: index + 1,
      message: copy.dayOfWeek === value[index].dayOfWeek
        ? 'Copied. Every other day already has this time, so change the day or times.'
        : `Copied to ${label(copy.dayOfWeek)}. Change the day if needed.`
    })
  }
  function remove(index: number) {
    onDraftsChange(Object.fromEntries(Object.entries(drafts).filter(([row]) => Number(row) !== index).map(([row, draft]) => [
      Number(row) > index ? Number(row) - 1 : Number(row), draft
    ])))
    replace(value.filter((_, item) => item !== index))
  }

  /** Plain-language explanation of what is wrong with a row and how to fix it. */
  function rowProblem(index: number) {
    const slot = value[index]
    for (const edge of ['startTime', 'endTime'] as SlotEdge[]) {
      const draft = drafts[index]?.[edge]
      if (!draft?.finished) continue
      const which = edge === 'startTime' ? 'start' : 'end'
      return draft.text === ''
        ? `Enter the ${which} time.`
        : `${draft.text} is not a valid ${which} time. Use 24-hour time from 00:00 to 23:59, e.g. 18:00 for 6 pm.`
    }
    const start = slot.startTime.slice(0, 5), end = slot.endTime.slice(0, 5)
    if (minutesOf(start) === minutesOf(end)) return `This starts and ends at ${start}. Choose an end time later than the start.`
    if (minutesOf(start) > minutesOf(end)) {
      return `This ends at ${end}, before it starts at ${start}. To study past midnight, end at 23:59 and add a new time on the next day from 00:00.`
    }
    if (allowOverlaps) return undefined
    const clashes = value.filter((other, item) => item !== index && overlaps(slot, other))
    return clashes.length
      ? `This clashes with ${clashes.map(describe).join(' and ')}. Change the times or remove one of them.`
      : undefined
  }

  function timeBox(index: number, slot: WeeklySlot, edge: SlotEdge, name: string, describedBy?: string) {
    const id = `${prefix}-${index}-${edge}`
    return (
      <div>
        <label className="visually-hidden" htmlFor={id}>{`${name} ${index + 1}`}</label>
        <input
          id={id}
          className="time-input"
          inputMode="numeric"
          autoComplete="off"
          placeholder="HH:MM"
          maxLength={5}
          value={drafts[index]?.[edge]?.text ?? slot[edge].slice(0, 5)}
          aria-invalid={describedBy ? true : undefined}
          aria-describedby={describedBy}
          onChange={event => edit(index, edge, event.target.value)}
          onBlur={() => finish(index, edge)}
        />
      </div>
    )
  }

  return (
    <fieldset className="weekly-editor" disabled={disabled}>
      <legend>Weekly availability</legend>
      <p className="field-hint">24-hour time. Leave empty if a weekly time has not been agreed.</p>
      {value.length === 0 && empty}
      {value.length > 0 && (
        <div className="slot-row slot-head" aria-hidden="true">
          <span>Day</span><span>Start</span><span /><span>End</span><span />
        </div>
      )}
      {value.map((slot, index) => {
        const problem = rowProblem(index)
        const isNew = added?.index === index
        const messageId = `${prefix}-${index}-message`
        return (
          <div className={`slot-row${problem ? ' slot-invalid' : isNew ? ' slot-new' : ''}`} key={index}>
            <div>
              <label className="visually-hidden" htmlFor={`${prefix}-${index}-day`}>Day {index + 1}</label>
              <select
                id={`${prefix}-${index}-day`}
                value={slot.dayOfWeek}
                aria-describedby={problem || isNew ? messageId : undefined}
                onChange={event => update(index, { dayOfWeek: event.target.value as WeeklySlot['dayOfWeek'] })}
              >{WEEK_DAYS.map(day => (
                <option key={day} value={day}>{label(day)}</option>
              ))}</select>
            </div>
            {timeBox(index, slot, 'startTime', 'Start', problem ? messageId : undefined)}
            <span className="slot-to" aria-hidden="true">to</span>
            {timeBox(index, slot, 'endTime', 'End', problem ? messageId : undefined)}
            <div className="slot-actions">
              <Button aria-label={`Duplicate time ${index + 1}`} disabled={Boolean(drafts[index])} onClick={() => duplicate(index)}>Duplicate</Button>
              <Button aria-label={`Copy time block ${index + 1} to other days`} disabled={Boolean(drafts[index])} onClick={() => setCopying({ slot: { ...slot }, days: [] })}>Copy to days</Button>
              <Button aria-label={`Remove time ${index + 1}`} onClick={() => remove(index)}>Remove</Button>
            </div>
            {problem && <p className="slot-message slot-error" id={messageId}><span aria-hidden="true">! </span>{problem}</p>}
            {!problem && isNew && <p className="slot-message slot-added" id={messageId} role="status"><span aria-hidden="true">✓ </span>{added.message}</p>}
          </div>
        )
      })}
      <Button
        onClick={() => replace([...value, { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }])}
      >Add time block</Button>
      {error && (
        <p className="field-error" role="alert">{error}</p>
      )}
      {notice && <p className="copy-feedback" role="status">{notice}</p>}
      <section className="schedule-preview" aria-label="Weekly schedule preview">
        <h3>Schedule preview</h3>
        <p className="field-hint">Check the week below before saving your changes.</p>
        <WeeklySchedule slots={value} />
      </section>
      {copying && <Dialog title="Copy time block to other days" onClose={() => setCopying(null)} busy={disabled}>
        <p>Copy {copying.slot.startTime.slice(0, 5) || '--:--'}–{copying.slot.endTime.slice(0, 5) || '--:--'} to the selected days. Identical existing blocks are kept once.</p>
        <fieldset className="fieldset" disabled={disabled}>
          <legend>Choose days</legend>
          <div className="check-options">{WEEK_DAYS.filter(day => day !== copying.slot.dayOfWeek).map(day => <label key={day}>
            <input type="checkbox" checked={copying.days.includes(day)} onChange={event => setCopying({ ...copying,
              days: event.target.checked ? [...copying.days, day] : copying.days.filter(item => item !== day) })} />
            {label(day)}
          </label>)}</div>
        </fieldset>
        <div className="actions dialog-actions">
          <Button onClick={() => setCopying(null)} disabled={disabled}>Cancel copy</Button>
          <Button variant="primary" onClick={copyDays} disabled={disabled || !copying.days.length}>Copy time block</Button>
        </div>
      </Dialog>}
    </fieldset>
  )
}
