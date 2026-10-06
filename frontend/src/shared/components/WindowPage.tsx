import type { ReactNode } from 'react'

export default function WindowPage({ title, description, actions, children }: {
  title: string
  description?: string
  actions?: ReactNode
  children: ReactNode
}) {
  return (
    <section className="retro-window" aria-label={title}>
      <div className="window-titlebar">
        <span className="window-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
        <span>{title}</span>
      </div>
      <div className="window-content">
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
      </div>
    </section>
  )
}
