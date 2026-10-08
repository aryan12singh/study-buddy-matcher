import type { ReactNode } from 'react'

export default function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'good' | 'pending' | 'danger' }) {
  return (
    <span className={`status-badge ${tone}`}>{children}</span>
  )
}
