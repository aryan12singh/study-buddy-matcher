import { Link } from 'react-router-dom'
import { getMyProfile } from '../profile/api'
import { getStudentSummary } from '../students/api'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import Button from '../../shared/components/Button'
import { startSteps } from './startSteps'

/**
 * The first steps for a new student. The links always work; ticks appear once the
 * student's own profile and activity summary have loaded, and a failed load says so.
 */
export default function StartChecklist() {
  const { account } = useAuth()
  const profile = useResource('help-profile', getMyProfile)
  const summary = useResource('help-summary', getStudentSummary)
  const steps = startSteps(account?.id ?? 0, profile.data, summary.data)
  const known = steps.filter(step => step.done !== undefined)
  const done = steps.filter(step => step.done).length
  const loading = profile.loading || summary.loading
  const error = profile.error ?? summary.error
  const retry = () => {
    if (profile.error) profile.reload()
    if (summary.error) summary.reload()
  }

  return (
    <section className="help-section help-start" aria-labelledby="help-start-title">
      <div className="help-section-head">
        <div>
          <span className="help-eyebrow">Step by step</span>
          <h2 id="help-start-title">Start here</h2>
        </div>
        {known.length === steps.length && (
          <p className="help-meter" role="img" aria-label={`${done} of ${steps.length} steps done`}>
            <span className="help-meter-track" aria-hidden="true">
              {steps.map(step => <i key={step.key} className={step.done ? 'done' : undefined} />)}
            </span>
            <span aria-hidden="true">{done}/{steps.length} done</span>
          </p>
        )}
      </div>
      {loading && !error && known.length < steps.length && (
        <p className="help-status" role="status">Checking your progress…</p>
      )}
      {error && (
        <div className="help-status help-status-error" role="alert">
          <span>We could not check your progress. The steps below still work. {error}</span>
          <Button onClick={retry}>Check progress again</Button>
        </div>
      )}
      <ol className="help-steps">
        {steps.map((step, index) => (
          <li key={step.key} className={`help-step${step.done ? ' is-done' : ''}`}>
            <span className="help-step-marker" aria-hidden="true">{step.done ? '' : index + 1}</span>
            <div className="help-step-text">
              <h3>{step.title}</h3>
              <p>{step.detail}</p>
              {step.done !== undefined && (
                <span className="help-step-state">{step.done ? 'Done' : 'Not done yet'}</span>
              )}
            </div>
            <Link className={`retro-button ${step.done ? 'secondary' : 'primary'}`} to={step.to}>{step.action}</Link>
          </li>
        ))}
      </ol>
    </section>
  )
}
