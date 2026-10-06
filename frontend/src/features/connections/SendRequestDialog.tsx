import { useState } from 'react'
import { useAuth } from '../../shared/auth/useAuth'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import type { StudyGoal } from '../../shared/api/types'
import { STUDY_GOALS, label } from '../../shared/api/types'
import { sendMatchRequest } from './api'
import type { MatchRequest, RequestContext } from './api'
import Dialog from '../../shared/components/Dialog'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import StatePanel from '../../shared/components/StatePanel'

/** Reuse from a matching card by passing its known MATCHING context. */

export default function SendRequestDialog({ receiverId, receiverName, context = { origin: 'PROFILE' }, onClose, onSent }: {
  receiverId?: number
  receiverName?: string
  context?: RequestContext
  onClose: () => void
  onSent?: (request: MatchRequest) => void
}) {
  const { account } = useAuth(),
    action = useAction()
  const courses = useResource('request-courses', getCourses)
  const [target, setTarget] = useState(receiverId ? String(receiverId) : '')
  const [message, setMessage] = useState(''),
    [courseId, setCourseId] = useState(context.courseId ? String(context.courseId) : '')
  const [studyGoal, setStudyGoal] = useState(context.studyGoal || '')
  const [errors, setErrors] = useState<Record<string, string>>({})

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid: Record<string, string> = {}
    if (!Number.isSafeInteger(Number(target)) || Number(target) <= 0) invalid.receiverId = 'Enter a valid student ID.'
    else if (Number(target) === account?.id) invalid.receiverId = 'Choose another student.'
    if (message.trim().length > 255) invalid.message = 'Use up to 255 characters.'
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const result = await action.run(
      () => sendMatchRequest(
        Number(target),
        message,
        {
          origin: context.origin,
          courseId: courseId ? Number(courseId) : null,
          studyGoal: studyGoal ? studyGoal as StudyGoal : null
        }
      ),
      'Match request sent.'
    )
    if (result.ok) {
      onSent?.(result.value)
      onClose()
    }
  }
  return (
    <Dialog
      title={`Send match request${receiverName ? ` to ${receiverName}` : ''}`}
      onClose={onClose}
      busy={action.pending}
    >
      <form
        className="stack-form"
        onSubmit={submit}
        noValidate
      >
        {!receiverId && (
          <Field
            id="request-student"
            label="Student ID"
            error={errors.receiverId || action.errors.receiverId}
            hint="Open a student profile from matching to send a request by name."
          >
            <input
              id="request-student"
              type="number"
              min="1"
              step="1"
              value={target}
              onChange={event => setTarget(event.target.value)}
              aria-invalid={Boolean(errors.receiverId)}
            />
          </Field>
        )}
        <Field
          id="request-message"
          label="Message (optional)"
          error={errors.message || action.errors.message}
        >
          <textarea
            id="request-message"
            maxLength={255}
            value={message}
            onChange={event => setMessage(event.target.value)}
            disabled={action.pending}
          />
        </Field>
        <fieldset className="fieldset" disabled={action.pending}>
          <legend>Study context (optional)</legend>
          <StatePanel
            loading={courses.loading && !courses.data}
            error={courses.error}
            onRetry={() => courses.reload()}
          />
          {courses.data && (
            <Field id="request-course" label="Course">
              <select
                id="request-course"
                value={courseId}
                onChange={event => setCourseId(event.target.value)}
              >
                <option value="">No specific course</option>
                {courses.data.map(course => (
                  <option key={course.id} value={course.id}>{course.code} — {course.name}</option>
                ))}
              </select>
            </Field>
          )}
          <Field id="request-goal" label="Study goal">
            <select
              id="request-goal"
              value={studyGoal}
              onChange={event => setStudyGoal(event.target.value as StudyGoal | '')}
            >
              <option value="">No specific goal</option>
              {STUDY_GOALS.map(goal => (
                <option key={goal} value={goal}>{label(goal)}</option>
              ))}
            </select>
          </Field>
        </fieldset>
        <p className="privacy-note">Contact numbers become available only after the request is accepted.</p>
        <ActionNotice error={action.error} />
        <div className="actions">
          <Button disabled={action.pending} onClick={onClose}>Cancel</Button>
          <Button
            type="submit"
            variant="primary"
            disabled={action.pending}
          >{action.pending ? 'Sending…' : 'Send match request'}</Button>
        </div>
      </form>
    </Dialog>
  )
}
