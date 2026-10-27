import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { errorMessage, fieldErrors } from '../../shared/api/client'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import { campusDate, label, STUDY_GOALS } from '../../shared/api/types'
import type { Course } from '../../shared/api/types'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import { useFormExit } from '../../shared/components/useFormExit'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import FormActions from '../../shared/components/FormActions'
import { isAiUnavailable } from '../quiz/api'
import { createStudyPlan, MAX_HOURS_PER_WEEK } from './api'
import { initialStudyPlanForm, latestExamDate, planRequest, validateStudyPlanForm } from './studyPlanForm'
import type { StudyPlanForm } from './studyPlanForm'
import { addDays } from './planDates'
import StudyMaterialInput from './StudyMaterialInput'
import './studyplan.css'

const AI_UNAVAILABLE = 'Gemini could not draft the roadmap just now, so no plan was saved. Try again in a minute,'
  + ' or plan the weeks yourself from your course outline.'

export default function CreateStudyPlanPage() {
  const courses = useResource('study-plan-courses', getCourses, true, false)
  return <WindowPage title="Create study plan"
    description="Gemini reads your material and spreads its topics over the weeks up to your exam, within the hours you have."
    actions={<Link className="retro-button" to="/study-plan">Back to study plans</Link>}>
    <StatePanel loading={courses.loading && !courses.data} error={courses.error} onRetry={() => courses.reload()} />
    {courses.data && <StudyPlanFormView courses={courses.data} />}
  </WindowPage>
}

function StudyPlanFormView({ courses }: { courses: Course[] }) {
  const [search] = useSearchParams()
  const navigate = useViewNavigation()
  const today = campusDate(new Date().toISOString())
  const [baseline] = useState(() => initialStudyPlanForm(search, courses))
  const [form, setForm] = useState<StudyPlanForm>(baseline)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState<string>()
  const dirty = JSON.stringify({ ...form, files: form.files.map(file => file.name) }) !== JSON.stringify({ ...baseline, files: [] })
  const exit = useFormExit(dirty, pending)

  function change(next: Partial<StudyPlanForm>) {
    setForm(previous => ({ ...previous, ...next }))
    // A field's message goes once the student changes that field.
    setErrors(previous => Object.fromEntries(Object.entries(previous).filter(([field]) => !(field in next))))
  }

  /** Switching between files and topics clears both material messages: only the chosen kind is checked. */
  function switchMaterial(materialKind: StudyPlanForm['materialKind']) {
    setForm(previous => ({ ...previous, materialKind }))
    setErrors(({ files: _files, topicsText: _topics, ...rest }) => rest)
  }

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateStudyPlanForm(form, today)
    setErrors(invalid)
    setFailure(undefined)
    if (Object.keys(invalid).length) return
    const { input, material } = planRequest(form)
    setPending(true)
    try {
      await createStudyPlan(input, material)
      exit.allowSavedNavigation()
      navigate('/study-plan', { state: { notice: 'Study plan created.' } })
    } catch (error) {
      setErrors(fieldErrors(error))
      setFailure(isAiUnavailable(error) ? AI_UNAVAILABLE : errorMessage(error))
    } finally {
      setPending(false)
    }
  }

  if (!courses.length) return <StatePanel empty emptyTitle="No courses available"
    emptyMessage="A course must be configured before a study plan can be created." />
  return <form className="stack-form" onSubmit={submit} noValidate>
    <div className="form-grid">
      <Field id="plan-course" label="Course" error={errors.courseId}>
        <select id="plan-course" value={form.courseId} disabled={pending} onChange={event => change({ courseId: event.target.value })}>
          <option value="">Choose a course</option>
          {courses.map(course => <option key={course.id} value={course.id}>{course.code} — {course.name}</option>)}
        </select>
      </Field>
      <Field id="plan-hours" label="Hours per week" error={errors.hoursPerWeek} hint={`How long you can study this course each week, 1 to ${MAX_HOURS_PER_WEEK}.`}>
        <input id="plan-hours" type="number" min={1} max={MAX_HOURS_PER_WEEK} step={1} value={form.hoursPerWeek} disabled={pending}
          onChange={event => change({ hoursPerWeek: event.target.value })} />
      </Field>
      <Field id="plan-exam" label="Exam date (optional)" error={errors.examDate} hint="Leave it empty to plan this week only.">
        <input id="plan-exam" type="date" min={addDays(today, 1)} max={latestExamDate(today)} value={form.examDate} disabled={pending}
          onChange={event => change({ examDate: event.target.value })} />
      </Field>
      <Field id="plan-goal" label="Study goal (optional)">
        <select id="plan-goal" value={form.studyGoal} disabled={pending} onChange={event => change({ studyGoal: event.target.value })}>
          <option value="">Not specified</option>
          {STUDY_GOALS.map(goal => <option key={goal} value={goal}>{label(goal)}</option>)}
        </select>
      </Field>
    </div>
    <StudyMaterialInput kind={form.materialKind} files={form.files} topicsText={form.topicsText} errors={errors} disabled={pending}
      onKindChange={switchMaterial}
      onFilesChange={files => change({ files })} onTopicsChange={topicsText => change({ topicsText })} />
    {pending && <p className="muted" role="status">Reading your material and drafting the roadmap. This can take up to a minute.</p>}
    <ActionNotice error={failure} />
    <FormActions dirty={dirty} pending={pending}>
      <Link className="retro-button" to="/study-plan">Cancel</Link>
      <Button type="submit" variant="primary" disabled={pending}>{pending ? 'Drafting roadmap…' : 'Create study plan'}</Button>
    </FormActions>
    {exit.confirmation}
  </form>
}
