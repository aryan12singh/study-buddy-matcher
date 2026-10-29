import type { Badge } from './api'

/** Only existing application routes are allowed; the API cannot supply a navigation URL. */
export function badgeAction(badge: Badge) {
  switch (badge.key) {
    case 'CONNECTOR': return badge.earned
      ? { to: '/connections?view=connected', label: 'View connections' }
      : { to: '/find-buddies', label: 'Find a study buddy' }
    case 'FIRST_MOVE': return { to: '/find-buddies', label: 'Find buddies to request' }
    case 'TEAM_PLAYER': return { to: '/groups', label: 'Browse study groups' }
    case 'FOUNDER': return { to: '/groups/new', label: 'Create a study group' }
    case 'PLANNER': return { to: '/sessions', label: 'Open study sessions' }
    case 'GROVE_KEEPER': return { to: '/sessions', label: 'Plan your next week' }
    case 'FORESTER': return badge.earned
      ? { to: '/forest', label: 'Visit my forest' }
      : { to: '/study-room', label: 'Grow a tree in a study room' }
  }
}
