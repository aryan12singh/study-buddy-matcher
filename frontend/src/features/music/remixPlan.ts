/** A remix plays between this many sounds at once, each somewhere in the volume range. */
const REMIX_MIN_LAYERS = 2
const REMIX_MAX_LAYERS = 3
const REMIX_MIN_VOLUME = 0.2
const REMIX_MAX_VOLUME = 0.8
/** How often a remix swaps a sound that stays in the mix for another from the same category. */
const REMIX_NEW_SOUND_CHANCE = 0.3

/** One category a remix keeps playing; every category not picked goes silent. */
export type RemixPick = { id: string; volume: number; newSound: boolean }

function between(min: number, max: number, random: () => number) {
  return min + random() * (max - min)
}

/**
 * Picks which categories a remix plays and how loud. A category already playing keeps its sound
 * unless chance swaps it; one that was silent always needs a sound found for it.
 */
export function planRemix(ids: string[], isPlaying: (id: string) => boolean, random: () => number = Math.random): RemixPick[] {
  const order = [...ids]
  for (let i = order.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [order[i], order[j]] = [order[j], order[i]]
  }
  const count = Math.round(between(REMIX_MIN_LAYERS, REMIX_MAX_LAYERS, random))
  return order.slice(0, count).map(id => ({
    id,
    volume: between(REMIX_MIN_VOLUME, REMIX_MAX_VOLUME, random),
    newSound: !isPlaying(id) || random() < REMIX_NEW_SOUND_CHANCE,
  }))
}
