import { useState } from 'react'
import type { RoomConfiguration, StudyRoom } from './api'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import { useFormExit } from '../../shared/components/useFormExit'
import Dialog from '../../shared/components/Dialog'

type Form = { focusMinutes: string; breakMinutes: string; participantLimit: string; hostId: string; coHostId: string }
const fromRoom = (room: StudyRoom): Form => ({ focusMinutes: String(room.focusMinutes), breakMinutes: String(room.breakMinutes),
  participantLimit: String(room.participantLimit), hostId: String(room.hostId), coHostId: room.coHostId ? String(room.coHostId) : '' })
export default function RoomSettingsForm({ room, pending, onSave, serverErrors }: {
  room: StudyRoom; pending: boolean; onSave: (input: RoomConfiguration) => Promise<boolean>; serverErrors: Record<string, string>
}) {
  const [draft, setDraft] = useState<{ form: Form; original: Form; version: number } | null>(null)
  const [discard, setDiscard] = useState(false)
  const form = draft?.form ?? fromRoom(room)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const dirty = draft !== null && JSON.stringify(form) !== JSON.stringify(draft.original)
  const displayedErrors = { ...serverErrors, ...errors }
  const exit = useFormExit(dirty, pending)
  function edit(field: keyof Form, value: string) {
    setDraft(current => {
      const initial = current ?? { form: fromRoom(room), original: fromRoom(room), version: room.version }
      return { ...initial, form: { ...initial.form, [field]: value } }
    })
  }

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid: Record<string, string> = {}
    for (const [field, maximum] of [['focusMinutes', room.maxFocusMinutes], ['breakMinutes', room.maxBreakMinutes], ['participantLimit', room.groupLimit]] as const) {
      const value = Number(form[field])
      if (!Number.isInteger(value) || value < 1 || value > maximum) invalid[field] = `Choose a whole number from 1 to ${maximum}.`
    }
    if (form.hostId && form.coHostId === form.hostId) invalid.coHostId = 'Choose a different co-host.'
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const saved = await onSave({ focusMinutes: Number(form.focusMinutes), breakMinutes: Number(form.breakMinutes),
      participantLimit: Number(form.participantLimit), hostId: form.hostId ? Number(form.hostId) : null,
      coHostId: form.coHostId ? Number(form.coHostId) : null, expectedVersion: draft?.version ?? room.version })
    if (saved) setDraft(null)
  }
  return <section className="detail-panel">
    <h2>Leader settings</h2>
    <p className="muted">Reset the timer before changing durations. The leader retains controls even when a host is disconnected.</p>
    <form className="stack-form" noValidate onSubmit={submit}>
      <div className="form-grid">
        <Field id="focus-minutes" label="Focus minutes" error={displayedErrors.focusMinutes}>
          <input id="focus-minutes" type="number" min="1" max={room.maxFocusMinutes} value={form.focusMinutes} disabled={pending}
            onChange={event => edit('focusMinutes', event.target.value)} />
        </Field>
        <Field id="break-minutes" label="Break minutes" error={displayedErrors.breakMinutes}>
          <input id="break-minutes" type="number" min="1" max={room.maxBreakMinutes} value={form.breakMinutes} disabled={pending}
            onChange={event => edit('breakMinutes', event.target.value)} />
        </Field>
        <Field id="participant-limit" label="Room capacity" error={displayedErrors.participantLimit}>
          <input id="participant-limit" type="number" min="1" max={room.groupLimit} value={form.participantLimit} disabled={pending}
            onChange={event => edit('participantLimit', event.target.value)} />
        </Field>
        <Field id="room-host" label="Host" error={displayedErrors.hostId}>
          <select id="room-host" value={form.hostId} disabled={pending} onChange={event => edit('hostId', event.target.value)}>
            {room.members.map(member => <option key={member.studentId} value={member.studentId}>{member.name}</option>)}
          </select>
        </Field>
        <Field id="room-co-host" label="Co-host" error={displayedErrors.coHostId}>
          <select id="room-co-host" value={form.coHostId} disabled={pending} onChange={event => edit('coHostId', event.target.value)}>
            <option value="">No co-host</option>
            {room.members.map(member => <option key={member.studentId} value={member.studentId}>{member.name}</option>)}
          </select>
        </Field>
      </div>
      <div className="actions">
        <Button type="submit" variant="primary" disabled={pending || !dirty}>Save room settings</Button>
        <Button disabled={pending || !draft} onClick={() => setDiscard(true)}>Discard settings changes</Button>
      </div>
    </form>
    {exit.confirmation}
    {discard && <Dialog title="Discard room settings?" onClose={() => setDiscard(false)}>
      <p>Your edits will be replaced with the latest saved settings.</p>
      <div className="actions">
        <Button onClick={() => setDiscard(false)}>Keep editing</Button>
        <Button variant="danger" onClick={() => { setDraft(null); setErrors({}); setDiscard(false) }}>Discard changes</Button>
      </div>
    </Dialog>}
  </section>
}
