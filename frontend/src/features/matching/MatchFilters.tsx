import { Link } from 'react-router-dom'
import type { Course } from '../../shared/api/types'
import { label, STUDY_GOALS } from '../../shared/api/types'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import { GROUP_SIZE_FILTERS, QUALITY_FILTERS, SHARED_HOURS_FILTERS, STUDY_MODE_FILTERS } from './matchSearchForm'
import type { MatchSearchForm } from './matchSearchForm'

/** The filter panel. Every filter is optional; applying none lists everyone. */
export default function MatchFilters({ id, form, courses, coursesLoading, coursesError, profileEditLink, onChange, onApply, onClear }: {
  id: string
  form: MatchSearchForm
  courses: Course[] | undefined
  coursesLoading: boolean
  coursesError: boolean
  /** Shown when the student has no courses to filter by. */
  profileEditLink?: string
  onChange: (field: keyof MatchSearchForm, value: string) => void
  onApply: () => void
  onClear: () => void
}) {
  const noCourses = courses?.length === 0
  return (
    <form
      id={id}
      className="stack-form match-filters"
      onSubmit={event => { event.preventDefault(); onApply() }}
      noValidate
      aria-label="Filter study buddies"
    >
      <div className="form-grid">
        <Field
          id="match-course"
          label="Course"
          hint={coursesError ?
            'Your courses could not be loaded. You can still filter by the other options.'
            : noCourses ? 'Add the courses you take to your profile to filter by course.' : 'Your courses this term.'}
        >
          <select id="match-course" value={form.courseId} disabled={!courses || noCourses} onChange={event => onChange('courseId', event.target.value)}>
            <option value="">{coursesLoading && !courses ? 'Loading your courses…' : 'Any course'}</option>
            {courses?.map(course => <option key={course.id} value={course.id}>{course.code} {course.name}</option>)}
          </select>
        </Field>
        <Field id="match-goal" label="Study goal">
          <select id="match-goal" value={form.studyGoal} onChange={event => onChange('studyGoal', event.target.value)}>
            <option value="">Any study goal</option>
            {STUDY_GOALS.map(goal => <option key={goal} value={goal}>{label(goal)}</option>)}
          </select>
        </Field>
        <Field id="match-mode" label="Study mode">
          <select id="match-mode" value={form.studyMode} onChange={event => onChange('studyMode', event.target.value)}>
            {STUDY_MODE_FILTERS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </Field>
        <Field id="match-group-size" label="Group size">
          <select id="match-group-size" value={form.groupSize} onChange={event => onChange('groupSize', event.target.value)}>
            {GROUP_SIZE_FILTERS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </Field>
        <Field id="match-hours" label="Shared free time" hint="Compared with the availability in your profile.">
          <select id="match-hours" value={form.minSharedHours} onChange={event => onChange('minSharedHours', event.target.value)}>
            {SHARED_HOURS_FILTERS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </Field>
        <Field id="match-quality" label="Minimum match quality">
          <select id="match-quality" value={form.minQuality} onChange={event => onChange('minQuality', event.target.value)}>
            {QUALITY_FILTERS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </Field>
      </div>
      {noCourses && profileEditLink && <p><Link to={profileEditLink}>Add courses to your profile</Link></p>}
      <div className="actions">
        <Button type="button" onClick={onClear}>Clear filters</Button>
        <Button type="submit" variant="primary">Apply filters</Button>
      </div>
    </form>
  )
}
