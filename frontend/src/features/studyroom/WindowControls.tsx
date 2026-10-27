import RoomSettingsMenu from './RoomSettingsMenu'
import type { RoomSettingsTab } from './RoomSettingsMenu'
import type { PanelVisibility } from './usePanelVisibility'
import type { RoomFullscreen } from './useRoomFullscreen'
import './windowControls.css'

/** Settings, hide panel and full screen, laid out like a desktop window's title-bar controls. */
export default function WindowControls({ tabs, panel: { hideButton, hide }, fullscreen }: {
  tabs: RoomSettingsTab[]; panel: PanelVisibility; fullscreen: RoomFullscreen
}) {
  const fullscreenLabel = fullscreen.expanded ? 'Exit full screen' : 'Full screen'
  return <div className="window-controls">
    <RoomSettingsMenu tabs={tabs} />
    <button ref={hideButton} type="button" className="room-window-control" aria-label="Hide panel" title="Hide panel"
      onClick={hide}><span className="window-control-icon minimise" aria-hidden="true" /></button>
    <button type="button" className="room-window-control" aria-label={fullscreenLabel} title={fullscreenLabel} onClick={fullscreen.toggle}>
      <span className={`window-control-icon ${fullscreen.expanded ? 'restore' : 'maximise'}`} aria-hidden="true" />
    </button>
  </div>
}
