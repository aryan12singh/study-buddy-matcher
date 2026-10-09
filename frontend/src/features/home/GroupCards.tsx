import { Link } from 'react-router-dom'
import { label } from '../../shared/api/types'
import CapacityMeter from '../../shared/components/CapacityMeter'
import PixelIcon from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import type { GroupSummary } from '../groups/api'
import type { Loaded } from './homeTypes'
import SectionTitle from './SectionTitle'

const SHOWN = 2

function details(group: GroupSummary) {
  return [
    group.viewer.leader ? null : `Led by ${group.leaderName}`,
    group.studyGoals[0] ? label(group.studyGoals[0]) : null,
    group.preferredStudyMode ? label(group.preferredStudyMode) : null
  ].filter(Boolean).join(' · ')
}

export default function GroupCards({ groups }: { groups: Loaded<GroupSummary[]> }) {
  const active = groups.data?.filter(group => group.active) ?? []
  return (
    <section aria-labelledby="home-groups">
      <SectionTitle
        id="home-groups"
        icon="groups"
        title="My study groups"
        link={active.length ? { to: '/groups?view=mine', label: 'All my groups ›' } : undefined}
      />
      <StatePanel loading={groups.loading && !groups.data} error={groups.error} onRetry={() => groups.reload()} />
      {groups.data && (
        <div className="home-cards">
          {active.length === 0 && (
            <div className="home-ghost home-ghost-wide">
              <span className="home-ghost-icon"><PixelIcon kind="groups" /></span>
              <div><strong>You are not in a group yet</strong><p>Groups you lead or join appear here with their course and member count.</p></div>
            </div>
          )}
          {active.slice(0, SHOWN).map(group => (
            <article key={group.id} className={`home-group${group.memberCount >= group.maxGroupSize ? ' full' : ''}`}>
              <div className="home-group-top">
                <span className="home-chip">{group.courseCode}</span>
                <span className={`home-role${group.viewer.leader ? '' : ' member'}`}>{group.viewer.leader ? 'You lead' : 'Member'}</span>
              </div>
              <h3><Link to={`/groups/${group.id}`}>{group.name}</Link></h3>
              <p>{details(group) || group.courseName}</p>
              <CapacityMeter members={group.memberCount} capacity={group.maxGroupSize} active={group.active} />
            </article>
          ))}
          <Link className="home-ghost home-ghost-link" to="/groups">
            <div><strong>+ Find or start a group</strong><p>Browse open groups for your courses.</p></div>
          </Link>
        </div>
      )}
    </section>
  )
}
