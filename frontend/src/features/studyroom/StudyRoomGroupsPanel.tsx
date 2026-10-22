import { Link } from 'react-router-dom'
import { getMyGroups } from '../groups/api'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import PixelIcon from '../../shared/components/PixelIcon'

export default function StudyRoomGroupsPanel() {
  const groups = useResource('study-room-groups', getMyGroups)
  const orderedGroups = groups.data && [...groups.data.filter(group => group.active), ...groups.data.filter(group => !group.active)]

  return (
    <aside className="study-corner-groups study-corner-panel" aria-labelledby="study-corner-groups-heading">
      <div className="study-corner-groups-heading">
        <span className="pomodoro-kicker"><PixelIcon kind="groups" />Better together</span>
        <div><h2 id="study-corner-groups-heading">Your study groups</h2>{groups.data && <span className="study-corner-group-count">{groups.data.length}</span>}</div>
        <p>A familiar face, a shared goal. Find your people here.</p>
      </div>
      <StatePanel loading={groups.loading && !groups.data} error={groups.error} onRetry={() => groups.reload()}
        empty={groups.data?.length === 0} emptyTitle="Your company is on its way"
        emptyMessage="Join or create a study group. Your groups will appear here when you are ready to study together."
        emptyKind="groups" />
      <div className="study-corner-group-list">
        {orderedGroups?.map(group => (
          <Link key={group.id} className={`study-corner-group${group.active ? '' : ' is-closed'}`}
            to={group.active ? `/groups/${group.id}/room` : `/groups/${group.id}`}
            aria-label={group.active ? `Open study room for ${group.name}` : `View closed group ${group.name}`}>
            <span className="study-corner-group-course">{group.courseCode}<span>{group.active ? (group.viewer.leader ? 'Your group' : 'Member') : 'Closed'}</span></span>
            <h3>{group.name}</h3>
            <p>{group.courseName}</p>
            <span className="study-corner-group-footer"><span>{group.memberCount} {group.memberCount === 1 ? 'member' : 'members'}</span><strong>{group.active ? 'Open room' : 'View group'} <span aria-hidden="true">↗</span></strong></span>
          </Link>
        ))}
      </div>
      <Link className="retro-button study-corner-browse" to="/groups">Browse study groups <span aria-hidden="true">→</span></Link>
    </aside>
  )
}
