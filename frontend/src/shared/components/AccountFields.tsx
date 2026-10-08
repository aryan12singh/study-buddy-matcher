import { useId } from 'react'
import type { AccountForm } from '../auth/validation'
import Field from './Field'

export default function AccountFields<T extends AccountForm>({ value, onChange, errors, student, creating, disabled }: {
  value: T
  onChange: (next: T) => void
  errors: Record<string, string>
  student: boolean
  creating: boolean
  disabled?: boolean
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
  return (
    <fieldset className="account-fields" disabled={disabled}>
      {fields.map(field => (
        <Field
          key={field.key}
          id={`${prefix}-${field.key}`}
          label={field.label}
          error={errors[field.key]}
          hint={field.key === 'contactNumber' && !creating ?
            'Leave blank to preserve the saved private number. The current number is not provided to administrators.'
            :
            undefined}
        >
          <input
            id={`${prefix}-${field.key}`}
            type={field.type || 'text'}
            value={value[field.key] || ''}
            min={field.type === 'number' ? 1 : undefined}
            step={field.type === 'number' ? 1 : undefined}
            maxLength={field.key === 'password' ? 72 : 255}
            autoComplete={field.autocomplete}
            aria-invalid={Boolean(errors[field.key])}
            aria-describedby={errors[field.key] ? `${prefix}-${field.key}-error` : undefined}
            onChange={event => onChange({ ...value, [field.key]: event.target.value })}
            required={field.key !== 'contactNumber' || creating}
          />
        </Field>
      ))}
    </fieldset>
  )
}
