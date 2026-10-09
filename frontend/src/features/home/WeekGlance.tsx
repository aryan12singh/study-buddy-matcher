import { Link } from 'react-router-dom'
import type { WeeklySlot } from '../../shared/api/types'
import PixelIcon from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import WeeklySchedule from '../../shared/components/WeeklySchedule'
import type { MyProfile } from '../profile/api'
import type { Loaded } from './homeTypes'

function minutes(time: string) {
  const [hours, mins] = time.split(':').map(Number)
  return hours * 60 + mins
}

function weeklyHours(slots: WeeklySlot[]) {
  const total = slots.reduce((sum, slot) => sum + minutes(slot.endTime) - minutes(slot.startTime), 0) / 60
  return Number.isInteger(total) ? String(total) : total.toFixed(1)
}

export default function WeekGlance({ profile, studentId }: { profile: Loaded<MyProfile>; studentId: number }) {
  const slots = profile.data?.availability ?? []
  const editLink = `/students/${studentId}/edit?tab=availability`
  return (
    <section className="home-box" aria-labelledby="home-week">
      <div className="home-box-head">
        <h2 id="home-week">Your week</h2>
        {slots.length > 0 && <Link className="home-more" to={editLink}>Edit times ›</Link>}
      </div>
      <StatePanel loading={profile.loading && !profile.data} error={profile.error} onRetry={() => profile.reload()} />
      {profile.data && (slots.length === 0 ? (
        <div className="home-ghost">
          <span className="home-ghost-icon"><PixelIcon kind="clock" /></span>
          <div className="home-ghost-text"><strong>No weekly times yet</strong><p>Add when you can usually study so matching can find overlap.</p></div>
          <Link className="retro-button primary" to={editLink}>Add weekly times ›</Link>
        </div>
      ) : (
        <>
          <WeeklySchedule slots={slots} />
          <div className="home-week-foot">
            <span><strong>{weeklyHours(slots)} hours</strong> a week</span>
            <span>Matching looks for overlap with these times.</span>
          </div>
        </>
      ))}
    </section>
  )
}
