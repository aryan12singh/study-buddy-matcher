import { Link } from 'react-router-dom'
import PixelIcon from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import Badge from '../../shared/components/Badge'
import { useResource } from '../../shared/api/useResource'
import type { MyProfile } from '../profile/api'
import { searchMatches } from '../matching/api'
import type { MatchSearch } from '../matching/api'
import { STRATEGIES } from '../matching/matchingSettingsForm'
import { QUALITY_LABELS, QUALITY_TONES } from '../matching/matchSearchForm'
import type { Loaded } from './homeTypes'
import { label } from '../../shared/api/types'

const STEPS = ['Pick a course', 'See ranked matches', 'Send a request']

/** A small live preview using the same endpoint and configuration as Find buddies. */
export default function FindBuddiesCard({ profile }: { profile: Loaded<MyProfile> }) {
  const target = profile.data?.targetCourse
  const goal = profile.data?.studyGoals[0]
  const search: MatchSearch = target ? { courseId: target.id } : goal ? { studyGoal: goal } : {}
  const enabled = Boolean(target || goal)
  const matches = useResource(`home-matches-${JSON.stringify(search)}`, signal => searchMatches(search, signal), enabled)
  const preview = matches.data?.matches.slice(0, 3) ?? []
  const query = target ? `?courseId=${target.id}` : goal ? `?studyGoal=${goal}` : ''
  return (
    <section className="home-finder" aria-labelledby="home-finder">
      <span className="home-finder-icon" aria-hidden="true"><PixelIcon kind="search" /></span>
      <div>
        <h2 id="home-finder">Find study buddies</h2>
        <p>Pick a course or study goal and get a ranked list of compatible students.</p>
        <ol className="home-finder-steps">{STEPS.map((step, index) => <li key={step}><span>{index + 1}</span>{step}</li>)}</ol>
        <StatePanel loading={(profile.loading && !profile.data) || (matches.loading && !matches.data)} error={profile.error || matches.error} onRetry={() => profile.error ? profile.reload(true) : matches.reload(true)}
          empty={enabled && matches.data?.matches.length === 0} emptyTitle="No matches for your saved preferences yet" emptyMessage="Try another course or study goal in Find buddies." emptyKind="search" />
        {profile.data && !profile.error && !enabled && <p>Choose a course or goal in the search page to get started, or <Link to={`/students/${profile.data?.id}/edit?tab=preferences`}>set a target course or study goal in your profile</Link> for a live preview here.</p>}
        {enabled && <p className="row-meta">Preview for {target ? `${target.code} (your saved target course)` : label(goal!)}. The search page lets you choose a different course or goal.</p>}
        {matches.data && <>
          <p className="row-meta">Top {preview.length} of {matches.data.totalMatches} · {STRATEGIES.find(strategy => strategy.value === matches.data?.strategy)?.label} matching · configuration version {matches.data.configurationVersion}</p>
          <ul className="home-match-preview" aria-label="Top matching students">
            {preview.map(match => <li key={match.studentId}>
              <Link to={`/students/${match.studentId}`}>{match.name}</Link>{' '}
              <Badge tone={QUALITY_TONES[match.quality]}>{QUALITY_LABELS[match.quality]}</Badge>
              <p>{new Intl.NumberFormat('en', { style: 'percent', maximumFractionDigits: 1 }).format(match.totalScore)} compatibility · {match.sharedHoursPerWeek} shared hours a week</p>
              <p>{match.explanation}</p>
            </li>)}
          </ul>
        </>}
        <p><Link className="retro-button primary" to={`/find-buddies${query}`}>Search for study buddies</Link></p>
      </div>
    </section>
  )
}
