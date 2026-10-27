import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { formatTimestamp, label } from '../../shared/api/types'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import { AI_PLAN_LABEL, deleteStudyPlan, getStudyPlan, markEntry } from './api'
import type { StudyPlan, StudyPlanEntry } from './api'
import { formatMinutes, formatPlanDate, weekCountLabel } from './planDates'
import RoadmapView from './RoadmapView'
import './studyplan.css'

/** One of the student's plans: its details, the roadmap to tick off, and deleting it. */
export default function StudyPlanDetailPage() {
  const { id } = useParams()
  const planId = Number(id)
  const valid = Number.isSafeInteger(planId) && planId > 0
  const plan = useResource(`study-plan-${planId}`, signal => getStudyPlan(planId, signal), valid)
  const course = plan.data?.course
  return <WindowPage title={course ? `${course.code} study plan` : 'Study plan'}
    description={course?.name} actions={<Link className="retro-button" to="/study-plan">Back to study plans</Link>}>
    <StatePanel loading={plan.loading && !plan.data} error={valid ? plan.error : 'This study plan link is invalid.'}
      onRetry={valid ? () => plan.reload() : undefined} />
    {plan.data && <PlanView plan={plan.data} />}
  </WindowPage>
}

function PlanView({ plan: loaded }: { plan: StudyPlan }) {
  const navigate = useViewNavigation()
  const progress = useAction(), removal = useAction()
  const [saving, setSaving] = useState<number>()
  const [confirming, setConfirming] = useState(false)
  // A tick shows the server's reply at once; the next load of the plan replaces it.
  const [saved, setSaved] = useState<StudyPlan>()
  const [shownLoad, setShownLoad] = useState(loaded)
  if (shownLoad !== loaded) {
    setShownLoad(loaded)
    setSaved(undefined)
  }
  const plan = saved ?? loaded
  const done = plan.entries.filter(entry => entry.completed).length

  async function toggle(entry: StudyPlanEntry, completed: boolean) {
    setSaving(entry.id)
    const result = await progress.run(() => markEntry(plan.id, entry.id, completed))
    if (result.ok) setSaved(result.value)
    setSaving(undefined)
  }

  async function remove() {
    const result = await removal.run(() => deleteStudyPlan(plan.id), undefined, true)
    if (result.ok) navigate('/study-plan', { state: { notice: 'Study plan deleted.' } })
  }

  return <>
    <div className="tag-list">
      <Badge tone={done === plan.entries.length && done > 0 ? 'good' : 'neutral'}>{done} of {plan.entries.length} topics done</Badge>
      <Badge>{weekCountLabel(plan.blockCount)}</Badge>
      <Badge>{formatMinutes(plan.minutesPerBlock)} a week</Badge>
      {plan.studyGoal && <Badge>{label(plan.studyGoal)}</Badge>}
    </div>
    <p>{plan.examDate ? `Exam on ${formatPlanDate(plan.examDate)}.` : 'This week only, no exam date.'}
      {' '}Starts {formatPlanDate(plan.firstBlockDate)}.</p>
    <p className="row-meta">{plan.sourceKind === 'UPLOAD' ? `Drafted from ${plan.sourceFilenames.join(', ')}` : 'Drafted from pasted topics'}
      {' '}· Created {formatTimestamp(plan.createdAt)}</p>
    <p className="study-plan-ai-label"><span aria-hidden="true">✦ </span>{AI_PLAN_LABEL}</p>
    {!!plan.sourceTopics.length && <details className="study-plan-topics">
      <summary>Topics found in your material ({plan.sourceTopics.length})</summary>
      <ol>{plan.sourceTopics.map(topic => <li key={topic}>{topic}</li>)}</ol>
    </details>}

    <ActionNotice error={progress.error} />
    <StatePanel empty={!plan.entries.length} emptyTitle="This plan has no topics" emptyKind="clock"
      emptyMessage="Delete it and create a new plan from your material." />
    {!!plan.entries.length && <RoadmapView plan={plan} savingEntryId={saving} onToggle={plan.canManage ? toggle : undefined} />}

    {plan.canManage && <div className="actions">
      <Button variant="danger" onClick={() => { removal.clear(); setConfirming(true) }}>Delete study plan</Button>
    </div>}
    {confirming && <ConfirmDialog title="Delete this study plan?" confirmLabel="Delete study plan" pending={removal.pending}
      error={removal.error} onConfirm={remove} onClose={() => setConfirming(false)}>
      <p>The {plan.course.code} roadmap and its progress are removed for good. Your uploaded files were never stored, so upload them again to make a new plan.</p>
    </ConfirmDialog>}
  </>
}
