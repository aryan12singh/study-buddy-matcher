import { Link } from 'react-router-dom'
import { getMyProfile } from '../profile/api'
import { getStudentSummary } from '../students/api'
import { getMyGroups } from '../groups/api'
import { getMySessions } from '../sessions/api'
import { useSessionClock } from '../sessions/useSessionClock'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import Button from '../../shared/components/Button'
import { helpNextStep } from './helpNextStep'
import { startSteps } from './startSteps'

const NO_SESSIONS: never[] = []

/** Progress uses current authorized records. Opening Help never joins a room or calls AI. */
export default function StartChecklist() {
  const { account } = useAuth()
  const student = account?.role === 'STUDENT'
  const profile = useResource(`help-profile-${account?.id}`, getMyProfile, student)
  const summary = useResource(`help-summary-${account?.id}`, getStudentSummary, student)
  const groups = useResource(`help-groups-${account?.id}`, getMyGroups, student)
  const sessions = useResource(`help-sessions-${account?.id}`, getMySessions, student)
  const now = useSessionClock(sessions.data ?? NO_SESSIONS)
  const sources = { profile, summary, groups, sessions }
  const steps = startSteps(account?.id ?? 0, sources, now)
  const tracked = steps.filter(step => step.state !== 'untracked')
  const confirmed = tracked.filter(step => step.state === 'confirmed').length
  const loading = tracked.some(step => step.state === 'loading')
  const failed = [
    { name: 'profile', source: profile }, { name: 'requests and connections', source: summary },
    { name: 'groups', source: groups }, { name: 'study sessions', source: sessions }
  ].filter(item => item.source.error)
  const recommendation = helpNextStep(account?.id ?? 0, sources, now)

  return (
    <section className="help-section help-start" aria-labelledby="help-start-title">
      <div className="help-section-head">
        <div>
          <span className="help-eyebrow">Step by step</span>
          <h2 id="help-start-title" tabIndex={-1}>Start here</h2>
        </div>
        <p className="help-meter" role="img" aria-label={`${confirmed} of ${tracked.length} current checks confirmed`}>
          <span className="help-meter-track" aria-hidden="true">
            {tracked.map(step => <i key={step.key} className={step.state} />)}
          </span>
          <span aria-hidden="true">{confirmed}/{tracked.length} current checks</span>
        </p>
      </div>
      <p className="help-progress-note">These checks show your current records, not everything you have ever done. Searches, room visits and calendar downloads are not tracked here.</p>
      <section className="help-next" aria-labelledby="help-next-title">
        <div aria-live="polite" aria-atomic="true">
          <span className="help-eyebrow">Recommended next action</span>
          <h3 id="help-next-title">{recommendation.step?.title ?? 'Your next action'}</h3>
          <p>{recommendation.step?.detail ?? recommendation.message}</p>
        </div>
        {recommendation.step && <Link className="retro-button primary" to={recommendation.step.to}>{recommendation.step.action}</Link>}
      </section>
      {loading && <p className="help-status" role="status">Checking current records… Guide links are ready to use.</p>}
      {failed.length > 0 && (
        <div className="help-status help-status-error" role="alert">
          <span>Could not check: {failed.map(item => item.name).join(', ')}. Unavailable checks are not marked incomplete.</span>
          <Button onClick={() => failed.forEach(item => item.source.reload(true))}>Check progress again</Button>
        </div>
      )}
      <ol className="help-steps">
        {steps.map((step, index) => (
          <li key={step.key} className={`help-step${step.state === 'confirmed' ? ' is-done' : ''}`}>
            <span className="help-step-marker" aria-hidden="true">{step.state === 'confirmed' ? '' : index + 1}</span>
            <div className="help-step-text">
              <h3>{step.title}</h3>
              <p>{step.detail}</p>
              <span className="help-step-state" data-state={step.state}>{step.stateLabel}</span>
            </div>
            <Link className="retro-button" to={step.to}>{step.action}</Link>
          </li>
        ))}
      </ol>
    </section>
  )
}
