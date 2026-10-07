import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getGroup, saveGroup } from './api'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import type { StudyMode } from '../../shared/api/types'
import { STUDY_GOALS, STUDY_MODES, label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import WeeklySlotEditor from '../../shared/components/WeeklySlotEditor'
import { validateGroupForm } from './validation'
import type { GroupForm } from './validation'

export default function GroupFormPage() {
  const { id } = useParams(),
    groupId = id ? Number(id) : undefined
  const validId = !id || Number.isSafeInteger(groupId) && Number(groupId) > 0
  const navigate = useViewNavigation(),
    action = useAction()
  const group = useResource(`edit-group-${id}`, signal => getGroup(groupId!, signal), Boolean(id) && validId, false)
  const courses = useResource('form-courses', getCourses, true, false)
  const initialised = useRef<string | null>(null)
  const [form, setForm] = useState<GroupForm>({
    name: '',
    description: '',
    courseId: '',
    preferredStudyMode: '',
    maxGroupSize: '4',
    studyGoals: [],
    availability: []
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  useEffect(
    () => {
      if (group.data && initialised.current !== id) {
        const data = group.data
        setForm({
          name: data.name,
          description: data.description || '',
          courseId: String(data.courseId),
          preferredStudyMode: data.preferredStudyMode || '',
          maxGroupSize: String(data.maxGroupSize),
          studyGoals: data.studyGoals,
          availability: data.availability
        })
        initialised.current = id || null
      }
    },
    [group.data, id]
  )
  const denied = Boolean(group.data && (!group.data.viewer.leader || !group.data.active))

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateGroupForm(form, group.data?.memberCount || 1)
    setErrors(invalid)
    if (Object.keys(invalid).length || denied) return
    const result = await action.run(
      () => saveGroup(
        {
          ...form,
          name: form.name.trim(),
          description: form.description.trim() || null,
          courseId: Number(form.courseId),
          maxGroupSize: Number(form.maxGroupSize),
          preferredStudyMode: (form.preferredStudyMode || null) as StudyMode | null
        },
        groupId
      ),
      id ? 'Group updated.' : 'Group created.'
    )
    if (result.ok) navigate(`/groups/${result.value.id}`)
  }
  const fieldError = (field: string) => errors[field] || action.errors[field]
  return (
    <WindowPage
      title={id ? 'Edit study group' : 'Create study group'}
      description="Save the course, goals, meeting preferences and full weekly schedule."
      actions={<Link className="retro-button" to={id ? `/groups/${id}` : '/groups'}>Cancel</Link>}
    >
      <StatePanel
        loading={courses.loading && !courses.data || Boolean(id) && group.loading && !group.data}
        error={!validId ? 'This group link is invalid.' : group.error || courses.error}
        onRetry={() => {
          if (id) group.reload()
          courses.reload()
        }}
      />
      {denied &&
        <StatePanel
          error={group.data?.active ? 'Only this group’s leader can edit it.' : 'A closed group cannot be edited.'}
        />}
      {validId && courses.data && (!id || group.data) && !denied && (
        <form
          onSubmit={submit}
          noValidate
          className="stack-form"
        >
          <div className="form-grid">
            <Field
              id="group-name"
              label="Group name"
              error={fieldError('name')}
            >
              <input
                id="group-name"
                value={form.name}
                maxLength={255}
                disabled={action.pending}
                aria-invalid={Boolean(fieldError('name'))}
                onChange={event => setForm({ ...form, name: event.target.value })}
              />
            </Field>
            <Field
              id="group-course"
              label="Course"
              error={fieldError('courseId')}
            >
              <select
                id="group-course"
                value={form.courseId}
                disabled={action.pending}
                aria-invalid={Boolean(fieldError('courseId'))}
                onChange={event => setForm({ ...form, courseId: event.target.value })}
              >
                <option value="">Choose a course</option>
                {courses.data.map(course => (
                  <option key={course.id} value={course.id}>{course.code} — {course.name}</option>
                ))}
              </select>
            </Field>
            <Field
              id="group-capacity"
              label="Maximum group size"
              error={fieldError('maxGroupSize')}
              hint={id ?
                `${group.data?.memberCount} accepted members, including the leader.`
                :
                'At least 2 people, including you as the leader.'}
            >
              <input
                id="group-capacity"
                type="number"
                min={Math.max(2, group.data?.memberCount || 1)}
                step="1"
                value={form.maxGroupSize}
                disabled={action.pending}
                aria-invalid={Boolean(fieldError('maxGroupSize'))}
                onChange={event => setForm({ ...form, maxGroupSize: event.target.value })}
              />
            </Field>
            <Field id="group-mode" label="Meeting mode (optional)">
              <select
                id="group-mode"
                value={form.preferredStudyMode}
                disabled={action.pending}
                onChange={event => setForm({ ...form, preferredStudyMode: event.target.value })}
              >
                <option value="">Not specified</option>
                {STUDY_MODES.map(mode => (
                  <option key={mode} value={mode}>{label(mode)}</option>
                ))}
              </select>
            </Field>
          </div>
          <Field
            id="group-description"
            label="Description (optional)"
            error={fieldError('description')}
          >
            <textarea
              id="group-description"
              value={form.description}
              maxLength={4000}
              disabled={action.pending}
              onChange={event => setForm({ ...form, description: event.target.value })}
            />
          </Field>
          <fieldset className="fieldset" disabled={action.pending}>
            <legend>Study goals (optional)</legend>
            <div className="check-options">
              {STUDY_GOALS.map(goal => (
                <label key={goal}>
                  <input
                    type="checkbox"
                    checked={form.studyGoals.includes(goal)}
                    onChange={event => setForm({
                      ...form,
                      studyGoals: event.target.checked ? [...form.studyGoals, goal] : form.studyGoals.filter(value => value !== goal)
                    })}
                  />
                  {label(goal)}
                </label>
              ))}
            </div>
          </fieldset>
          <WeeklySlotEditor
            value={form.availability}
            onChange={availability => setForm({ ...form, availability })}
            error={fieldError('availability')}
            disabled={action.pending}
          />
          <ActionNotice error={action.error} />
          <div className="actions">
            <Link className="retro-button" to={id ? `/groups/${id}` : '/groups'}>Cancel</Link>
            <Button
              type="submit"
              variant="primary"
              disabled={action.pending || !courses.data.length}
            >{action.pending ? 'Saving…' : id ? 'Save group changes' : 'Create study group'}</Button>
          </div>
          {!courses.data.length && (
            <p className="field-error">No courses are available. A course must be configured before creating a group.</p>
          )}
        </form>
      )}
    </WindowPage>
  )
}
