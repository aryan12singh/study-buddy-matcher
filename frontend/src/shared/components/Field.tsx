import { cloneElement, isValidElement } from 'react'
import type { ReactNode } from 'react'

export default function Field({ label, id, error, hint, children }: {
  label: string
  id: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  const control = isValidElement<{ 'aria-describedby'?: string; 'aria-invalid'?: boolean }>(children) && typeof children.type === 'string'
    ?
    cloneElement(
      children,
      {
        'aria-describedby': [children.props['aria-describedby'], hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined,
        'aria-invalid': Boolean(error) || children.props['aria-invalid']
      }
    ) : children
  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      {control}
      {hint && (
        <p className="field-hint" id={`${id}-hint`}>{hint}</p>
      )}
      {error && (
        <p
          className="field-error"
          id={`${id}-error`}
          role="alert"
        >{error}</p>
      )}
    </div>
  )
}
