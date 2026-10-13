import { useRef, useState } from 'react'
import { searchMatches } from './api'
import type { MatchResult, MatchSearch } from './api'
import {
  EMPTY_SEARCH, GROUP_SIZE_FILTERS, QUALITY_FILTERS, SHARED_HOURS_FILTERS, STUDY_MODE_FILTERS, toForm, toSearch
} from './matchSearchForm'
import type { MatchSearchForm } from './matchSearchForm'
import { STRATEGIES } from './matchingSettingsForm'
import { getMyProfile } from '../profile/api'
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
 * The matching search: everyone ranked by the admin's strategy, narrowed by
 * optional filters, keeping the order returned by the strategy.
 */
export default function FindBuddiesPanel() {
  const { account } = useAuth()
  const profile = useResource('match-search-profile', getMyProfile, true, false)
  const [form, setForm] = useState<MatchSearchForm>(EMPTY_SEARCH),
    [search, setSearch] = useState<MatchSearch>({}),
    [filtersOpen, setFiltersOpen] = useState(false)
  const [sendingTo, setSendingTo] = useState<MatchResult | null>(null),
    [notice, setNotice] = useState<string>()
  const [pendingRequests, setPendingRequests] = useState<{ studentId: number; refreshSequence: number }[]>([])
  const searchSequence = useRef(0)
  const results = useResource(`match-search-${JSON.stringify(search)}`, async signal => {
    const loadSequence = ++searchSequence.current
    return { ...await searchMatches(search, signal), loadSequence }
  }, true, false)
  const myCourses = profile.data ? searchableCourses(profile.data.coursesTaken, profile.data.targetCourse) : undefined
  // Only the post-send refresh or a later search may clear a confirmed request;
  // an older response can arrive while another student's POST is still pending.
  const matches = results.data?.matches.map(match => match.relationship === 'STRANGER'
    && pendingRequests.some(request => request.studentId === match.studentId && request.refreshSequence > (results.data?.loadSequence ?? 0))
    ? { ...match, relationship: 'OUTGOING_PENDING' as const } : match)
  const strategy = STRATEGIES.find(({ value }) => value === results.data?.strategy)
  const filterCount = Object.keys(search).length

  function apply(next: MatchSearch) {
    setNotice(undefined)
    setForm(toForm(next))
    // Applying the same filters again fetches fresh results instead of doing nothing.
    if (JSON.stringify(next) === JSON.stringify(search)) results.reload(true)
    else setSearch(next)
  }

  function remove(field: keyof MatchSearch) {
    const next = { ...search }
    delete next[field]
    apply(next)
  }

  const chips = filterChips(search, myCourses).map(chip => ({ ...chip, onRemove: () => remove(chip.key) }))

  return (
    <>
      <div className="match-toolbar">
        <div>
          <p className="match-count">{resultText(matches?.length, filterCount)}</p>
          <p className="row-meta">
            {strategy ? `Ranked by ${strategy.label} matching. ${strategy.description}` : ''}
            {' '}Your target course counts even without a course filter.
          </p>
        </div>
        <Button aria-expanded={filtersOpen} aria-controls={FILTERS_ID} onClick={() => setFiltersOpen(open => !open)}>
          {filterCount ? `Filters · ${filterCount}` : 'Filters'}
        </Button>
      </div>
      {filtersOpen && (
        <MatchFilters
          id={FILTERS_ID}
          form={form}
          courses={myCourses}
          coursesLoading={profile.loading}
          coursesError={Boolean(profile.error)}
          profileEditLink={account ? `/students/${account.id}/edit` : undefined}
          onChange={(field, value) => setForm(previous => ({ ...previous, [field]: value }))}
          onApply={() => { apply(toSearch(form)); setFiltersOpen(false) }}
          onClear={() => apply({})}
        />
      )}
      <FilterSummary filters={chips} onClear={() => apply({})} />
      <ActionNotice success={notice} />
      <StatePanel
        loading={results.loading && !results.data}
        error={results.error}
        onRetry={() => results.reload(true)}
        empty={matches?.length === 0}
        emptyTitle={filterCount ? 'No students match these filters' : 'No other students to match yet'}
        emptyMessage={filterCount ?
          'Remove a filter, or relax the shared free time or match quality filters.'
          : 'Matches appear here once other students have set up their study profiles.'}
        emptyKind="search"
      />
      <div className="data-list" aria-label="Matching students">
        {matches?.map(match => (
          <MatchCard key={match.studentId} match={match} onSendRequest={() => { setNotice(undefined); setSendingTo(match) }} />
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

function resultText(count: number | undefined, filterCount: number) {
  if (count === undefined) return 'Finding study buddies…'
  const students = `${count} ${count === 1 ? 'student' : 'students'}`
  return filterCount ? `${students} match your filters` : `${students} to study with`
}

/** The student's courses taken plus their target course if it is not among them, by course code. */
function searchableCourses(taken: Course[], target: Course | null) {
  const courses = target && !taken.some(course => course.id === target.id) ? [...taken, target] : taken
  return [...courses].sort((left, right) => left.code.localeCompare(right.code))
}
