import { useId } from 'react'
import type { AccountForm } from '../auth/validation'
import { CONTACT_NUMBER_HINT } from '../auth/contactNumber'
import { PROGRAMME_HINT, SMU_SCHOOLS, YEAR_OF_STUDY_MAX } from '../auth/studentDetails'
import Field from './Field'
import PasswordInput from './PasswordInput'

export default function AccountFields<T extends AccountForm>({ value, onChange, errors, student, creating, disabled, passwordToggle = false }: {
  value: T
  onChange: (next: T) => void
  errors: Record<string, string>
  student: boolean
  creating: boolean
  disabled?: boolean
  /** Adds a Show password button; registration leaves it off. */
  passwordToggle?: boolean
}) {
  const prefix = useId()
  const fields: {
    key: keyof AccountForm
    label: string
    type?: string
    autocomplete?: string
  }[] = [
      { key: 'email', label: 'Email address', type: 'email', autocomplete: 'username' },
      ...(creating ? [{ key: 'password' as const, label: 'Password', type: 'password', autocomplete: 'new-password' }] : []),
      ...(student ?
        [
          { key: 'name' as const, label: 'Full name', autocomplete: 'name' },
          { key: 'school' as const, label: 'School' },
          { key: 'programme' as const, label: 'Programme' },
          { key: 'yearOfStudy' as const, label: 'Year of study', type: 'number' },
          {
            key: 'contactNumber' as const,
            label: creating ? 'Contact number' : 'Replace contact number (optional)',
            type: 'tel',
            autocomplete: 'tel'
          }
        ]
        :
        []),
    ]
  const hints: Partial<Record<keyof AccountForm, string>> = {
    programme: PROGRAMME_HINT,
    contactNumber: `${CONTACT_NUMBER_HINT}${creating ? '' : ' Leave blank to preserve the saved private number. The current number is not provided to administrators.'}`
  }
  return (
    <fieldset className="account-fields" disabled={disabled}>
      {fields.map(field => {
        const id = `${prefix}-${field.key}`
        const shared = {
          id,
          value: value[field.key] || '',
          'aria-invalid': Boolean(errors[field.key]),
          'aria-describedby': errors[field.key] ? `${id}-error` : undefined,
          required: field.key !== 'contactNumber' || creating
        }
        const update = (next: string) => onChange({ ...value, [field.key]: next })
        return (
          <Field key={field.key} id={id} label={field.label} error={errors[field.key]} hint={hints[field.key]}>
            {field.key === 'password' && passwordToggle ?
              <PasswordInput {...shared} maxLength={72} autoComplete={field.autocomplete} onChange={event => update(event.target.value)} />
              : field.key === 'school' ?
                <select {...shared} onChange={event => update(event.target.value)}>
                  <option value="">Choose your school</option>
                  {SMU_SCHOOLS.map(school => <option key={school.code} value={school.code}>{school.code} — {school.name}</option>)}
                </select>
                :
                <input
                  {...shared}
                  type={field.type || 'text'}
                  min={field.type === 'number' ? 1 : undefined}
                  max={field.type === 'number' ? YEAR_OF_STUDY_MAX : undefined}
                  step={field.type === 'number' ? 1 : undefined}
                  maxLength={field.key === 'password' ? 72 : 255}
                  autoComplete={field.autocomplete}
                  onChange={event => update(event.target.value)}
                />}
          </Field>
        )
      })}
    </fieldset>
  )
}
