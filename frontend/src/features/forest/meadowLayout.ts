import type { Tree, TreeSpecies } from './api'

/** Width in pixels of each tree size when a meadow cell is roomy; smaller cells shrink them. */
const TREE_WIDTHS: Record<TreeSpecies, number> = { SPROUT: 44, SAPLING: 58, OAK: 84, PINE: 78, ANCIENT_BLOSSOM: 104 }
const ROOMY_CELL = 76
const MIN_CELLS_PER_SIDE = 5

export interface PlacedTree { tree: Tree; x: number; y: number; width: number }
export type DecorationKind = 'tuft' | 'flower' | 'pebble'
export interface Decoration { kind: DecorationKind; x: number; y: number }
const DECORATIONS: DecorationKind[] = ['tuft', 'flower', 'tuft', 'pebble']

/**
 * Places each tree on a square grid at the cell its server-drawn plot points to, moving on to the
 * next free cell when two plots collide. Oldest trees are placed first, so a new tree never moves
 * an old one. The grid grows with the forest, keeping about half the cells empty.
 */
export function layOut(trees: Tree[], meadowPixels: number) {
  const side = Math.max(MIN_CELLS_PER_SIDE, Math.ceil(Math.sqrt(trees.length * 2)))
  const cells = side * side
  const cellPixels = meadowPixels / side
  const scale = Math.min(1, Math.max(.5, cellPixels / ROOMY_CELL))
  const taken = new Set<number>()
  const placed: PlacedTree[] = []
  for (const tree of [...trees].sort((a, b) => a.id - b.id)) {
    let cell = tree.plot % cells
    while (taken.has(cell)) cell = (cell + 1) % cells
    taken.add(cell)
    // A small offset from the same plot keeps the rows from looking planted with a ruler.
    const jitterX = ((Math.floor(tree.plot / cells) % 5) - 2) * .12
    const jitterY = ((Math.floor(tree.plot / (cells * 5)) % 5) - 2) * .12
    placed.push({
      tree,
      x: ((cell % side) + .5 + jitterX) / side,
      y: (Math.floor(cell / side) + .5 + jitterY) / side,
      width: Math.round(TREE_WIDTHS[tree.species] * scale),
    })
  }
  return { side, placed, decorations: decorate(trees, side, taken) }
}

/**
 * Sprinkles grass tufts, flowers and pebbles over some empty cells. The pattern comes from the
 * trees' server-drawn plots, so it is stable for a forest and different between forests.
 */
function decorate(trees: Tree[], side: number, taken: Set<number>) {
  let seed = trees.reduce((total, tree) => (total * 31 + tree.plot) % 2147483647, 17)
  const next = () => (seed = (seed * 48271) % 2147483647)
  const decorations: Decoration[] = []
  for (let cell = 0; cell < side * side; cell++) {
    if (taken.has(cell) || next() % 3 !== 0) continue
    decorations.push({
      kind: DECORATIONS[next() % DECORATIONS.length],
      x: ((cell % side) + .2 + (next() % 60) / 100) / side,
      y: (Math.floor(cell / side) + .2 + (next() % 60) / 100) / side,
    })
  }
  return decorations
}
