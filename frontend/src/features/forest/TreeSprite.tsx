import { PixelRects } from '../achievements/pixelArt'
import type { TreeLight, TreeSpecies } from './api'
import { LEAF_PALETTES, SEED, SOIL, SOIL_ROWS, SPARKLE, TREE_HEIGHT, treeRows } from './treeSprites'

/** One pixel tree on its patch of soil. A null species draws the seed a visit starts from. */
export default function TreeSprite({ species, variant = 0, light, className, soil = true }: {
  species: TreeSpecies | null
  variant?: number
  light: TreeLight | null
  className?: string
  /** The meadow draws its own ground, so trees standing in it leave their soil patch out. */
  soil?: boolean
}) {
  const rows = species ? treeRows(species, variant) : SEED
  const palette = LEAF_PALETTES[light ?? 'DAY']
  const sparkles = rows.map(row => row.replace(/[^s]/g, '.'))
  return (
    <svg className={className} viewBox={`0 0 16 ${TREE_HEIGHT + (soil ? SOIL_ROWS.length : 0)}`} aria-hidden="true"
      focusable="false" shapeRendering="crispEdges">
      <g className="tree-body"><PixelRects rows={species ? rows.map(row => row.replace(/s/g, '.')) : rows} palette={palette} /></g>
      {species === 'ANCIENT_BLOSSOM' && <g className="tree-sparkle"><PixelRects rows={sparkles} palette={SPARKLE} /></g>}
      {soil && <PixelRects rows={SOIL_ROWS} palette={SOIL} y={TREE_HEIGHT} />}
    </svg>
  )
}
