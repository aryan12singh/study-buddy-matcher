import { useId, type ReactNode } from 'react'

type NavSectionProps = {
  title: string
  children: ReactNode
}

export default function NavSection({ title, children }: NavSectionProps) {
  const titleId = useId()
  return (
    <div className="nav-section" role="group" aria-labelledby={titleId}>
      <span id={titleId} className="nav-section-title">{title}</span>
      {children}
    </div>
  )
}
