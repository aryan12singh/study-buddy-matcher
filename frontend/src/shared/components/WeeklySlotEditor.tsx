import { useId } from 'react'
import type { WeeklySlot } from '../api/types'
import { label, WEEK_DAYS } from '../api/types'
import Button from './Button'

export default function WeeklySlotEditor({ value, onChange, error, disabled }: {
  value: WeeklySlot[]
  onChange: (slots: WeeklySlot[]) => void
  error?: string
  disabled?: boolean
}) {
  const prefix = useId()

  function update(index: number, next: Partial<WeeklySlot>) {
    onChange(value.map((slot, item) => item === index ? { ...slot, ...next } : slot))
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
          <Button
            aria-label={`Remove time block ${index + 1}`}
            onClick={() => onChange(value.filter((_, item) => item !== index))}
          >Remove</Button>
        </div>
      ))}
      <Button
        onClick={() => onChange([...value, { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }])}
      >Add time block</Button>
      {error && (
        <p className="field-error" role="alert">{error}</p>
      )}
    </fieldset>
  )
}
