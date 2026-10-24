import { useState } from 'react'
import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import PersonalPomodoro from './PersonalPomodoro'
import StudyRoomGroupsPanel from './StudyRoomGroupsPanel'
import HiddenPanelTools from './HiddenPanelTools'
import AtmosphereScene from './AtmosphereScene'
import RoomWeatherPanel from './RoomWeatherPanel'
import MusicPlayer from '../music/MusicPlayer'
import NowPlayingMini from '../music/NowPlayingMini'
import MusicProvider from '../music/MusicProvider'
import { DEFAULT_ATMOSPHERE } from './weatherTheme'
import type { Atmosphere } from './weatherTheme'
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
  const [atmosphere, setAtmosphere] = useState<Atmosphere>(DEFAULT_ATMOSPHERE)
  function toggleGroups() {
    const next = !groupsCollapsed
    setGroupsCollapsed(next)
    try { localStorage.setItem(GROUPS_COLLAPSED_KEY, String(next)) } catch { /* Storage is optional. */ }
  }

  return (
    <WindowPage title="Study Room" description="A quiet corner for a focused mind.">
      <MusicProvider>
        <div className={`study-room-workspace atmosphere-workspace${fullscreen.expanded ? ' is-expanded' : ''}${panel.hidden ? ' is-minimal' : ''}`}>
          <HiddenPanelTools panel={panel} fullscreen={fullscreen} />
          <AtmosphereScene className="atmosphere-backdrop" {...atmosphere} />
          <div className={`study-corner-layout${groupsCollapsed ? ' groups-collapsed' : ''}`}>
            {account && <PersonalPomodoro key={account.id} accountId={account.id} panel={panel} fullscreen={fullscreen} corner={<NowPlayingMini />}
              extraTabs={[{ id: 'atmosphere', label: 'Room atmosphere', icon: '☼', persistent: true,
                content: <RoomWeatherPanel liveEnabled store="personal-room" onChange={setAtmosphere} /> },
              { id: 'music', label: 'Music', icon: '♪', keepAfterOpen: true, content: <MusicPlayer /> }]} />}
            <StudyRoomGroupsPanel collapsed={groupsCollapsed} onToggle={toggleGroups} />
          </div>
        </div>
      </MusicProvider>
    </WindowPage>
  )
}
