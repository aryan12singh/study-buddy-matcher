import { useEffect, useRef } from 'react'
import { useBlocker, useLocation } from 'react-router-dom'
import Dialog from './Dialog'
import Button from './Button'

/** Drafts stay in memory. Block SPA exits and warn on reload only while there is work to lose. */
export function useFormExit(dirty: boolean, pending: boolean) {
  const { key } = useLocation()
  const savedEntry = useRef<string | null>(null)
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    savedEntry.current !== key && (dirty || pending) &&
    (currentLocation.pathname !== nextLocation.pathname || currentLocation.search !== nextLocation.search)
  )
  useEffect(() => {
    if (!dirty && !pending) return
    const warn = (event: BeforeUnloadEvent) => {
      if (savedEntry.current === key) return
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty, pending, key])
  return {
    allowSavedNavigation: () => { savedEntry.current = key },
    confirmation: blocker.state === 'blocked' ? (
      <Dialog title={pending ? 'Leave while saving?' : 'Discard unsaved changes?'} onClose={() => blocker.reset()}>
        <p>{pending ? 'Your changes are still being saved. Stay here for the result, or leave this page.' : 'Your edits have not been saved. Keep editing, or discard them and leave this page.'}</p>
        <div className="actions dialog-actions">
          <Button onClick={() => blocker.reset()}>Keep editing</Button>
          <Button variant="danger" onClick={() => blocker.proceed()}>{pending ? 'Leave page' : 'Discard changes'}</Button>
        </div>
      </Dialog>
    ) : null
  }
}
