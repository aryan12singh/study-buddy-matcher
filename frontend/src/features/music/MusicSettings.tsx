import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import MusicPlayer from './MusicPlayer'
import './music.css'

/** The collapsible music section shown in every study room. */
export default function MusicSettings() {
  // Spotify sign-in returns with ?spotify=, so open the section to show the outcome.
  const [search] = useSearchParams()
  const [returnedFromSpotify] = useState(() => search.has('spotify'))
  const [opened, setOpened] = useState(returnedFromSpotify)
  // The player loads on first open and stays mounted, so collapsing the section does not stop the music.
  return (
    <details className="music-settings" open={returnedFromSpotify || undefined} onToggle={event => { if (event.currentTarget.open) setOpened(true) }}>
      <summary>Music settings</summary>
      {opened && <MusicPlayer />}
    </details>
  )
}
