import Button from './Button'
import type { ReactNode } from 'react'
import PixelIcon from './PixelIcon'
import type { PixelIconKind } from './PixelIcon'

export default function StatePanel({ loading, error, empty, emptyTitle = 'Nothing here yet', emptyMessage, emptyAction, emptyKind = 'brand', onRetry }: {
  loading?: boolean
  error?: string
  empty?: boolean
  emptyTitle?: string
  emptyMessage?: string
  emptyAction?: ReactNode
  emptyKind?: PixelIconKind
  onRetry?: () => void
}) {
  if (loading) return (
    <div className="state-panel loading-panel" role="status">
      <div className="loading-caption"><PixelIcon />Loading…</div>
      <div className="pixel-loading-track" aria-hidden="true"><span /></div>
      <div className="loading-skeleton" aria-hidden="true"><i /><i /><i /></div>
    </div>
  )
  if (error) return (
    <div className="state-panel error-panel" role="alert">
      <h2>Unable to load this view</h2>
      <p>{error}</p>
      {onRetry && (
        <Button onClick={onRetry}>Try again</Button>
      )}
    </div>
  )
  if (empty) return (
    <div className="state-panel">
      <div className="empty-art" aria-hidden="true"><PixelIcon kind={emptyKind} /></div>
      <h2>{emptyTitle}</h2>
      {emptyMessage && (
        <p>{emptyMessage}</p>
      )}
      {emptyAction && <div className="actions">{emptyAction}</div>}
    </div>
  )
  return null
}
