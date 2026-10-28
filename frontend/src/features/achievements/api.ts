import { api } from '../../shared/api/client'

export type BadgeTier = 'BRONZE' | 'SILVER' | 'GOLD'
export type BadgeKey = 'CONNECTOR' | 'FIRST_MOVE' | 'TEAM_PLAYER' | 'FOUNDER' | 'PLANNER' | 'GROVE_KEEPER'

export interface Badge {
  key: BadgeKey
  title: string
  description: string
  /** Highest tier ever reached; never lowered. */
  tier: BadgeTier | null
  /** The tier today's count reaches on its own; can be lower than tier. */
  currentTier: BadgeTier | null
  nextTier: BadgeTier | null
  current: number
  target: number
  earned: boolean
  earnedHint: string
}

export interface Streak {
  currentWeeks: number
  bestWeeks: number
  /** ISO date of the Monday of each week in the current run, oldest first. */
  currentRunWeeks: string[]
  weekZone: string
}

export interface Achievements {
  level: number
  maxLevel: number
  points: number
  maxPoints: number
  pointsToNextLevel: number
  pointsPerLevel: number
  earnedCount: number
  totalCount: number
  streak: Streak
  badges: Badge[]
}

export async function getMyAchievements(signal?: AbortSignal) {
  return (await api.get<Achievements>('/achievements/me', { signal })).data
}

export function tierLabel(tier: BadgeTier | null) {
  return tier ? tier.charAt(0) + tier.slice(1).toLowerCase() : 'Locked'
}
