import { useEffect, useId, useRef, useState } from 'react'
import { useSpotify } from './spotifyContext'
import { useAmbient } from './ambientContext'
import { useMusicSettings } from './musicSettingsContext'
import { openRoomSettings } from '../studyroom/roomSettingsEvents'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import MusicSlider from './MusicSlider'
import './music.css'

/**
 * The timer's corner player: the current Spotify track with next track and volume, and one volume
 * for all ambient sounds. Only the Premium player can report a track; the free embedded playlist
 * keeps its controls to itself. With nothing playing it invites the student to pick some music.
 * In a group room, `personal` adds a note that only this student hears it, beside the shared timer.
 */
export default function NowPlayingMini({ personal = false }: { personal?: boolean }) {
  const { mode, player, volume, setVolume } = useSpotify()
  const ambient = useAmbient()
  const settings = useMusicSettings()
  const [volumesOpen, setVolumesOpen] = useState(false)
  const card = useRef<HTMLDivElement>(null)
  const volumesId = useId()

  // Like any dropdown, the volumes close on Escape or a click elsewhere on the page.
  useEffect(() => {
    if (!volumesOpen) return
    const closeOutside = (event: PointerEvent) => { if (!card.current?.contains(event.target as Node)) setVolumesOpen(false) }
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setVolumesOpen(false) }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [volumesOpen])
  const track = mode === 'premium' && player.status === 'ready' ? player.nowPlaying : undefined
  if (!track && !ambient.anyPlaying) {
    // The free playlist cannot report whether it is playing, so once it is loaded the prompt steps aside.
    if (mode === 'embed' && settings.opened) return null
    return (
      <div className="now-playing-mini is-prompt">
        <div className="now-playing-mini-row">
          <span className="now-playing-mini-cover is-empty" aria-hidden="true"><PixelIcon kind="music" /></span>
          <div className="now-playing-mini-track">
            <span className="now-playing-mini-kicker">Nothing playing</span>
            <strong>Study with some music?</strong>
          </div>
        </div>
        {personal && <p className="now-playing-mini-note">Only you will hear it, not the group.</p>}
        <Button variant="primary" className="now-playing-mini-open" onClick={() => openRoomSettings('music')}>Open music settings</Button>
      </div>
    )
  }
  const playing = !!track && !track.paused
  return (
    <div ref={card} className={`now-playing-mini${playing ? ' is-playing' : ''}${volumesOpen ? ' is-open' : ''}`} role="group" aria-label="Music now playing">
      <div className="now-playing-mini-row">
        {track?.imageUrl
          ? <img className="now-playing-mini-cover" src={track.imageUrl} alt="" width={32} height={32} />
          : <span className="now-playing-mini-cover is-empty" aria-hidden="true"><PixelIcon kind="music" /></span>}
        <div className="now-playing-mini-track">
          <span className="now-playing-mini-kicker">{track ? (playing ? 'Now playing' : 'Paused') : 'Ambient sounds'}</span>
          <strong title={track?.name}>{track ? track.name : 'Playing'}</strong>
        </div>
        {/* Plain icon buttons, styled like the panel's window controls opposite. */}
        {track && <button type="button" className="room-window-control now-playing-mini-control" onClick={player.nextTrack}
          aria-label="Next track" title="Next track"><span aria-hidden="true">⏭</span></button>}
        <button type="button" className="room-window-control now-playing-mini-control" aria-expanded={volumesOpen} aria-controls={volumesId}
          onClick={() => setVolumesOpen(open => !open)} aria-label={volumesOpen ? 'Hide volume' : 'Show volume'} title="Volume">
          <span aria-hidden="true">{volumesOpen ? '▴' : '▾'}</span>
        </button>
      </div>
      {personal && <p className="now-playing-mini-note">Only you hear this, not the group.</p>}
      {volumesOpen && <div id={volumesId} className="now-playing-mini-volumes">
        {track && <label className="now-playing-mini-volume">
          <span aria-hidden="true">Music</span>
          <MusicSlider aria-label="Spotify volume" min={0} max={1} step={0.05} value={volume} onValue={setVolume} />
        </label>}
        {ambient.anyPlaying && <label className="now-playing-mini-volume">
          <span aria-hidden="true">Ambient</span>
          <MusicSlider aria-label="Ambient sounds volume" min={0} max={1} step={0.05} value={ambient.master} onValue={ambient.setMaster} />
        </label>}
      </div>}
    </div>
  )
}
