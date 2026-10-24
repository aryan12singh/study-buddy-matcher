import { describe, expect, it } from 'vitest'
import { planRemix } from './remixPlan'

const ids = ['rain', 'cafe', 'forest', 'ocean']

describe('planRemix', () => {
  it('plays two or three distinct categories within the volume range', () => {
    for (let run = 0; run < 50; run++) {
      const picks = planRemix(ids, () => false)
      expect(picks.length).toBeGreaterThanOrEqual(2)
      expect(picks.length).toBeLessThanOrEqual(3)
      expect(new Set(picks.map(pick => pick.id)).size).toBe(picks.length)
      picks.forEach(pick => {
        expect(ids).toContain(pick.id)
        expect(pick.volume).toBeGreaterThanOrEqual(0.2)
        expect(pick.volume).toBeLessThanOrEqual(0.8)
      })
    }
  })

  it('finds a sound for a category that was silent', () => {
    expect(planRemix(ids, () => false).every(pick => pick.newSound)).toBe(true)
  })

  it('keeps a playing sound unless chance swaps it', () => {
    // A random value of 0.5 is above the swap chance, so playing sounds stay.
    expect(planRemix(ids, () => true, () => 0.5).every(pick => !pick.newSound)).toBe(true)
    expect(planRemix(ids, () => true, () => 0).every(pick => pick.newSound)).toBe(true)
  })

  it('plays nothing when there are no categories', () => {
    expect(planRemix([], () => false)).toEqual([])
  })
})
