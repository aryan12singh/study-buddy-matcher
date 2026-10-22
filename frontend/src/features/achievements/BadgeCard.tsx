import type { Badge } from './api'
import { tierLabel } from './api'
import Medallion from './Medallion'

export default function BadgeCard({ badge }: { badge: Badge }) {
  const tier = tierLabel(badge.tier)
  const progress = badge.target > 0 ? badge.current / badge.target : 0
  const label = `${badge.title}, ${tier}, ${badge.current} of ${badge.target}`
  return (
    <li className={`badge-card${badge.earned ? ` badge-earned badge-${badge.tier?.toLowerCase()}` : ' badge-locked'}`} aria-label={label}>
      <Medallion badge={badge.key} tier={badge.tier} progress={progress} />
      <div className="badge-copy">
        <span className="badge-tier">{tier}</span>
        <h3>{badge.title}</h3>
        <p>{badge.description}</p>
        <div className="badge-meter" aria-hidden="true"><span style={{ width: `${Math.min(100, progress * 100)}%` }} /></div>
        <p className="badge-count"><strong>{badge.current}</strong> / {badge.target}</p>
        <p className="badge-hint">{badge.earnedHint}</p>
      </div>
    </li>
  )
}
