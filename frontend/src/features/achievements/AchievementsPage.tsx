import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import AchievementSummary from './AchievementSummary'
import BadgeCard from './BadgeCard'
import StudyGrove from './StudyGrove'
import { getMyAchievements } from './api'
import './achievements.css'

export default function AchievementsPage() {
  const achievements = useResource('achievements-me', getMyAchievements)
  const data = achievements.data
  return (
    <WindowPage title="Achievements" description="Badges and a study grove earned from your connections, groups and planned sessions.">
      {!data ? (
        <StatePanel loading={achievements.loading} error={achievements.error} onRetry={() => achievements.reload(true)} />
      ) : (
        <div className="achievements">
          <AchievementSummary data={data} />
          <StudyGrove streak={data.streak} />
          <section aria-labelledby="badges-title">
            <h2 id="badges-title" className="achievements-heading">Badges</h2>
            {data.badges.length === 0 ? (
              <StatePanel empty emptyTitle="No badges yet" emptyMessage="Badges appear here as you connect, join groups and plan sessions." />
            ) : (
              <ul className="badge-grid">
                {data.badges.map(badge => <BadgeCard key={badge.key} badge={badge} />)}
              </ul>
            )}
          </section>
        </div>
      )}
    </WindowPage>
  )
}
