import Field from '../../shared/components/Field'
import { CONTACT_NUMBER_HINT } from '../../shared/auth/contactNumber'
import { PROGRAMME_HINT, SMU_SCHOOLS, YEAR_OF_STUDY_MAX } from '../../shared/auth/studentDetails'
import type { ProfileForm } from './profileForm'

const FIELDS: { key: 'name' | 'school' | 'programme' | 'yearOfStudy' | 'contactNumber'; label: string; type?: string; hint?: string; full?: boolean }[] = [
  { key: 'name', label: 'Full name', full: true },
  { key: 'school', label: 'School' },
  { key: 'programme', label: 'Programme', hint: PROGRAMME_HINT },
  { key: 'yearOfStudy', label: 'Year of study', type: 'number' },
  { key: 'contactNumber', label: 'Contact number', type: 'tel', hint: `${CONTACT_NUMBER_HINT} Shared only with students you are connected to.` }
]

export default function AboutYouFields({ value, onChange, errors, disabled }: {
  value: ProfileForm
  onChange: (next: ProfileForm) => void
  errors: Record<string, string>
  disabled: boolean
}) {
  return (
    <div className="form-grid">
      {FIELDS.map(field => (
        <div key={field.key} className={field.full ? 'form-full' : undefined}>
          <Field id={`profile-${field.key}`} label={field.label} error={errors[field.key]} hint={field.hint}>
            {field.key === 'school' ?
              <select
                id="profile-school"
                value={value.school}
                disabled={disabled}
                onChange={event => onChange({ ...value, school: event.target.value })}
              >
                <option value="">Choose your school</option>
                {SMU_SCHOOLS.map(school => <option key={school.code} value={school.code}>{school.code} — {school.name}</option>)}
              </select>
              :
              <input
                id={`profile-${field.key}`}
                type={field.type || 'text'}
                min={field.type === 'number' ? 1 : undefined}
                max={field.type === 'number' ? YEAR_OF_STUDY_MAX : undefined}
                maxLength={field.type === 'number' ? undefined : 255}
                value={value[field.key]}
                disabled={disabled}
                onChange={event => onChange({ ...value, [field.key]: event.target.value })}
              />}
          </Field>
        </div>
      ))}
    </div>
  )
}
