import { useState } from 'react'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import StatePanel from '../../shared/components/StatePanel'
import type { StudySession } from '../sessions/api'
import { AI_PLAN_LABEL, deleteStudyPlan, getSessionPlan } from './api'
import type { StudyPlan } from './api'
import RoadmapView from './RoadmapView'
import SessionPlanForm from './SessionPlanForm'
import { useEntryProgress } from './useEntryProgress'
import './studyplan.css'

/**
 * A study session's roadmap, on the session's page: what each upcoming meeting covers. Whoever
 * manages the session creates, replaces, ticks and deletes it; other participants read it.
 */
export default function SessionRoadmapPanel({ session }: { session: StudySession }) {
  const plan = useResource(`session-plan-${session.id}`, signal => getSessionPlan(session.id, signal))
  const [editing, setEditing] = useState(false)
  const [notice, setNotice] = useState<string>()
  const canPlan = session.canManage && session.status === 'SCHEDULED' && session.nextOccurrence !== null

  function created() {
    setEditing(false)
    setNotice('Session roadmap saved. Everyone in the session can see it.')
    plan.reload()
  }

  return <section className="detail-panel session-roadmap" aria-labelledby="session-roadmap-heading">
    <h2 id="session-roadmap-heading">Session roadmap</h2>
    <ActionNotice success={notice} />
    <StatePanel loading={plan.loading && plan.data === undefined} error={plan.error} onRetry={() => plan.reload()} />
    {editing && <SessionPlanForm session={session} replacing={!!plan.data} onCreated={created} onCancel={() => setEditing(false)} />}
    {!editing && plan.data === null && <StatePanel empty emptyKind="clock" emptyTitle="No roadmap for this session yet"
      emptyMessage={emptyMessage(session, canPlan)}
      emptyAction={canPlan ? <Button variant="primary" onClick={() => { setNotice(undefined); setEditing(true) }}>Plan these meetings</Button> : undefined} />}
    {!editing && plan.data && <PlanShown session={session} plan={plan.data} canPlan={canPlan}
      onReplace={() => { setNotice(undefined); setEditing(true) }} onDeleted={() => { setNotice('Session roadmap deleted.'); plan.reload() }} />}
  </section>
}

function emptyMessage(session: StudySession, canPlan: boolean) {
  if (canPlan) return 'Upload the slides or paste the topics, and Gemini plans what each upcoming meeting covers.'
  if (session.status === 'CANCELLED' || session.nextOccurrence === null) return 'This session has no upcoming meetings to plan.'
  return session.scope === 'GROUP' ? 'The group leader can create one for everyone.' : 'Either of you can create one.'
}

function PlanShown({ session, plan: loaded, canPlan, onReplace, onDeleted }: {
  session: StudySession; plan: StudyPlan; canPlan: boolean; onReplace: () => void; onDeleted: () => void
}) {
  const progress = useEntryProgress(loaded)
  const removal = useAction()
  const [confirming, setConfirming] = useState(false)
  const plan = progress.plan
  const done = plan.entries.filter(entry => entry.completed).length
  // Editing the session does not change its roadmap, so the dates may no longer match.
  const outdated = session.status === 'SCHEDULED' && Date.parse(session.updatedAt) > Date.parse(plan.createdAt)

  async function remove() {
    const result = await removal.run(() => deleteStudyPlan(plan.id))
    if (result.ok) {
      setConfirming(false)
      onDeleted()
    }
  }

  return <>
    <p>{plan.course.code} · {done} of {plan.entries.length} topics covered ·{' '}
      {plan.sourceKind === 'UPLOAD' ? `drafted from ${plan.sourceFilenames.join(', ')}` : 'drafted from pasted topics'}</p>
    <p className="study-plan-ai-label"><span aria-hidden="true">✦ </span>{AI_PLAN_LABEL}</p>
    {outdated && <p className="field-error" role="status">This session was changed after the roadmap was made, so its meeting dates may not match.
      {plan.canManage && canPlan ? ' Replace the roadmap to plan the current dates.' : ''}</p>}
    <ActionNotice error={progress.error} />
    <RoadmapView plan={plan} savingEntryId={progress.savingEntryId} onToggle={plan.canManage ? progress.toggle : undefined} />
    {plan.canManage && <div className="actions">
      {canPlan && <Button onClick={onReplace}>Replace roadmap</Button>}
      <Button variant="danger" onClick={() => { removal.clear(); setConfirming(true) }}>Delete roadmap</Button>
    </div>}
    {confirming && <ConfirmDialog title="Delete this session’s roadmap?" confirmLabel="Delete roadmap" pending={removal.pending}
      error={removal.error} onConfirm={remove} onClose={() => setConfirming(false)}>
      <p>Everyone in the session loses the roadmap and its progress. The session itself is kept.</p>
    </ConfirmDialog>}
  </>
}
