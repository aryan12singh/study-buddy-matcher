import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import PersonalPomodoro from './PersonalPomodoro'
import StudyRoomGroupsPanel from './StudyRoomGroupsPanel'
import './studyCorner.css'

export default function StudyRoomsPage() {
  const { account } = useAuth()

  return (
    <WindowPage title="Study Room" description="A quiet corner for a focused mind.">
      <div className="study-room-workspace">
        <div className="study-corner-scenery" aria-hidden="true"><i className="study-corner-cloud cloud-left" /><i className="study-corner-cloud cloud-right" /><i className="study-corner-hills" /><i className="study-corner-meadow" /></div>
        <div className="study-corner-layout">
          {account && <PersonalPomodoro key={account.id} accountId={account.id} />}
          <StudyRoomGroupsPanel />
        </div>
      </div>
    </WindowPage>
  )
}
