import type { Achievements, Badge, BadgeKey } from '../features/achievements/api'

const badge = (key: BadgeKey, title: string, overrides: Partial<Badge> = {}): Badge => ({
  key, title, description: `${title} metric explanation.`, tier: null, nextTier: 'BRONZE', current: 0, target: 1,
  earned: false, earnedHint: '1 more to reach Bronze.', ...overrides
})

export const achievementStanding: Achievements = {
  level: 2, maxLevel: 7, points: 5, maxPoints: 18, pointsToNextLevel: 1, pointsPerLevel: 3, earnedCount: 4, totalCount: 6,
  streak: { currentWeeks: 3, bestWeeks: 3, currentRunWeeks: ['2026-10-05', '2026-10-12', '2026-10-19'], weekZone: 'Asia/Singapore' },
  badges: [
    badge('CONNECTOR', 'Connector', { tier: 'SILVER', nextTier: 'GOLD', current: 3, target: 5, earned: true, earnedHint: '2 more active connections to reach Gold.' }),
    badge('FIRST_MOVE', 'First Move', { tier: 'BRONZE', nextTier: 'SILVER', current: 1, target: 5, earned: true }),
    badge('TEAM_PLAYER', 'Team Player'),
    badge('FOUNDER', 'Founder'),
    badge('PLANNER', 'Planner', { tier: 'BRONZE', nextTier: 'SILVER', current: 3, target: 5, earned: true }),
    badge('GROVE_KEEPER', 'Grove Keeper', { tier: 'BRONZE', nextTier: 'SILVER', current: 3, target: 4, earned: true })
  ]
}
