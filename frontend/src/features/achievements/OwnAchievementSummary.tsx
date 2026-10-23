import { Link } from 'react-router-dom'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import AchievementSummary from './AchievementSummary'
import { getMyAchievements } from './api'
import './achievements.css'

/** Mounted only on the signed-in student's own profile, and keyed by that owner. */
export default function OwnAchievementSummary({ ownerId }: { ownerId: number }) {
  const resource = useResource(`achievements-${ownerId}`, getMyAchievements)
  return (
    <section className="detail-panel own-achievements" aria-labelledby="own-achievements-title">
      <div className="own-achievements-heading">
        <h2 id="own-achievements-title">Your achievements</h2>
        <Link className="retro-button" to="/achievements">View all achievements</Link>
      </div>
      <p>Private to you. Current badge tiers reflect your records; the streak counts planned session weeks.</p>
      {resource.data ? <AchievementSummary data={resource.data} compact />
        : <StatePanel loading={resource.loading} error={resource.error} onRetry={() => resource.reload(true)} />}
    </section>
  )
}
