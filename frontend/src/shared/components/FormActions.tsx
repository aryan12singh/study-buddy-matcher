import type { ReactNode } from 'react'

export default function FormActions({ dirty, pending, children }: { dirty: boolean; pending: boolean; children: ReactNode }) {
  return <div className="form-actions">
    <p className="form-save-state" role="status"><span aria-hidden="true" />
      {pending ? 'Saving changes…' : dirty ? 'Unsaved changes' : 'Ready to save'}
    </p>
    <div className="actions">{children}</div>
  </div>
}
