import type { Badge } from './api'
import { tierLabel } from './api'
import Medallion from './Medallion'
import { Link } from 'react-router-dom'
import { badgeAction } from './badgeActions'

export default function BadgeCard({ badge }: { badge: Badge }) {
  const tier = tierLabel(badge.tier)
  const progress = badge.target > 0 ? Math.max(0, Math.min(1, badge.current / badge.target)) : 0
  const next = badge.nextTier ? tierLabel(badge.nextTier) : null
  const label = `${badge.title}, ${tier}, ${badge.current} of ${badge.target}${next ? ` toward ${next}` : ', gold threshold reached'}`
  const action = badgeAction(badge)
  return (
    <li className={`badge-card${badge.earned ? ` badge-earned badge-${badge.tier?.toLowerCase()}` : ' badge-locked'}`} aria-label={label}>
      <Medallion badge={badge.key} tier={badge.tier} progress={progress} />
      <div className="badge-copy">
        <span className="badge-tier">{tier}</span>
        <h3>{badge.title}</h3>
        <p>{badge.description}</p>
        <div className="badge-meter" role="progressbar" aria-label={`${badge.title} ${next ?? 'Gold'} progress`}
          aria-valuemin={0} aria-valuemax={badge.target} aria-valuenow={Math.min(badge.current, badge.target)}
          aria-valuetext={next ? `${badge.current} of ${badge.target} toward ${next}` : `Gold threshold reached; current count ${badge.current}`}>
          <span style={{ width: `${progress * 100}%` }} />
        </div>
        <p className="badge-count"><strong>{badge.current}</strong>{next ? ` / ${badge.target} for ${next}` : ` · Gold at ${badge.target}`}</p>
        <p className="badge-hint">{badge.earnedHint}</p>
        <Link className="retro-button badge-action" to={action.to}>{action.label}</Link>
      </div>
    </li>
  )
}
