import { useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { searchMatches } from './api'
import type { MatchResult, MatchSearch } from './api'
import {
  GROUP_SIZE_FILTERS, QUALITY_FILTERS, SHARED_HOURS_FILTERS, SORT_OPTIONS, STUDY_MODE_FILTERS, hasSearchContext, toForm, toSearch
} from './matchSearchForm'
import type { MatchSearchForm } from './matchSearchForm'
import { STRATEGIES } from './matchingSettingsForm'
import { getMyProfile } from '../profile/api'
import { getCourses } from '../../shared/api/courses'
import { STUDY_GOALS } from '../../shared/api/types'
import Field from '../../shared/components/Field'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { label } from '../../shared/api/types'
import type { Course } from '../../shared/api/types'
import Button from '../../shared/components/Button'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import FilterSummary from '../../shared/components/FilterSummary'
import SendRequestDialog from '../connections/SendRequestDialog'
import MatchCard from './MatchCard'
import MatchFilters from './MatchFilters'

const FILTERS_ID = 'match-filters'

/**
 * The matching search: students ranked by the admin's strategy, narrowed by
 * optional filters, keeping the order returned by the strategy.
 */
export default function FindBuddiesPanel() {
  const { account } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialSearch = searchFromUrl(searchParams)
  const courses = useResource('match-course-catalog', getCourses, true, false)
  const profile = useResource('match-search-profile', getMyProfile, true, false)
  const [form, setForm] = useState<MatchSearchForm>(() => toForm(initialSearch)),
    [search, setSearch] = useState<MatchSearch>(initialSearch),
    [filtersOpen, setFiltersOpen] = useState(!hasSearchContext(initialSearch)),
    [searchError, setSearchError] = useState<string>()
  const [sendingTo, setSendingTo] = useState<MatchResult | null>(null),
    [notice, setNotice] = useState<string>()
  const [pendingRequests, setPendingRequests] = useState<{ studentId: number; refreshSequence: number }[]>([])
  const searchSequence = useRef(0)
  const results = useResource(`match-search-${JSON.stringify(search)}`, async signal => {
    const loadSequence = ++searchSequence.current
    return { ...await searchMatches(search, signal), loadSequence }
  }, hasSearchContext(search), false)
  // Only the post-send refresh or a later search may clear a confirmed request;
  // an older response can arrive while another student's POST is still pending.
  const matches = results.data?.matches.map(match => match.relationship === 'STRANGER'
    && pendingRequests.some(request => request.studentId === match.studentId && request.refreshSequence > (results.data?.loadSequence ?? 0))
    ? { ...match, relationship: 'OUTGOING_PENDING' as const } : match)
  const strategy = STRATEGIES.find(({ value }) => value === results.data?.strategy)
  const selectedSort = results.data?.sort || search.sort || 'BEST_MATCH'
  const sortLabel = SORT_OPTIONS.find(option => option.value === selectedSort)?.label
  const filterCount = Object.keys(search).filter(key => key !== 'sort' && key !== 'limit').length
  const profileEditLink = account ? `/students/${account.id}/edit` : undefined
  const incomplete = profile.data && (!profile.data.coursesTaken?.length || !profile.data.preferredStudyMode || !profile.data.availability?.length)

  function apply(next: MatchSearch) {
    setNotice(undefined)
    setSearchError(undefined)
    setSearchParams(Object.fromEntries(Object.entries(next).map(([key, value]) => [key, String(value)])), { replace: true })
    setForm(toForm(next))
    // Applying the same filters again fetches fresh results instead of doing nothing.
    if (JSON.stringify(next) === JSON.stringify(search)) results.reload(true)
    else setSearch(next)
  }

  function remove(field: keyof MatchSearch) {
    const next = { ...search }
    delete next[field]
    apply(next)
    if (!hasSearchContext(next)) setFiltersOpen(true)
  }

  const chips = filterChips(search, courses.data).map(chip => ({ ...chip, onRemove: () => remove(chip.key) }))

  return (
    <>
      {incomplete && (
        <div className="route-notice" role="status">
          <p>Your profile is incomplete. Add courses, a study mode and weekly availability for a more useful comparison.</p>
          {profileEditLink && <Link to={profileEditLink}>Complete your study profile</Link>}
        </div>
      )}
      <div className="match-toolbar">
        <div>
          <p className="match-count">{hasSearchContext(search) ? resultText(matches?.length, results.data?.totalMatches) : 'Choose a course or study goal to start'}</p>
          <p className="row-meta">
            {strategy ? selectedSort === 'BEST_MATCH'
              ? `Ranked by ${strategy.label} matching. ${strategy.description}`
              : `Sorted by ${sortLabel}. ${strategy.label} matching breaks ties. ${strategy.description}` : ''}
            {strategy && ` Configuration version ${results.data?.configurationVersion}.`}
            {' '}Your saved target course helps compare preferences; it does not select this search.
          </p>
        </div>
        <Button aria-expanded={filtersOpen} aria-controls={FILTERS_ID} onClick={() => setFiltersOpen(open => !open)}>
          {filterCount ? `Filters · ${filterCount}` : 'Filters'}
        </Button>
      </div>
      <Field id="match-sort" label="Sort matches">
        <select id="match-sort" value={search.sort || 'BEST_MATCH'} disabled={!hasSearchContext(search)} onChange={event => apply({ ...search, sort: event.target.value as MatchSearch['sort'] })}>
          {SORT_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
      </Field>
      {filtersOpen && (
        <MatchFilters
          id={FILTERS_ID}
          form={form}
          courses={courses.data}
          coursesLoading={courses.loading}
          coursesError={Boolean(courses.error)}
          profileEditLink={profileEditLink}
          searchError={searchError}
          onChange={(field, value) => { setSearchError(undefined); setForm(previous => ({ ...previous, [field]: value })) }}
          onApply={() => {
            const next = toSearch(form)
            if (!hasSearchContext(next)) { setSearchError('Choose a course or study goal before searching.'); return }
            apply(next); setFiltersOpen(false)
          }}
          onClear={() => apply({})}
        />
      )}
      {courses.error && filtersOpen && <Button onClick={() => courses.reload(true)}>Retry courses</Button>}
      {profile.error && <p className="field-hint">Your profile could not be loaded. You can still search. <Button onClick={() => profile.reload(true)}>Retry profile</Button></p>}
      <FilterSummary filters={chips} onClear={() => { apply({}); setFiltersOpen(true) }} />
      {!hasSearchContext(search) && <StatePanel empty emptyTitle="Start your first matching search" emptyMessage="Choose a course or study goal above. Add optional filters to compare students who suit your plans." emptyKind="search" />}
      <ActionNotice success={notice} />
      <StatePanel
        loading={results.loading && !results.data}
        error={results.error}
        onRetry={() => results.reload(true)}
        empty={matches?.length === 0}
        emptyTitle="No students match this search"
        emptyMessage="Try another course or goal, or relax the shared free time or match quality filters. Existing connections are excluded."
        emptyKind="search"
      />
      <div className="data-list" aria-label="Matching students">
        {matches?.map(match => (
          <MatchCard key={match.studentId} match={match} search={search} onSendRequest={() => { setNotice(undefined); setSendingTo(match) }} />
        ))}
      </div>
      {sendingTo && (
        <SendRequestDialog
          receiverId={sendingTo.studentId}
          receiverName={sendingTo.name}
          context={{ origin: 'MATCHING', courseId: search.courseId, studyGoal: search.studyGoal }}
          onClose={() => setSendingTo(null)}
          onSent={() => {
            // useAction starts the post-send refresh before calling onSent.
            // Read the ref now: the dialog may retain an older callback while sending.
            const refreshSequence = searchSequence.current
            setPendingRequests(previous => [
              ...previous.filter(request => request.studentId !== sendingTo.studentId),
              { studentId: sendingTo.studentId, refreshSequence }
            ])
            setNotice(`Match request sent to ${sendingTo.name}. Track it under Outgoing requests on Connections.`)
          }}
        />
      )}
    </>
  )
}

/** One removable chip per applied filter, in the same order as the form. */
function filterChips(search: MatchSearch, courses: Course[] | undefined): { key: keyof MatchSearch; label: string }[] {
  const chips: { key: keyof MatchSearch; label: string }[] = []
  if (search.courseId !== undefined) {
    const course = courses?.find(candidate => candidate.id === search.courseId)
    chips.push({ key: 'courseId', label: course ? `${course.code} ${course.name}` : 'Selected course' })
  }
  if (search.studyGoal) chips.push({ key: 'studyGoal', label: label(search.studyGoal) })
  if (search.studyMode) chips.push({ key: 'studyMode', label: optionLabel(STUDY_MODE_FILTERS, search.studyMode) })
  if (search.groupSize) chips.push({ key: 'groupSize', label: optionLabel(GROUP_SIZE_FILTERS, search.groupSize) })
  if (search.minSharedHours) chips.push({ key: 'minSharedHours', label: optionLabel(SHARED_HOURS_FILTERS, String(search.minSharedHours)) })
  if (search.minQuality) chips.push({ key: 'minQuality', label: optionLabel(QUALITY_FILTERS, search.minQuality) })
  return chips
}

function optionLabel(options: { value: string; label: string }[], value: string) {
  return options.find(option => option.value === value)?.label || label(value)
}

function resultText(count: number | undefined, total: number | undefined) {
  if (count === undefined) return 'Finding study buddies…'
  const students = `${count} ${count === 1 ? 'student' : 'students'}`
  return total !== undefined && total > count ? `Showing ${count} of ${total} matching students. Narrow your filters to see others.` : `${students} match your search`
}

/** Shared links from Home select a search without changing profile preferences. */
function searchFromUrl(params: URLSearchParams): MatchSearch {
  const course = Number(params.get('courseId')), goal = params.get('studyGoal'), hours = Number(params.get('minSharedHours'))
  const mode = params.get('studyMode'), size = params.get('groupSize'), quality = params.get('minQuality'), sort = params.get('sort')
  return {
    ...(Number.isInteger(course) && course > 0 ? { courseId: course } : {}),
    ...(STUDY_GOALS.includes(goal as typeof STUDY_GOALS[number]) ? { studyGoal: goal as typeof STUDY_GOALS[number] } : {}),
    ...(mode && STUDY_MODE_FILTERS.some(option => option.value === mode) ? { studyMode: mode as MatchSearch['studyMode'] } : {}),
    ...(size && GROUP_SIZE_FILTERS.some(option => option.value === size) ? { groupSize: size as MatchSearch['groupSize'] } : {}),
    ...(quality && QUALITY_FILTERS.some(option => option.value === quality) ? { minQuality: quality as MatchSearch['minQuality'] } : {}),
    ...(Number.isFinite(hours) && hours > 0 && hours <= 168 ? { minSharedHours: hours } : {}),
    ...(sort && SORT_OPTIONS.some(option => option.value === sort) ? { sort: sort as MatchSearch['sort'] } : {})
  }
}
