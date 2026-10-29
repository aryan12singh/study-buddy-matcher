import { PixelRects } from '../achievements/pixelArt'
import type { TreeLight, TreeSpecies } from './api'
import { LEAF_PALETTES, SEED, SOIL, SOIL_ROWS, SPARKLE, TREE_HEIGHT, TREE_ROWS } from './treeSprites'

/** One pixel tree on its patch of soil. A null species draws the seed a visit starts from. */
export default function TreeSprite({ species, light, className }: {
  species: TreeSpecies | null
  light: TreeLight | null
  className?: string
}) {
  const rows = species ? TREE_ROWS[species] : SEED
  const palette = LEAF_PALETTES[light ?? 'DAY']
  const sparkles = rows.map(row => row.replace(/[^s]/g, '.'))
  return (
    <svg className={className} viewBox={`0 0 16 ${TREE_HEIGHT + SOIL_ROWS.length}`} aria-hidden="true"
      focusable="false" shapeRendering="crispEdges">
      <g className="tree-body"><PixelRects rows={species ? rows.map(row => row.replace(/s/g, '.')) : rows} palette={palette} /></g>
      {species === 'ANCIENT_BLOSSOM' && <g className="tree-sparkle"><PixelRects rows={sparkles} palette={SPARKLE} /></g>}
      <PixelRects rows={SOIL_ROWS} palette={SOIL} y={TREE_HEIGHT} />
    </svg>
  )
}
