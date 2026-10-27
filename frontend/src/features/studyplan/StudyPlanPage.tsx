import { Link } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Badge from '../../shared/components/Badge'
import { listStudyPlans } from './api'
import type { StudyPlanSummary } from './api'
import { formatPlanDate, weekCountLabel } from './planDates'
import './studyplan.css'

/** The student's own personal study plans, newest first. */
export default function StudyPlanPage() {
  const { account } = useAuth()
  const plans = useResource(`study-plans-${account?.id}`, listStudyPlans)
  return <WindowPage title="Study plans"
    description="Week-by-week roadmaps for your courses, paced to your exam and the hours you have each week.">
    <StatePanel loading={plans.loading && !plans.data} error={plans.error} onRetry={() => plans.reload()} />
    {plans.data && <>
      <StatePanel empty={!plans.data.length} emptyTitle="No study plans yet" emptyKind="clock"
        emptyMessage="Upload your lecture slides or paste the topics, and Gemini drafts a week-by-week roadmap up to your exam." />
      {!!plans.data.length && <ul className="data-list study-plan-list" aria-label="Study plans">
        {plans.data.map(plan => <StudyPlanRow key={plan.id} plan={plan} />)}
      </ul>}
    </>}
  </WindowPage>
}

function StudyPlanRow({ plan }: { plan: StudyPlanSummary }) {
  const finished = plan.entryCount > 0 && plan.completedCount === plan.entryCount
  return <li className="data-row">
    <div>
      <h3><Link to={`/study-plan/${plan.id}`}>{plan.course.code} — {plan.course.name}</Link></h3>
      <div className="tag-list">
        <Badge tone={finished ? 'good' : 'neutral'}>{plan.completedCount} of {plan.entryCount} topics done</Badge>
        <Badge>{weekCountLabel(plan.blockCount)}</Badge>
        {plan.studyGoal && <Badge>{label(plan.studyGoal)}</Badge>}
      </div>
      <p>{plan.examDate ? `Exam on ${formatPlanDate(plan.examDate)}` : 'This week only, no exam date'}</p>
      <p className="row-meta">Starts {formatPlanDate(plan.firstBlockDate)}</p>
    </div>
    <div className="actions"><Link className="retro-button" to={`/study-plan/${plan.id}`}>Open study plan</Link></div>
  </li>
}
