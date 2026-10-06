export default function ActionNotice({ error, success }: { error?: string; success?: string }) {
  return (
    <>
      {error && (
        <p className="action-notice error-panel" role="alert">{error}</p>
      )}
      {success && (
        <p className="action-notice success-panel" role="status">{success}</p>
      )}
    </>
  )
}
