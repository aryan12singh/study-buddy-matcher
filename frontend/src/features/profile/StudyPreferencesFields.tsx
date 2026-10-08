import Field from '../../shared/components/Field'
import { label, STUDY_GOALS, STUDY_MODES } from '../../shared/api/types'
import type { Course } from '../../shared/api/types'
import { GROUP_SIZE_PREFERENCES } from './api'
import type { GroupSizePreference } from './api'
import type { ProfileForm } from './profileForm'

function groupSizeLabel(choice: GroupSizePreference, largestGroup: number) {
  if (choice === 'ONE_TO_ONE') return { title: 'One-to-one', hint: 'You and one buddy, 2 people' }
  if (choice === 'SMALL_GROUP') return { title: 'Small group', hint: `3 to ${largestGroup} people` }
  return { title: 'Either', hint: `2 to ${largestGroup} people` }
}

function FieldError({ id, error }: { id: string; error?: string }) {
  return error ? <p className="field-error" id={id} role="alert">{error}</p> : null
}

export default function StudyPreferencesFields({ value, onChange, errors, courses, largestGroup, disabled }: {
  value: ProfileForm
  onChange: (next: ProfileForm) => void
  errors: Record<string, string>
  courses: Course[]
  largestGroup: number
  disabled: boolean
}) {
  const sortedCourses = [...courses].sort((left, right) => left.code.localeCompare(right.code))
  return (
    <>
      <fieldset className="fieldset" disabled={disabled} aria-describedby={errors.courseIds ? 'profile-courses-error' : undefined}>
        <legend>Courses currently taken</legend>
        <p className="field-hint">Choose at least one. Matching compares these with other students.</p>
        <div className="check-options option-columns">
          {sortedCourses.map(course => (
            <label key={course.id}>
              <input
                type="checkbox"
                checked={value.courseIds.includes(course.id)}
                onChange={event => onChange({
                  ...value,
                  courseIds: event.target.checked
                    ? [...value.courseIds, course.id].sort((left, right) => left - right)
                    : value.courseIds.filter(id => id !== course.id)
                })}
              />
              <strong className="course-code">{course.code}</strong> <span>{course.name}</span>
            </label>
          ))}
        </div>
        <FieldError id="profile-courses-error" error={errors.courseIds} />
      </fieldset>

      <div className="form-grid">
        <Field
          id="profile-target-course"
          label="Course I need a study buddy for (optional)"
          hint="Used as the default course when you search for matches."
          error={errors.targetCourseId}
        >
          <select
            id="profile-target-course"
            value={value.targetCourseId}
            disabled={disabled}
            onChange={event => onChange({ ...value, targetCourseId: event.target.value })}
          >
            <option value="">Not decided yet</option>
            {sortedCourses.map(course => (
              <option key={course.id} value={course.id}>{course.code} — {course.name}</option>
            ))}
          </select>
        </Field>
      </div>

      <fieldset className="fieldset" disabled={disabled} aria-describedby={errors.preferredStudyMode ? 'profile-mode-error' : undefined}>
        <legend>Preferred study mode</legend>
        <div className="check-options">
          {STUDY_MODES.map(mode => (
            <label key={mode}>
              <input
                type="radio"
                name="profile-study-mode"
                checked={value.preferredStudyMode === mode}
                onChange={() => onChange({ ...value, preferredStudyMode: mode })}
              />
              {label(mode)}
            </label>
          ))}
        </div>
        <FieldError id="profile-mode-error" error={errors.preferredStudyMode} />
      </fieldset>

      <fieldset className="fieldset" disabled={disabled} aria-describedby={errors.groupSizePreference ? 'profile-size-error' : undefined}>
        <legend>Preferred group size</legend>
        <div className="check-options option-rows">
          {GROUP_SIZE_PREFERENCES.map(choice => {
            const text = groupSizeLabel(choice, largestGroup)
            return (
              <label key={choice}>
                <input
                  type="radio"
                  name="profile-group-size"
                  checked={value.groupSizePreference === choice}
                  onChange={() => onChange({ ...value, groupSizePreference: choice })}
                />
                <strong className="option-title">{text.title}</strong> <span className="muted">{text.hint}</span>
              </label>
            )
          })}
        </div>
        <FieldError id="profile-size-error" error={errors.groupSizePreference} />
      </fieldset>

      <fieldset className="fieldset" disabled={disabled}>
        <legend>Study goals (optional)</legend>
        <div className="check-options">
          {STUDY_GOALS.map(goal => (
            <label key={goal}>
              <input
                type="checkbox"
                checked={value.studyGoals.includes(goal)}
                onChange={event => onChange({
                  ...value,
                  studyGoals: event.target.checked ? [...value.studyGoals, goal].sort() : value.studyGoals.filter(item => item !== goal)
                })}
              />
              {label(goal)}
            </label>
          ))}
        </div>
      </fieldset>
    </>
  )
}
