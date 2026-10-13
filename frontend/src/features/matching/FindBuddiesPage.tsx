import WindowPage from '../../shared/components/WindowPage'
import FindBuddiesPanel from './FindBuddiesPanel'

/** The matching search as its own page in the student navigation. */
export default function FindBuddiesPage() {
  return (
    <WindowPage
      title="Find buddies"
      description="Search for compatible students by course or study goal and send them a match request."
    >
      <FindBuddiesPanel />
    </WindowPage>
  )
}
