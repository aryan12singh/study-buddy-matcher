import Button from '../../shared/components/Button'
import type { PanelVisibility } from './usePanelVisibility'
import type { RoomFullscreen } from './useRoomFullscreen'
import './windowControls.css'

/** The way back while a room's panel is hidden; it mounts with the hidden view so focus lands here. */
export default function HiddenPanelTools({ panel, fullscreen }: { panel: PanelVisibility; fullscreen: RoomFullscreen }) {
  if (!panel.hidden) return null
  return <div className="room-minimal-tools">
    {/* eslint-disable-next-line jsx-a11y/no-autofocus */}
    <Button autoFocus onClick={panel.show}><span aria-hidden="true">▣</span> Show panel</Button>
    {fullscreen.expanded && <Button onClick={fullscreen.toggle}><span aria-hidden="true">⤡</span> Exit full screen</Button>}
  </div>
}
