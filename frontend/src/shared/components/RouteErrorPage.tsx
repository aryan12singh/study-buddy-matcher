import StatePanel from './StatePanel'
import WindowPage from './WindowPage'

/**
 * Shown instead of React Router's raw error screen when a page throws while rendering or its
 * code fails to download, for example on flaky wifi or in a tab left open across a redeploy.
 */
export default function RouteErrorPage() {
  return (
    <WindowPage title="Something went wrong">
      <StatePanel error="This page could not be shown. Reload to try again." onRetry={() => window.location.reload()} />
    </WindowPage>
  )
}
