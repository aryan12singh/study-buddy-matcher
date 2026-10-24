import { useEffect, useState } from 'react'
import type { NowPlaying } from './useSpotifyPlayer'

function clock(ms: number) {
  const seconds = Math.floor(ms / 1000)
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

/** How far through the current track the player is. Dragging the line jumps to that point. */
export default function SpotifyProgress({ track, onSeek }: { track: NowPlaying; onSeek: (positionMs: number) => void }) {
  const [now, setNow] = useState(() => Date.now())
  // Spotify only reports the position when something changes, so count forward while it plays.
  useEffect(() => {
    if (track.paused) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [track.paused])
  const elapsed = track.paused ? 0 : Math.max(0, now - track.reportedAt)
  const position = Math.min(track.positionMs + elapsed, track.durationMs)
  return (
    <div className="spotify-progress">
      <span>{clock(position)}</span>
      <input type="range" aria-label="Track position" aria-valuetext={`${clock(position)} of ${clock(track.durationMs)}`}
        min={0} max={track.durationMs} step={1000} value={position}
        onChange={event => onSeek(Number(event.target.value))} />
      <span>{clock(track.durationMs)}</span>
    </div>
  )
}
