import { useId } from 'react'
import type { BadgeKey, BadgeTier } from './api'
import { PixelRects } from './pixelArt'

/** 8 x 8 original emblems, one per badge; 'x' is the engraved ink and 'o' a highlight. */
const EMBLEMS: Record<BadgeKey, readonly string[]> = {
  CONNECTOR: ['.xxx....', 'x...x...', 'x..xxxx.', 'x..x.x.x', '.xxx.x.x', '...x...x', '....xxx.', '........'],
  FIRST_MOVE: ['.......x', '.....xxx', '...xxoxx', '.xxxooxx', '....xxx.', '....xx..', '....x...', '........'],
  TEAM_PLAYER: ['...xx...', '.x.xx.x.', 'xx....xx', '...xx...', '.xxxxxx.', 'xxxxxxxx', 'xx.xx.xx', '........'],
  FOUNDER: ['xxxxxx..', 'xooooxx.', 'xooooxxx', 'xxxxxx..', 'x.......', 'x.......', 'x.......', 'xxx.....'],
  PLANNER: ['.x...x..', 'xxxxxxxx', 'x......x', 'x.xx.x.x', 'x......x', 'x.x.xx.x', 'x......x', 'xxxxxxxx'],
  GROVE_KEEPER: ['...xx...', '..xoox..', '.xooxxx.', 'xooxxxxx', '.xxxxxx.', '...xx...', '...xx...', '..xxxx..'],
  FORESTER: ['...x....', '..xox..x', '.xooxx.x', '..xox.xo', '.xooxxxo', 'xooxxxxx', '...x...x', '..xxx.xx']
}

const METALS: Record<BadgeTier | 'LOCKED', { light: string; mid: string; dark: string; ink: string; ribbon: string }> = {
  BRONZE: { light: '#f6c896', mid: '#b8743a', dark: '#6e3b16', ink: '#4a2408', ribbon: '#9f2828' },
  SILVER: { light: '#fbfcfd', mid: '#b4bec7', dark: '#5f6b75', ink: '#2c353d', ribbon: '#2d459d' },
  GOLD: { light: '#fff3b0', mid: '#e1b23c', dark: '#8a5f0c', ink: '#4f3500', ribbon: '#1f6b4a' },
  LOCKED: { light: '#efefed', mid: '#bdbdbb', dark: '#80807e', ink: '#5d5d5b', ribbon: '#9a9a98' }
}

const RING_RADIUS = 55
const RING_LENGTH = 2 * Math.PI * RING_RADIUS

/** Layered enamel-pin medallion: ribbon, metal rim, bevel, engraved face, pixel emblem and shine. */
export default function Medallion({ badge, tier, progress }: { badge: BadgeKey; tier: BadgeTier | null; progress: number }) {
  const id = useId().replace(/:/g, '')
  const metal = METALS[tier ?? 'LOCKED']
  const filled = Math.max(0, Math.min(1, progress))
  return (
    <svg className="medallion-art" viewBox="0 0 120 132" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id={`${id}-rim`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor={metal.light} />
          <stop offset=".55" stopColor={metal.mid} />
          <stop offset="1" stopColor={metal.dark} />
        </linearGradient>
        <linearGradient id={`${id}-bevel`} x1="1" y1="1" x2="0" y2="0">
          <stop offset="0" stopColor={metal.light} />
          <stop offset="1" stopColor={metal.dark} />
        </linearGradient>
        <radialGradient id={`${id}-face`} cx=".38" cy=".32" r=".8">
          <stop offset="0" stopColor={metal.light} />
          <stop offset=".7" stopColor={metal.mid} />
          <stop offset="1" stopColor={metal.dark} />
        </radialGradient>
        <clipPath id={`${id}-clip`}><circle cx="60" cy="60" r="33" /></clipPath>
      </defs>
      <g className="medallion-ribbon">
        <path d="M38 84 L28 128 L40 120 L48 130 L56 92 Z" fill={metal.ribbon} />
        <path d="M82 84 L92 128 L80 120 L72 130 L64 92 Z" fill={metal.ribbon} />
        <path d="M38 84 L28 128 L33 124 L44 88 Z" fill="#000" opacity=".18" />
      </g>
      <circle className="medallion-track" cx="60" cy="60" r={RING_RADIUS} fill="none" strokeWidth="5" />
      <circle className="medallion-progress" cx="60" cy="60" r={RING_RADIUS} fill="none" strokeWidth="5" stroke={metal.ribbon}
        strokeDasharray={`${filled * RING_LENGTH} ${RING_LENGTH}`} transform="rotate(-90 60 60)" strokeLinecap="butt" />
      <circle cx="60" cy="60" r="47" fill={`url(#${id}-rim)`} stroke={metal.ink} strokeWidth="2" />
      <circle cx="60" cy="60" r="40" fill={`url(#${id}-bevel)`} />
      <circle cx="60" cy="60" r="34" fill={`url(#${id}-face)`} stroke={metal.dark} strokeWidth="1.5" />
      <circle cx="60" cy="60" r="29" fill="none" stroke={metal.light} strokeWidth="1" strokeDasharray="2 3" opacity=".7" />
      <PixelRects rows={EMBLEMS[badge]} palette={{ x: metal.ink, o: metal.light }} x={40} y={40} size={5} />
      <g clipPath={`url(#${id}-clip)`}>
        <path className="medallion-shine" d="M18 50 L50 18 L62 18 L18 62 Z" fill="#fff" opacity=".32" />
      </g>
      <path d="M22 44 A40 40 0 0 1 44 22" fill="none" stroke="#fff" strokeWidth="2.5" opacity=".55" strokeLinecap="round" />
    </svg>
  )
}
