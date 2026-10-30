import type { RoomReactionType } from './api'

/** Original 12×12 pixel drawings for each reaction, in the same style as the badges and trees. */
export const REACTIONS: Record<RoomReactionType, { label: string; rows: string[]; palette: Record<string, string> }> = {
  CLAP: {
    label: 'Clap',
    rows: ['y....y....y.', '..y.....y...', '....hh.hh...', '...hHhhHh...', '..hhHhhHhh..', '..hhHhhHhh..',
      '..hhhhhhhh..', '..hhhhhhhh..', '...hhhhhh...', '...HhhhhH...', '....HHHH....', '............'],
    palette: { h: '#f4c99a', H: '#c98a5a', y: '#ffd84a' },
  },
  HEART: {
    label: 'Heart',
    rows: ['............', '..rr....rr..', '.rwrr..rrrr.', 'rwrrrrrrrrrR', 'rrrrrrrrrrrR', 'rrrrrrrrrrrR',
      '.rrrrrrrrrR.', '..rrrrrrrR..', '...rrrrrR...', '....rrrR....', '.....rR.....', '............'],
    palette: { r: '#e2425b', R: '#a8243c', w: '#ffd0d8' },
  },
  FIRE: {
    label: 'On a roll',
    rows: ['.....r......', '....rr......', '....ror..r..', '...roor.rr..', '..rooorrror.', '..roooooor..',
      '.rooyyyoorr.', '.rooyyyyoor.', '.royyyyyyor.', '.royyyyyyor.', '..royyyyor..', '...rrrrrr...'],
    palette: { r: '#e2422b', o: '#ff8a2a', y: '#ffd84a' },
  },
  LIGHTBULB: {
    label: 'Got it',
    rows: ['....YYYY....', '...YyyyyY...', '..YywwyyyY..', '..YywyyyyY..', '..YyyyyyyY..', '..YyyyyyyY..',
      '...YyyyyY...', '....YyyY....', '....gGgG....', '....GgGg....', '.....GG.....', '............'],
    palette: { y: '#ffe066', Y: '#c99a1a', w: '#fffbe0', g: '#b0b0b0', G: '#6b6b6b' },
  },
  COFFEE: {
    label: 'Coffee break',
    rows: ['...s..s.....', '....s..s....', '...s..s.....', '............', '.CCCCCCCC...', '.ckkkkkkC...',
      '.cccccccCCC.', '.cccccccC.C.', '.cccccccCCC.', '..ccccccC...', '.CCCCCCCCC..', '............'],
    palette: { c: '#f2efe6', C: '#8f887a', k: '#6b4426', s: '#9fb3bb' },
  },
  SPROUT: {
    label: 'Growing',
    rows: ['............', '............', '..ll....ll..', '.lllm..mlll.', '.llmmm.mmll.', '..lmmmmmml..',
      '....mmmm....', '.....mm.....', '.....mm.....', '..ssssssss..', '.ssssssssss.', '............'],
    palette: { l: '#9fd36f', m: '#4f9a4a', s: '#8a5a32' },
  },
}

export const REACTION_TYPES = Object.keys(REACTIONS) as RoomReactionType[]
