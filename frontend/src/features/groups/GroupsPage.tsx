import { Link, useSearchParams } from 'react-router-dom'
import { getGroups, getMyApplications, getMyGroups } from './api'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import { STUDY_GOALS, STUDY_MODES, label, formatTimestamp } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import TabBar from '../../shared/components/TabBar'
import Field from '../../shared/components/Field'
import StatePanel from '../../shared/components/StatePanel'
import Badge from '../../shared/components/Badge'
import GroupCard from './GroupCard'
import FilterSummary from '../../shared/components/FilterSummary'
import Button from '../../shared/components/Button'

export default function GroupsPage() {
  const [search, setSearch] = useSearchParams()
  const requestedView = search.get('view')
  const tab = requestedView === 'mine' || requestedView === 'applications' ? requestedView : 'browse'
  const requestedCourse = search.get('courseId') || ''
  const courseId = /^\d+$/.test(requestedCourse) && Number.isSafeInteger(Number(requestedCourse)) && Number(requestedCourse) > 0 ? requestedCourse : ''
  const studyGoal = STUDY_GOALS.find(goal => goal === search.get('studyGoal')) || ''
  const studyMode = STUDY_MODES.find(mode => mode === search.get('studyMode')) || ''
  function updateSearch(key: string, value: string, replace = true) {
    setSearch(previous => {
      const next = new URLSearchParams(previous)
      if (value) next.set(key, value)
      else next.delete(key)
      return next
    }, { replace })
  }
  function clearFilters() {
    setSearch(previous => {
      const next = new URLSearchParams(previous)
      for (const key of ['courseId', 'studyGoal', 'studyMode']) next.delete(key)
      return next
    }, { replace: true })
  }
  const courses = useResource('group-filter-courses', getCourses, tab === 'browse')
  const groups = useResource(
    `groups-${tab}-${courseId}-${studyGoal}-${studyMode}`,
    signal => tab === 'mine' ? getMyGroups(signal) : getGroups({ courseId, studyGoal, studyMode }, signal),
    tab !== 'applications'
  )
  const applications = useResource('my-group-applications', getMyApplications, tab === 'applications')
  const filters = [
    ...(courseId ? [{ key: 'courseId', label: `Course: ${courses.data?.find(course => String(course.id) === courseId)?.code || courseId}`, onRemove: () => updateSearch('courseId', '') }] : []),
    ...(studyGoal ? [{ key: 'studyGoal', label: `Goal: ${label(studyGoal)}`, onRemove: () => updateSearch('studyGoal', '') }] : []),
    ...(studyMode ? [{ key: 'studyMode', label: `Mode: ${label(studyMode)}`, onRemove: () => updateSearch('studyMode', '') }] : [])
  ]
  return (
    <WindowPage
      title="Study groups"
      description="Find a course group or bring students together."
      actions={<Link className="retro-button primary" to="/groups/new">Create study group</Link>}
    >
      <TabBar
        value={tab}
        label="Study group views"
        options={[
          { value: 'browse', label: 'Browse groups' },
          { value: 'mine', label: 'Your groups' },
          { value: 'applications', label: 'Your applications' }
        ]}
        onChange={value => updateSearch('view', value === 'browse' ? '' : value, false)}
      />
      {tab === 'browse' && (
        <div className="filters">
          <Field id="filter-course" label="Course">
            <select
              id="filter-course"
              value={courseId}
              onChange={event => updateSearch('courseId', event.target.value)}
              disabled={!courses.data}
            >
              <option value="">All courses</option>
              {courses.data?.map(course => (
                <option key={course.id} value={course.id}>{course.code} — {course.name}</option>
              ))}
            </select>
          </Field>
          <Field id="filter-goal" label="Study goal">
            <select
              id="filter-goal"
              value={studyGoal}
              onChange={event => updateSearch('studyGoal', event.target.value)}
            >
              <option value="">All goals</option>
              {STUDY_GOALS.map(goal => (
                <option key={goal} value={goal}>{label(goal)}</option>
              ))}
            </select>
          </Field>
          <Field id="filter-mode" label="Study mode">
            <select
              id="filter-mode"
              value={studyMode}
              onChange={event => updateSearch('studyMode', event.target.value)}
            >
              <option value="">All modes</option>
              {STUDY_MODES.map(mode => (
                <option key={mode} value={mode}>{label(mode)}</option>
              ))}
            </select>
          </Field>
          {courses.error && <StatePanel error={courses.error} onRetry={() => courses.reload()} />}
          {courses.loading && !courses.data && (
            <p role="status">Loading courses…</p>
          )}
        </div>
      )}
      <FilterSummary filters={tab === 'browse' ? filters : []} onClear={clearFilters}
        result={tab === 'applications' ? applications.data && `${applications.data.length} application${applications.data.length === 1 ? '' : 's'}` : groups.data && `${groups.data.length} group${groups.data.length === 1 ? '' : 's'} in this view`} />
      {tab === 'applications' ? (
        <>
          <StatePanel
            loading={applications.loading && !applications.data}
            error={applications.error}
            onRetry={() => applications.reload()}
            empty={applications.data?.length === 0}
            emptyTitle="No group applications yet"
            emptyMessage="Browse groups and ask to join one that fits your goals."
            emptyKind="groups"
            emptyAction={<Button onClick={() => updateSearch('view', '', false)}>Browse study groups</Button>}
          />
          <div className="data-list">
            {applications.data?.map(application => (
              <article className="data-row" key={application.id}>
                <div>
                  <h2>
                    <Link to={`/groups/${application.groupId}`}>{application.groupName}</Link>
                  </h2>
                  <div className="tag-list">
                    <Badge
                      tone={application.status === 'PENDING' ? 'pending' : application.status === 'ACCEPTED' ? 'good' : 'neutral'}
                    >{label(application.status)}</Badge>
                    {!application.groupActive && (
                      <Badge>Closed group</Badge>
                    )}
                  </div>
                  {application.message && (
                    <p className="message-text">{application.message}</p>
                  )}
                  <p className="row-meta">Applied {formatTimestamp(application.createdAt)} (SGT){application.respondedAt && ` · Answered ${formatTimestamp(application.respondedAt)}`}</p>
                </div>
                <Link className="retro-button" to={`/groups/${application.groupId}`}>View group</Link>
              </article>
            ))}
          </div>
        </>
      ) : (
        <>
          <StatePanel
            loading={groups.loading && !groups.data}
            error={groups.error}
            onRetry={() => groups.reload()}
            empty={groups.data?.length === 0}
            emptyTitle={tab === 'mine' ? 'No groups joined yet' : 'No groups match these filters'}
            emptyMessage={tab === 'mine' ?
              'Groups you belong to or lead will appear here.'
              :
              'Try changing the filters, or create a group for your course.'}
            emptyKind="groups"
            emptyAction={tab === 'mine' ? <Button onClick={() => updateSearch('view', '', false)}>Browse study groups</Button> : filters.length ? <Button onClick={clearFilters}>Show all groups</Button> : <Link className="retro-button primary" to="/groups/new">Start a study group</Link>}
          />
          <div className="data-list">{groups.data?.map(group => <GroupCard key={group.id} group={group} />)}</div>
        </>
      )}
    </WindowPage>
  )
}
