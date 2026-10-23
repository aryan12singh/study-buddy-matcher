import { useState } from 'react'
import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import PersonalPomodoro from './PersonalPomodoro'
import StudyRoomGroupsPanel from './StudyRoomGroupsPanel'
import HiddenPanelTools from './HiddenPanelTools'
import { usePanelVisibility } from './usePanelVisibility'
import { useRoomFullscreen } from './useRoomFullscreen'
import './studyCorner.css'

const GROUPS_COLLAPSED_KEY = 'study-room-groups-collapsed'
function readGroupsCollapsed() {
  try { return localStorage.getItem(GROUPS_COLLAPSED_KEY) === 'true' } catch { return false }
}

export default function StudyRoomsPage() {
  const { account } = useAuth()
  const panel = usePanelVisibility()
  const fullscreen = useRoomFullscreen()
  const [groupsCollapsed, setGroupsCollapsed] = useState(readGroupsCollapsed)
  function toggleGroups() {
    const next = !groupsCollapsed
    setGroupsCollapsed(next)
    try { localStorage.setItem(GROUPS_COLLAPSED_KEY, String(next)) } catch { /* Storage is optional. */ }
  }

  return (
    <WindowPage title="Study Room" description="A quiet corner for a focused mind.">
      <div className={`study-room-workspace${fullscreen.expanded ? ' is-expanded' : ''}${panel.hidden ? ' is-minimal' : ''}`}>
        <HiddenPanelTools panel={panel} fullscreen={fullscreen} />
        <div className="study-corner-scenery" aria-hidden="true"><i className="study-corner-cloud cloud-left" /><i className="study-corner-cloud cloud-right" /><i className="study-corner-hills" /><i className="study-corner-meadow" /></div>
        <div className={`study-corner-layout${groupsCollapsed ? ' groups-collapsed' : ''}`}>
          {account && <PersonalPomodoro key={account.id} accountId={account.id} panel={panel} fullscreen={fullscreen} />}
          <StudyRoomGroupsPanel collapsed={groupsCollapsed} onToggle={toggleGroups} />
        </div>
      </div>
    </WindowPage>
  )
}
