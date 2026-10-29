import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import { Link } from 'react-router-dom'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import AchievementSummary from './AchievementSummary'
import BadgeCard from './BadgeCard'
import StudyGrove from './StudyGrove'
import { getMyAchievements } from './api'
import './achievements.css'

export default function AchievementsPage() {
  const { account } = useAuth()
  const achievements = useResource(`achievements-${account?.id}`, getMyAchievements, account?.role === 'STUDENT')
  const data = achievements.data
  return (
    <WindowPage title="Achievements" description="Your private badge standing and planned-session grove.">
      {!data ? (
        <StatePanel loading={achievements.loading} error={achievements.error} onRetry={() => achievements.reload(true)} />
      ) : (
        <div className="achievements">
          <section aria-labelledby="standing-title">
            <h2 id="standing-title" className="achievements-heading">Current standing</h2>
            <p className="achievement-policy">Badge tiers you reach are kept, so your level never goes down. Progress uses your current records: connections, open group memberships and eligible schedules can fall, while sent requests and created groups count while their records are retained.</p>
            <AchievementSummary data={data} />
          </section>
          <StudyGrove streak={data.streak} />
          <p className="forest-callout">Every study room visit also plants a tree. <Link to="/forest">Visit my forest</Link></p>
          <details className="achievement-help">
            <summary>How these numbers work</summary>
            <p>Each badge contributes the highest tier you have reached: Bronze 1 point, Silver 2, Gold 3. Points determine your level, up to {data.maxPoints} points and level {data.maxLevel}. A tier is saved when you open your achievements, so losing a record later lowers only the count, not the tier or level.</p>
            <p>Planner counts individual occurrences, including each week of a recurring session. Only non-cancelled sessions in an active buddy connection or an open group you currently belong to count, from your current connection or membership start date onward. Anyone may have scheduled these sessions.</p>
            <p>Current planned streak is the consecutive run ending this week, or last week while this week is still open. Best eligible run is the longest run in those currently eligible schedules. Editing, cancelling or losing access to a schedule can change either number.</p>
            <p>Planning a session or opening a study-room timer does not record attendance, completed focus time or study quality. These badges make no claim about those activities.</p>
          </details>
          <section aria-labelledby="badges-title">
            <h2 id="badges-title" className="achievements-heading">Badges</h2>
            {data.badges.length === 0 ? (
              <StatePanel empty emptyTitle="No badges available" emptyMessage="Start with a buddy connection or a study group."
                emptyAction={<Link className="retro-button primary" to="/find-buddies">Find a study buddy</Link>} />
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
