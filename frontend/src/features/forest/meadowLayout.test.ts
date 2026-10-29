import { describe, expect, it } from 'vitest'
import type { Tree } from './api'
import { layOut } from './meadowLayout'

const tree = (id: number, plot: number): Tree => ({ id, plot, variant: 0, species: 'OAK', light: 'DAY', focusedMinutes: 60,
  startedAt: '2026-10-28T02:00:00Z', plantedAt: '2026-10-28T03:00:00Z', groupName: 'Crew' })

describe('meadow layout', () => {
  it('gives colliding plots their own cells and keeps trees inside the meadow', () => {
    const { side, placed } = layOut([tree(1, 7), tree(2, 7), tree(3, 32)], 420)
    expect(side).toBe(5)
    const cells = placed.map(spot => `${Math.floor(spot.x * side)},${Math.floor(spot.y * side)}`)
    expect(new Set(cells).size).toBe(3)
    placed.forEach(spot => { expect(spot.x).toBeGreaterThan(0); expect(spot.x).toBeLessThan(1); expect(spot.y).toBeLessThan(1) })
  })

  it('never moves an old tree when a new one is planted, and grows the grid with the forest', () => {
    const before = layOut([tree(1, 12)], 420).placed[0]
    const after = layOut([tree(2, 12), tree(1, 12)], 420).placed.find(spot => spot.tree.id === 1)
    expect(after).toEqual(before)
    expect(layOut(Array.from({ length: 40 }, (_, i) => tree(i + 1, i * 13)), 420).side).toBe(9)
  })
})
