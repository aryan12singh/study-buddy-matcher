const EMBED_URL = 'https://open.spotify.com/embed/playlist/'

/** Spotify's compact embed height, which fits the settings menu. */
const COMPACT_HEIGHT = 152

/**
 * Spotify's own embedded player, for students without Premium. It needs no sign-in here: signed out of
 * Spotify in this browser it plays previews, signed in it plays full tracks.
 */
export default function SpotifyEmbed({ playlistId }: { playlistId: string }) {
  return (
    <iframe className="spotify-embed" title="Spotify study playlist" src={`${EMBED_URL}${encodeURIComponent(playlistId)}`}
      width="100%" height={COMPACT_HEIGHT} loading="lazy" allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture" />
  )
}
