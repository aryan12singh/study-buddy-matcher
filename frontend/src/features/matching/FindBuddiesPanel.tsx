import { useState } from 'react'
import { Link } from 'react-router-dom'
import { searchMatches } from './api'
import type { MatchResult, MatchSearch } from './api'
import {
  EMPTY_SEARCH, GROUP_SIZE_FILTERS, QUALITY_FILTERS, QUALITY_LABELS, QUALITY_TONES, SHARED_HOURS_FILTERS, STUDY_MODE_FILTERS,
  sharedHoursText, toSearch, validateSearch
} from './matchSearchForm'
import type { MatchSearchForm } from './matchSearchForm'
import { getMyProfile } from '../profile/api'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { label, STUDY_GOALS } from '../../shared/api/types'
import type { Course } from '../../shared/api/types'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import Avatar from '../../shared/components/Avatar'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import SendRequestDialog from '../connections/SendRequestDialog'

/** The matching search on the Connections page: a course or study goal, optional filters, and ranked results. */
export default function FindBuddiesPanel() {
  const { account } = useAuth()
  const profile = useResource('match-search-profile', getMyProfile, true, false)
  const [form, setForm] = useState<MatchSearchForm>(EMPTY_SEARCH),
    [errors, setErrors] = useState<Record<string, string>>({}),
    [search, setSearch] = useState<MatchSearch | null>(null),
    // null until the student picks a course, so the dropdown shows their target course by default.
    [chosenCourseId, setChosenCourseId] = useState<string | null>(null)
  const [sendingTo, setSendingTo] = useState<MatchResult | null>(null),
    [notice, setNotice] = useState<string>()
  const results = useResource(
    `match-search-${JSON.stringify(search)}`,
    signal => searchMatches(search as MatchSearch, signal),
    search !== null,
    false
  )
  const targetCourse = profile.data?.targetCourse
  const current: MatchSearchForm = { ...form, courseId: chosenCourseId ?? (targetCourse ? String(targetCourse.id) : '') }
  const myCourses = searchableCourses(profile.data?.coursesTaken || [], targetCourse || null)
  const noCourses = Boolean(profile.data) && myCourses.length === 0

  function change(field: keyof MatchSearchForm, value: string) {
    setForm(previous => ({ ...previous, [field]: value }))
  }

  function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateSearch(current)
    setErrors(invalid)
    setNotice(undefined)
    if (Object.keys(invalid).length) return
    const next = toSearch(current)
    // Searching again with the same filters fetches fresh results instead of doing nothing.
    if (JSON.stringify(next) === JSON.stringify(search)) results.reload(true)
    else setSearch(next)
  }

  return (
    <>
      <form
        className="stack-form"
        onSubmit={submit}
        noValidate
        aria-label="Search for study buddies"
      >
        <div className="form-grid">
          <Field
            id="match-course"
            label="Course"
            error={errors['match-course']}
            hint={profile.error ?
              'Your courses could not be loaded. You can still search by study goal.'
              : noCourses ? 'Add the courses you take to your profile to search by course.' : 'Your courses this term.'}
          >
            <select
              id="match-course"
              value={current.courseId}
              disabled={!profile.data || noCourses}
              onChange={event => setChosenCourseId(event.target.value)}
            >
              <option value="">{profile.loading && !profile.data ? 'Loading your courses…' : 'Any course'}</option>
              {myCourses.map(course => (
                <option key={course.id} value={course.id}>{course.code} {course.name}</option>
              ))}
            </select>
          </Field>
          <Field id="match-goal" label="Study goal">
            <select id="match-goal" value={form.studyGoal} onChange={event => change('studyGoal', event.target.value)}>
              <option value="">Any study goal</option>
              {STUDY_GOALS.map(goal => (
                <option key={goal} value={goal}>{label(goal)}</option>
              ))}
            </select>
          </Field>
          <Field id="match-mode" label="Study mode">
            <select id="match-mode" value={form.studyMode} onChange={event => change('studyMode', event.target.value)}>
              {STUDY_MODE_FILTERS.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
          <Field id="match-group-size" label="Group size">
            <select id="match-group-size" value={form.groupSize} onChange={event => change('groupSize', event.target.value)}>
              {GROUP_SIZE_FILTERS.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
          <Field id="match-hours" label="Shared free time" hint="Compared with the availability in your profile.">
            <select id="match-hours" value={form.minSharedHours} onChange={event => change('minSharedHours', event.target.value)}>
              {SHARED_HOURS_FILTERS.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
          <Field id="match-quality" label="Minimum match quality">
            <select id="match-quality" value={form.minQuality} onChange={event => change('minQuality', event.target.value)}>
              {QUALITY_FILTERS.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
        </div>
        {noCourses && account && (
          <p><Link to={`/students/${account.id}/edit`}>Add courses to your profile</Link></p>
        )}
        <div className="actions">
          <Button
            type="button"
            disabled={results.loading && search !== null}
            onClick={() => { setForm(EMPTY_SEARCH); setChosenCourseId(''); setErrors({}) }}
          >Clear filters</Button>
          <Button
            type="submit"
            variant="primary"
            disabled={results.loading && search !== null}
          >{results.loading && search !== null ? 'Searching…' : 'Search for study buddies'}</Button>
        </div>
      </form>
      <ActionNotice success={notice} />
      {search === null ? (
        <StatePanel
          empty
          emptyTitle="Find a study buddy"
          emptyMessage="Choose a course or a study goal, add any filters, then search. Results are ranked best match first."
          emptyKind="search"
        />
      ) : (
        <>
          <StatePanel
            loading={results.loading && !results.data}
            error={results.error}
            onRetry={() => results.reload(true)}
            empty={results.data?.length === 0}
            emptyTitle="No students match these filters"
            emptyMessage="Try a different course or study goal, or relax the shared free time or match quality filters."
            emptyKind="search"
          />
          <div className="data-list" aria-label="Matching students">
            {results.data?.map(match => (
              <article className="data-row" key={match.studentId}>
                <div>
                  <div className="person-heading"><Avatar name={match.name} /><h2>
                    <Link to={`/students/${match.studentId}`}>{match.name}</Link>
                  </h2></div>
                  <Badge tone={QUALITY_TONES[match.quality]}>{QUALITY_LABELS[match.quality]}</Badge>
                  <p className="row-meta">{match.school} · {match.programme} · Year {match.yearOfStudy}</p>
                  <p className="row-meta">{sharedHoursText(match.sharedHoursPerWeek)}</p>
                </div>
                <div className="actions">
                  <Link className="retro-button" to={`/students/${match.studentId}`}>View profile</Link>
                  <Button
                    variant="primary"
                    onClick={() => { setNotice(undefined); setSendingTo(match) }}
                  >Send match request</Button>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
      {sendingTo && search && (
        <SendRequestDialog
          receiverId={sendingTo.studentId}
          receiverName={sendingTo.name}
          context={{ origin: 'MATCHING', courseId: search.courseId, studyGoal: search.studyGoal }}
          onClose={() => setSendingTo(null)}
          onSent={() => setNotice(`Match request sent to ${sendingTo.name}. Track it under Outgoing requests.`)}
        />
      )}
    </>
  )
}

/** The student's courses taken plus their target course if it is not among them, by course code. */
function searchableCourses(taken: Course[], target: Course | null) {
  const courses = target && !taken.some(course => course.id === target.id) ? [...taken, target] : taken
  return [...courses].sort((left, right) => left.code.localeCompare(right.code))
}
