import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { fieldErrors } from '../../shared/api/client'
import { getCourses } from '../../shared/api/courses'
import { useAction } from '../../shared/api/useAction'
import { useResource } from '../../shared/api/useResource'
import { label, STUDY_GOALS } from '../../shared/api/types'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import StatePanel from '../../shared/components/StatePanel'
import { describeAiFailure } from '../quiz/api'
import type { StudySession } from '../sessions/api'
import { createSessionPlan } from './api'
import { EMPTY_MATERIAL, sessionPlanRequest, validateSessionPlanForm } from './studyPlanForm'
import type { SessionPlanForm as Form } from './studyPlanForm'
import StudyMaterialInput from './StudyMaterialInput'

const DESCRIBE_FAILURE = describeAiFailure('Nothing changed. You can try again later, or write the plan in the session’s agenda instead.')

/**
 * Creates a session's roadmap, or replaces its current one after a confirm. A group session
 * studies its group's course; a buddy session has none, so the buddy chooses it here.
 */
export default function SessionPlanForm({ session, replacing, onCreated, onCancel }: {
  session: StudySession
  replacing: boolean
  onCreated: () => void
  onCancel: () => void
}) {
  const needsCourse = session.scope === 'BUDDY'
  const courses = useResource('session-plan-courses', getCourses, needsCourse, false)
  const [form, setForm] = useState<Form>({ courseId: '', studyGoal: '', ...EMPTY_MATERIAL })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const action = useAction({ describeError: DESCRIBE_FAILURE })
  const pending = action.pending
  const [confirming, setConfirming] = useState(false)

  function change(next: Partial<Form>) {
    setForm(previous => ({ ...previous, ...next }))
    setErrors(previous => Object.fromEntries(Object.entries(previous).filter(([field]) => !(field in next))))
  }

  function switchMaterial(materialKind: Form['materialKind']) {
    setForm(previous => ({ ...previous, materialKind }))
    setErrors(({ files: _files, topicsText: _topics, ...rest }) => rest)
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateSessionPlanForm(form, needsCourse)
    setErrors(invalid)
    action.clear()
    if (Object.keys(invalid).length) return
    if (replacing) setConfirming(true)
    else void send()
  }

  async function send() {
    const { input, material } = sessionPlanRequest(form, needsCourse)
    const result = await action.run(() => createSessionPlan(session.id, input, material))
    setConfirming(false)
    // Server field messages join the form's own, so each clears once its field changes.
    if (result.ok) onCreated()
    else setErrors(fieldErrors(result.failure))
  }

  if (needsCourse && !courses.data) {
    return <StatePanel loading={courses.loading} error={courses.error} onRetry={() => courses.reload()} />
  }
  return <form className="stack-form session-plan-form" onSubmit={submit} noValidate aria-label="Plan this session">
    <p className="muted">Gemini spreads your material over this session’s upcoming meetings, each as long as the session.</p>
    <div className="form-grid">
      {needsCourse && <Field id="session-plan-course" label="Course" error={errors.courseId}>
        <select id="session-plan-course" value={form.courseId} disabled={pending} onChange={event => change({ courseId: event.target.value })}>
          <option value="">Choose a course</option>
          {courses.data!.map(course => <option key={course.id} value={course.id}>{course.code} — {course.name}</option>)}
        </select>
      </Field>}
      <Field id="session-plan-goal" label="Study goal (optional)">
        <select id="session-plan-goal" value={form.studyGoal} disabled={pending} onChange={event => change({ studyGoal: event.target.value })}>
          <option value="">Not specified</option>
          {STUDY_GOALS.map(goal => <option key={goal} value={goal}>{label(goal)}</option>)}
        </select>
      </Field>
    </div>
    <StudyMaterialInput kind={form.materialKind} files={form.files} topicsText={form.topicsText} errors={errors} disabled={pending}
      onKindChange={switchMaterial} onFilesChange={files => change({ files })} onTopicsChange={topicsText => change({ topicsText })} />
    {pending && <p className="muted" role="status">Reading your material and planning the meetings. This can take up to a minute.</p>}
    <ActionNotice error={action.error} />
    <div className="actions">
      <Button disabled={pending} onClick={onCancel}>Cancel</Button>
      <Button type="submit" variant="primary" disabled={pending}>
        {pending ? 'Drafting roadmap…' : replacing ? 'Replace roadmap' : 'Create session roadmap'}</Button>
    </div>
    {confirming && <ConfirmDialog title="Replace this session’s roadmap?" confirmLabel="Replace roadmap" pending={pending}
      onConfirm={() => void send()} onClose={() => setConfirming(false)}>
      <p>Everyone in the session sees the new roadmap instead, and the topics ticked off on the current one are lost.</p>
    </ConfirmDialog>}
  </form>
}
