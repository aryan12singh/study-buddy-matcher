import { Link } from 'react-router-dom'
import { getMyGroups } from '../groups/api'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'

export default function StudyRoomGroupsPanel({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  const groups = useResource('study-room-groups', getMyGroups)
  const orderedGroups = groups.data && [...groups.data.filter(group => group.active), ...groups.data.filter(group => !group.active)]

  return (
    <aside className={`study-corner-groups study-corner-panel${collapsed ? ' is-collapsed' : ''}`}
      aria-labelledby={collapsed ? undefined : 'study-corner-groups-heading'} aria-label={collapsed ? 'Your study groups' : undefined}>
      {/* One button for both states keeps keyboard focus in place when the list folds away or returns. */}
      <button type="button" className="study-corner-groups-toggle" aria-expanded={!collapsed} aria-controls="study-corner-groups-content"
        aria-label={collapsed ? 'Show study groups' : 'Hide study groups'} title={collapsed ? 'Show study groups' : 'Hide study groups'} onClick={onToggle}>
        <span aria-hidden="true">{collapsed ? '‹' : '›'}</span>
        {collapsed && <span className="study-corner-groups-toggle-label" aria-hidden="true">Study groups</span>}
      </button>
      {!collapsed && <div id="study-corner-groups-content" className="study-corner-groups-content">
      <div className="study-corner-groups-heading">
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
      </div>}
    </aside>
  )
}
