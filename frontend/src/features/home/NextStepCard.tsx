import { Link } from 'react-router-dom'
import type { NextStep } from './nextStep'

export default function NextStepCard({ step }: { step: NextStep }) {
  return (
    <section className="home-next" aria-label="Next step">
      <div className="home-next-text">
        <span className="home-eyebrow">Next step</span>
        <strong>{step.title}</strong>
        <span>{step.detail}</span>
        {step.progress && (
          <span className="home-progress" role="img" aria-label={`Profile setup: ${step.progress.done} of ${step.progress.total} steps done`}>
            {Array.from({ length: step.progress.total }, (_, index) => (
              <i key={index} className={index < step.progress!.done ? 'done' : undefined} />
            ))}
          </span>
        )}
      </div>
      <Link className="retro-button primary" to={step.to}>{step.action}</Link>
    </section>
  )
}
