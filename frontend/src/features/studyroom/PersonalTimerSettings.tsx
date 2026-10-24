import { useState } from 'react'
import type { FormEvent } from 'react'
import Button from '../../shared/components/Button'
import Field from '../../shared/components/Field'
import { POMODORO_LIMITS } from './usePersonalPomodoro'

type Durations = { focusMinutes: string; breakMinutes: string }

export default function PersonalTimerSettings({ focusMinutes, breakMinutes, onSave }: {
  focusMinutes: number; breakMinutes: number; onSave: (focusMinutes: number, breakMinutes: number) => void
}) {
  const [durations, setDurations] = useState<Durations>({ focusMinutes: String(focusMinutes), breakMinutes: String(breakMinutes) })
  const [errors, setErrors] = useState<Partial<Durations>>({})
  const [saved, setSaved] = useState(false)

  function edit(field: keyof Durations, value: string) {
    setDurations(previous => ({ ...previous, [field]: value }))
    setSaved(false)
  }
  function save(event: FormEvent) {
    event.preventDefault()
    const nextErrors: Partial<Durations> = {}
    for (const field of ['focusMinutes', 'breakMinutes'] as const) {
      const value = Number(durations[field])
      if (!Number.isInteger(value) || value < 1 || value > POMODORO_LIMITS[field]) {
        nextErrors[field] = `Choose a whole number from 1 to ${POMODORO_LIMITS[field]} minutes.`
      }
    }
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return
    onSave(Number(durations.focusMinutes), Number(durations.breakMinutes))
    setSaved(true)
  }

  return <form className="detail-panel" noValidate onSubmit={save}>
    <p className="muted">Find a rhythm that works for you. Saving starts a fresh focus session.</p>
    <div className="form-grid">
      <Field id="personal-focus-minutes" label="Focus minutes" error={errors.focusMinutes}>
        <input id="personal-focus-minutes" type="number" min="1" max={POMODORO_LIMITS.focusMinutes} step="1"
          value={durations.focusMinutes} onChange={event => edit('focusMinutes', event.target.value)} />
      </Field>
      <Field id="personal-break-minutes" label="Break minutes" error={errors.breakMinutes}>
        <input id="personal-break-minutes" type="number" min="1" max={POMODORO_LIMITS.breakMinutes} step="1"
          value={durations.breakMinutes} onChange={event => edit('breakMinutes', event.target.value)} />
      </Field>
    </div>
    <div className="actions"><Button type="submit" variant="primary">Save timer settings</Button></div>
    <p role="status">{saved ? 'Saved. Your fresh focus session is ready.' : ''}</p>
  </form>
}
