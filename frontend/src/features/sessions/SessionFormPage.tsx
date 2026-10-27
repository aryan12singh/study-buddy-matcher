import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { Link, useParams } from 'react-router-dom'
import { createSession, getAvailability, getSession, updateSession } from './api'
import type { SessionScope, StudySession } from './api'
import { initialSessionForm, nextSuggestedTimes, sessionInput, suggestionDurationMinutes, validateSessionForm } from './sessionForm'
import type { SessionForm } from './sessionForm'
import { sessionParent } from './sessionPresentation'
import { scopeListPath } from './api'
import { getGroup } from '../groups/api'
import { getConnections } from '../connections/api'
import { useResource } from '../../shared/api/useResource'
import { errorMessage, fieldErrors, refreshResources } from '../../shared/api/client'
import { useAuth } from '../../shared/auth/useAuth'
import { useFormExit } from '../../shared/components/useFormExit'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import FormActions from '../../shared/components/FormActions'
import WeeklySchedule from '../../shared/components/WeeklySchedule'
import { label } from '../../shared/api/types'
import './sessions.css'

type EditorContext = { scope: SessionScope; session?: StudySession; name: string; canManage: boolean }
export default function SessionFormPage({ kind }: { kind?: 'GROUP' | 'BUDDY' }) {
  const { id } = useParams()
  const { account } = useAuth()
  return <SessionFormView key={`${account?.id}-${kind}-${id}`} id={Number(id)} kind={kind} />
}
function SessionFormView({ id, kind }: { id: number; kind?: 'GROUP' | 'BUDDY' }) {
  const { account } = useAuth()
  const valid = Number.isSafeInteger(id) && id > 0
  const context = useResource<EditorContext>(`session-editor-${account?.id}-${kind}-${id}`, async signal => {
    if (!kind) {
      const session = await getSession(id, signal)
      return { scope: { kind: session.scope, id: session.groupId ?? session.connectionId! }, session, name: session.title,
        canManage: session.canManage && session.status === 'SCHEDULED' }
    }
    if (kind === 'GROUP') {
      const group = await getGroup(id, signal)
      return { scope: { kind, id }, name: group.name, canManage: group.active && group.viewer.leader }
    }
    const connection = (await getConnections(signal)).find(item => item.id === id)
    return { scope: { kind, id }, name: connection?.otherStudentName ?? 'Buddy', canManage: !!connection }
  }, valid, false)
  return <WindowPage title={kind ? 'Plan study session' : 'Edit study session'} description={context.data ? `For ${context.data.name}` : 'Save a dated plan, including any weekly repeat dates.'}
    actions={<Link className="retro-button" to={kind ? scopeListPath({ kind, id }) : `/sessions/${id}`}>Cancel</Link>}>
    <StatePanel loading={context.loading && !context.data} error={valid ? context.error : 'This session link is invalid.'} onRetry={valid ? () => context.reload() : undefined} />
    {context.data && (context.data.canManage ? <SessionEditor key={`${kind}-${id}`} context={context.data} />
      : <StatePanel error="Only the group leader or a participant in an active buddy connection can manage an active session." />)}
  </WindowPage>
}
function SessionEditor({ context }: { context: EditorContext }) {
  const { account } = useAuth()
  const navigate = useViewNavigation()
  const [form, setForm] = useState<SessionForm>(() => initialSessionForm(context.session, context.scope.kind === 'GROUP' ? context.scope.id : undefined))
  const [baseline, setBaseline] = useState(() => JSON.stringify(form))
  const [pending, setPending] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string>()
  const [denied, setDenied] = useState(false)
  const [discard, setDiscard] = useState(false)
  const [suggestionDuration, setSuggestionDuration] = useState('60')
  const alive = useRef(true)
  useEffect(() => { alive.current = true; return () => { alive.current = false } }, [])
  const dirty = JSON.stringify(form) !== baseline
  const exit = useFormExit(dirty && !denied, pending)
  const availability = useResource(`session-availability-${account?.id}-${context.scope.kind}-${context.scope.id}`,
    signal => getAvailability(context.scope, signal), true, false)
  const groupId = context.scope.kind === 'GROUP' ? context.scope.id : undefined
  const durationMinutes = suggestionDurationMinutes(suggestionDuration)
  const suggestions = durationMinutes === null ? [] : availability.data?.sharedSlots.flatMap(slot => {
    const times = nextSuggestedTimes(slot, durationMinutes)
    return times ? [{ slot, times }] : []
  }) ?? []
  function edit(field: keyof SessionForm, value: string) {
    setForm(previous => ({ ...previous, [field]: value }))
    setErrors(previous => { const next = { ...previous }; delete next[field]; return next })
  }
  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending || denied) return
    const invalid = validateSessionForm(form, groupId)
    setErrors(invalid); setError(undefined)
    if (Object.keys(invalid).length) return
    setPending(true)
    try {
      const data = context.session ? await updateSession(context.session.id, sessionInput(form), context.session.version)
        : await createSession(context.scope, sessionInput(form))
      if (!alive.current) return
      setBaseline(JSON.stringify(form)); exit.allowSavedNavigation()
      refreshResources()
      navigate(`/sessions/${data.id}`, { state: { notice: context.session ? 'Study session updated.' : 'Study session created.' } })
    } catch (failure) {
      if (!alive.current) return
      setError(errorMessage(failure)); setErrors(fieldErrors(failure))
      if (axios.isAxiosError(failure) && [401, 403, 404].includes(failure.response?.status ?? 0)) {
        setDenied(true); setForm(initialSessionForm()); setBaseline(JSON.stringify(initialSessionForm()))
      }
    } finally { if (alive.current) setPending(false) }
  }
  return <>
    {!denied && <form noValidate onSubmit={submit} className="stack-form session-form">
      <div className="form-grid">
        <Field id="session-title" label="Session title" error={errors.title}><input id="session-title" value={form.title} maxLength={255} disabled={pending} onChange={event => edit('title', event.target.value)} /></Field>
        <Field id="session-location" label="Location (optional)" error={errors.location}><input id="session-location" value={form.location} maxLength={255} disabled={pending} onChange={event => edit('location', event.target.value)} /></Field>
        <Field id="session-start" label="Starts" error={errors.startLocal}><input id="session-start" type="datetime-local" step="60" value={form.startLocal} disabled={pending} onChange={event => edit('startLocal', event.target.value)} /></Field>
        <Field id="session-end" label="Ends" error={errors.endLocal}><input id="session-end" type="datetime-local" step="60" value={form.endLocal} disabled={pending} onChange={event => edit('endLocal', event.target.value)} /></Field>
      </div>
      <fieldset className="fieldset" disabled={pending}>
        <legend>How often?</legend>
        <div className="check-options">
          <label><input type="radio" name="session-repeat" checked={form.repeat === 'ONCE'} onChange={() => edit('repeat', 'ONCE')} /> One-off session</label>
          <label><input type="radio" name="session-repeat" checked={form.repeat === 'WEEKLY'} onChange={() => edit('repeat', 'WEEKLY')} /> Repeats weekly</label>
        </div>
      </fieldset>
      {form.repeat === 'WEEKLY' && <Field id="session-count" label="Number of weekly sessions" error={errors.occurrenceCount} hint="Between 2 and 52, starting on the date above.">
        <input id="session-count" type="number" min="2" max="52" step="1" value={form.occurrenceCount} disabled={pending} onChange={event => edit('occurrenceCount', event.target.value)} />
      </Field>}
      <Field id="session-agenda" label="Agenda (optional)" error={errors.agenda}><textarea id="session-agenda" value={form.agenda} maxLength={4000} disabled={pending} onChange={event => edit('agenda', event.target.value)} /></Field>
      <Field id="session-link" label="Room or meeting link (optional)" error={errors.roomLink} hint={groupId
        ? `Where everyone joins the session. Keep /groups/${groupId}/room to meet in this group's study room (shared focus timer and music), or paste an online meeting link such as Zoom or Teams (https://…, no passwords or ?query). A meeting link also goes into calendar exports.`
        : 'Where you both join the session: paste an online meeting link such as Zoom or Teams (https://…, no passwords or ?query). It is shown on the session page and included in calendar exports.'}>
        <input id="session-link" value={form.roomLink} maxLength={2048} disabled={pending} onChange={event => edit('roomLink', event.target.value)} />
      </Field>
      <section className="detail-panel session-availability">
        <h2>Mutual weekly availability</h2>
        <p className="muted">These suggestions use current participants’ weekly availability. They suggest a time, not confirmed attendance.</p>
        <Field id="session-suggestion-duration" label="Suggested session duration (minutes)"
          error={durationMinutes === null ? 'Enter a positive whole number of minutes for suggestions.' : undefined}
          hint="Defaults to 60 minutes. Changing this only filters suggestions; your entered dates stay unchanged until you choose a time.">
          <input id="session-suggestion-duration" type="number" min="1" step="1" value={suggestionDuration} disabled={pending} onChange={event => setSuggestionDuration(event.target.value)} />
        </Field>
        <StatePanel loading={availability.loading && !availability.data} error={availability.error} onRetry={() => availability.reload()} />
        {availability.data && <><p>{availability.data.participantCount} participants · {availability.data.sharedMinutes} shared minutes each week</p>
          <WeeklySchedule slots={availability.data.sharedSlots} />
          <div className="actions">{suggestions.map(({ slot, times }, index) => <Button key={index} disabled={pending} onClick={() => {
            const currentTimes = durationMinutes ? nextSuggestedTimes(slot, durationMinutes) : null
            if (!currentTimes) return
            setForm(previous => ({ ...previous, ...currentTimes })); setErrors(previous => { const next = { ...previous }; delete next.startLocal; delete next.endLocal; return next })
          }}>Use next {label(slot.dayOfWeek)} {times.startLocal.slice(11)}–{times.endLocal.slice(11)} ({durationMinutes} min)</Button>)}</div>
          {!availability.data.sharedSlots.length && <p className="muted">No shared weekly time is saved. Agree a date together and enter it manually.</p>}
          {!!availability.data.sharedSlots.length && durationMinutes !== null && !suggestions.length && <p className="muted">No shared weekly window fits {durationMinutes} minutes. Choose a shorter duration or agree a date together and enter it manually.</p>}
        </>}
      </section>
      {error && <p className="field-error" role="alert">{error}</p>}
      <FormActions dirty={dirty} pending={pending}>
        <Button type="submit" variant="primary" disabled={pending}>{pending ? 'Saving…' : context.session ? 'Save session changes' : 'Create study session'}</Button>
        <Button disabled={pending || !dirty} onClick={() => setDiscard(true)}>Discard session edits</Button>
        <Link className="retro-button" to={context.session ? `/sessions/${context.session.id}` : scopeListPath(context.scope)}>Cancel</Link>
      </FormActions>
    </form>}
    {denied && <StatePanel error={error || 'Session access is no longer available.'} />}
    {exit.confirmation}
    {discard && <Dialog title="Discard session edits?" onClose={() => setDiscard(false)}><p>Your edits will be replaced with the saved values.</p>
      <div className="actions"><Button onClick={() => setDiscard(false)}>Keep editing</Button><Button variant="danger" onClick={() => {
        setForm(initialSessionForm(context.session, groupId)); setErrors({}); setError(undefined); setDiscard(false)
      }}>Discard changes</Button></div></Dialog>}
    {context.session && error && !denied && <p className="muted">If another participant changed this plan, keep a copy of your edits, then <Link to={sessionParent(context.session)}>return to the session list</Link> and reopen the latest version.</p>}
  </>
}
