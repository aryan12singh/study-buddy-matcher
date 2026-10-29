import { api } from '../../shared/api/client'

export type TreeSpecies = 'SPROUT' | 'SAPLING' | 'OAK' | 'PINE' | 'ANCIENT_BLOSSOM'
export type TreeLight = 'DAWN' | 'DAY' | 'DUSK' | 'NIGHT'

export interface Tree {
  id: number
  species: TreeSpecies
  light: TreeLight
  focusedMinutes: number
  startedAt: string
  plantedAt: string
  groupName: string
}

export interface Forest {
  trees: Tree[]
  treeCount: number
  focusedMinutes: number
  longestMinutes: number
  stages: { species: TreeSpecies; minutes: number }[]
}

/** The tree growing in one room now; species is null until the visit is long enough to plant one. */
export interface GrowingTree {
  visiting: boolean
  focusedMinutes: number
  species: TreeSpecies | null
  next: TreeSpecies | null
  minutesToNext: number | null
  light: TreeLight | null
}

export async function getMyForest(signal?: AbortSignal) {
  return (await api.get<Forest>('/forest/me', { signal })).data
}

export async function getGrowingTree(groupId: number, signal?: AbortSignal) {
  return (await api.get<GrowingTree>(`/forest/me/rooms/${groupId}`, { signal })).data
}

export const SPECIES_NAMES: Record<TreeSpecies, string> = {
  SPROUT: 'Sprout', SAPLING: 'Sapling', OAK: 'Oak', PINE: 'Tall pine', ANCIENT_BLOSSOM: 'Ancient blossom',
}
export const LIGHT_NAMES: Record<TreeLight, string> = { DAWN: 'dawn', DAY: 'daytime', DUSK: 'dusk', NIGHT: 'night' }

export function formatMinutes(minutes: number) {
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60), rest = minutes % 60
  return rest ? `${hours} h ${rest} min` : `${hours} h`
}
