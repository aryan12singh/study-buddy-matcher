import type { Location } from 'react-router-dom'

/** Where a page's back button should go, passed as router state by the page that linked to it. */
export type ReturnTo = { path: string; label: string }

/** Router state for a link that should bring the user back to the given page. */
export function returnToState(path: string, label: string) {
  return { returnTo: { path, label } satisfies ReturnTo }
}

/** The return target from router state, or the fallback. Only paths inside the app are accepted. */
export function returnToFrom(location: Location, fallback: ReturnTo): ReturnTo {
  const candidate = (location.state as { returnTo?: Partial<ReturnTo> } | null)?.returnTo
  const valid = typeof candidate?.path === 'string' && candidate.path.startsWith('/') && !candidate.path.startsWith('//')
    && typeof candidate.label === 'string'
  return valid ? { path: candidate.path as string, label: candidate.label as string } : fallback
}
