import Button from './Button'

export default function StatePanel({ loading, error, empty, emptyTitle = 'Nothing here yet', emptyMessage, onRetry }: {
  loading?: boolean
  error?: string
  empty?: boolean
  emptyTitle?: string
  emptyMessage?: string
  onRetry?: () => void
}) {
  if (loading) return (
    <div className="state-panel" role="status"><span className="loading-square" aria-hidden="true" />Loading…</div>
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
      <h2>{emptyTitle}</h2>
      {emptyMessage && (
        <p>{emptyMessage}</p>
      )}
    </div>
  )
  return null
}
