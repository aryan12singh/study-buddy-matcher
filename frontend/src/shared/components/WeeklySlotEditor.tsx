import { useId, useState } from 'react'
import type { WeeklySlot } from '../api/types'
import { label, WEEK_DAYS } from '../api/types'
import Button from './Button'
import Dialog from './Dialog'
import WeeklySchedule from './WeeklySchedule'

export default function WeeklySlotEditor({ value, onChange, error, disabled }: {
  value: WeeklySlot[]
  onChange: (slots: WeeklySlot[]) => void
  error?: string
  disabled?: boolean
}) {
  const prefix = useId()
  const [copying, setCopying] = useState<{ slot: WeeklySlot; days: WeeklySlot['dayOfWeek'][] } | null>(null)
  const [notice, setNotice] = useState('')

  function update(index: number, next: Partial<WeeklySlot>) {
    setNotice('')
    onChange(value.map((slot, item) => item === index ? { ...slot, ...next } : slot))
  }
  function copyDays() {
    if (!copying || disabled) return
    const additions = copying.days.filter(day => !value.some(slot => slot.dayOfWeek === day &&
      slot.startTime.slice(0, 5) === copying.slot.startTime.slice(0, 5) && slot.endTime.slice(0, 5) === copying.slot.endTime.slice(0, 5)))
      .map(dayOfWeek => ({ ...copying.slot, dayOfWeek }))
    onChange([...value, ...additions])
    setNotice(additions.length ? `Time block copied to ${additions.length} ${additions.length === 1 ? 'day' : 'days'}.` : 'Selected days already have this time block.')
    setCopying(null)
  }
  return (
    <fieldset className="weekly-editor" disabled={disabled}>
      <legend>Weekly availability</legend>
      <p className="field-hint">Asia/Singapore (SGT). Leave empty if a weekly time has not been agreed.</p>
      {value.map((slot, index) => (
        <div className="slot-row" key={index}>
          <div>
            <label htmlFor={`${prefix}-${index}-day`}>Day {index + 1}</label>
            <select
              id={`${prefix}-${index}-day`}
              value={slot.dayOfWeek}
              onChange={event => update(index, { dayOfWeek: event.target.value as WeeklySlot['dayOfWeek'] })}
            >{WEEK_DAYS.map(day => (
              <option key={day} value={day}>{label(day)}</option>
            ))}</select>
          </div>
          <div>
            <label htmlFor={`${prefix}-${index}-start`}>Start {index + 1}</label>
            <input
              id={`${prefix}-${index}-start`}
              type="time"
              value={slot.startTime.slice(0, 5)}
              onInput={event => update(index, { startTime: event.currentTarget.value })}
              onChange={event => update(index, { startTime: event.target.value })}
            />
          </div>
          <div>
            <label htmlFor={`${prefix}-${index}-end`}>End {index + 1}</label>
            <input
              id={`${prefix}-${index}-end`}
              type="time"
              value={slot.endTime.slice(0, 5)}
              onInput={event => update(index, { endTime: event.currentTarget.value })}
              onChange={event => update(index, { endTime: event.target.value })}
            />
          </div>
          <div className="slot-actions">
            <Button aria-label={`Duplicate time block ${index + 1}`} onClick={() => {
              setNotice('Time block duplicated. Choose its day and times below.')
              onChange([...value, { ...slot }])
            }}>Duplicate</Button>
            <Button aria-label={`Copy time block ${index + 1} to other days`} onClick={() => setCopying({ slot: { ...slot }, days: [] })}>Copy to days</Button>
            <Button aria-label={`Remove time block ${index + 1}`} onClick={() => {
              setNotice('')
              onChange(value.filter((_, item) => item !== index))
            }}>Remove</Button>
          </div>
        </div>
      ))}
      <Button
        onClick={() => onChange([...value, { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }])}
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
        <p>Copy {copying.slot.startTime.slice(0, 5) || '--:--'}–{copying.slot.endTime.slice(0, 5) || '--:--'} (SGT) to the selected days. Identical existing blocks are kept once.</p>
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
