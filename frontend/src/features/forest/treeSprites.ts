import type { TreeLight, TreeSpecies } from './api'

const WIDTH = 16
export const TREE_HEIGHT = 24
const BLANK = '.'.repeat(WIDTH)

/** Pads a sprite at the top so every tree stands on the same ground line. */
function standing(rows: string[]) {
  return [...Array(TREE_HEIGHT - rows.length).fill(BLANK), ...rows]
}

const SPROUT = standing([
  '.....ll..ll.....', '....lmml.lmml...', '.....mm.mm......', '.......tt.......', '.......tt.......', '......tttt......',
])
const SAPLING = standing([
  '.......ll.......', '......lllm......', '.....llmmmd.....', '....llmmmmdd....', '....lmmmmmdd....', '.....mmmmdd.....',
  '......dttd......', '.......tt.......', '.......tt.......', '.......tt.......', '.......tt.......', '......tttt......',
])
const OAK = standing([
  '.....llllll.....', '...llllmmmmm....', '..lllmmmmmmmd...', '.llmmmmmmmmmdd..', '.lmmmmmmmmmmddd.', 'llmmmmmmmmmmmddd',
  'lmmmmmmmmmmmdddd', '.mmmmmmmmmmdddd.', '.dmmmmmmmmddddd.', '..ddmmmmdddddd..', '....dddTtddd....', '.......Tt.......',
  '.......Tt.......', '.......Tt.......', '.......Tt.......', '......TTtt......', '.....TT..tt.....',
])
const PINE = standing([
  '.......nn.......', '......nnnn......', '.....nnmmnn.....', '......nmmn......', '.....nnmmmn.....', '....nnmmmmdn....',
  '.....nmmmmd.....', '....nnmmmmdd....', '...nnmmmmmmdd...', '....nmmmmmdd....', '...nnmmmmmmdd...', '..nnmmmmmmmmdd..',
  '...nmmmmmmmdd...', '..nnmmmmmmmmdd..', '.nnmmmmmmmmmmdd.', 'nnmmmmmmmmmmmddd', '..ddddddddddd...', '.......Tt.......',
  '.......Tt.......', '.......Tt.......', '.......Tt.......', '......TTtt......',
])
const ANCIENT_BLOSSOM = standing([
  '..s.........s...', '.....PPpPP......', '...PPpppPPpP....', '..PpppPPpppPP...', '.PPpplppPPppPP..', 'PpppmpppPpppmpP.',
  'PpplmmpPppmmppPP', '.ppmmmppppmmmpp.', '.Ppmmdpppmmdppp.', '..ppdddpppdddp..', 's..pppTTttppp..s', '.....TTTttt.....',
  '......TTtt......', '......TTtt......', '.....TTTttt.....', '......TTtt......', '......TTtt......', '.....TTTtttt....',
  '....TT..TT.tt...',
])
/** Shown in the room before a visit has grown anything. */
export const SEED = standing(['.......ss.......', '......sstt......', '.......tt.......'])

export const TREE_ROWS: Record<TreeSpecies, string[]> = { SPROUT, SAPLING, OAK, PINE, ANCIENT_BLOSSOM }

const TRUNK = { t: '#7a4b2a', T: '#5a3520', s: '#c9a46a' }
/** The light a visit started in recolours its leaves, so trees of one species still differ. */
export const LEAF_PALETTES: Record<TreeLight, Record<string, string>> = {
  DAWN: { ...TRUNK, l: '#d8e889', m: '#8cbf4f', d: '#557f3a', n: '#6fa04a', p: '#ffc4d2', P: '#ffe1e8' },
  DAY: { ...TRUNK, l: '#9fd36f', m: '#4f9a4a', d: '#2f6b3a', n: '#3f8a4a', p: '#f7a6bd', P: '#ffd2df' },
  DUSK: { ...TRUNK, l: '#f6c46a', m: '#d9813c', d: '#8f4a2a', n: '#b8642f', p: '#f58fa6', P: '#ffc0a8' },
  NIGHT: { ...TRUNK, l: '#8fd1c8', m: '#3f8794', d: '#24485e', n: '#2f6a78', p: '#c9a6f2', P: '#e6d6ff' },
}
/** Sparkles are drawn on their own layer so only they twinkle. */
export const SPARKLE = { s: '#fff3a0' }
export const SOIL_ROWS = ['hghghghghghghghg', 'gggggggggggggggg', 'ssrssssrsssssrss']
export const SOIL = { g: '#5aa83e', h: '#7cc457', s: '#8a5a32', r: '#6b4426' }
