import type { ReactNode } from 'react'
import RetroWindow from './RetroWindow'

export default function WindowPage({ title, description, actions, children }: {
  title: string
  description?: string
  actions?: ReactNode
  children: ReactNode
}) {
  return (
    <RetroWindow title={title}>
      <div className="page-heading">
        <div>
          <h1>{title}</h1>
          {description && (
            <p>{description}</p>
          )}
        </div>
        {actions && (
          <div className="actions">{actions}</div>
        )}
      </div>
      {children}
    </RetroWindow>
  )
}
