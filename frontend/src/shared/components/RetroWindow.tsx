import type { ReactNode } from 'react'

/** The decorative window chrome every page and landing panel shares: a title bar with three dots, then the content. */
export default function RetroWindow({ title, className, children }: {
  title: string
  className?: string
  children: ReactNode
}) {
  return (
    <section className={className ? `retro-window ${className}` : 'retro-window'} aria-label={title}>
      <div className="window-titlebar">
        <span className="window-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
        <span>{title}</span>
      </div>
      <div className="window-content">{children}</div>
    </section>
  )
}
