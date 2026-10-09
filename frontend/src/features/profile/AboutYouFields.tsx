import Field from '../../shared/components/Field'
import type { ProfileForm } from './profileForm'

const FIELDS: { key: 'name' | 'school' | 'programme' | 'yearOfStudy' | 'contactNumber'; label: string; type?: string; hint?: string; full?: boolean }[] = [
  { key: 'name', label: 'Full name', full: true },
  { key: 'school', label: 'School' },
  { key: 'programme', label: 'Programme' },
  { key: 'yearOfStudy', label: 'Year of study', type: 'number' },
  { key: 'contactNumber', label: 'Contact number', hint: 'Shared only with students you are connected to.' }
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
            <input
              id={`profile-${field.key}`}
              type={field.type || 'text'}
              min={field.type === 'number' ? 1 : undefined}
              maxLength={field.type ? undefined : 255}
              value={value[field.key]}
              disabled={disabled}
              onChange={event => onChange({ ...value, [field.key]: event.target.value })}
            />
          </Field>
        </div>
      ))}
    </div>
  )
}
