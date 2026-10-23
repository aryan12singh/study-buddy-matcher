import { Link } from 'react-router-dom'
import PixelIcon from '../../shared/components/PixelIcon'
import type { Explainer } from './helpContent'

export default function ExplainerCard({ explainer }: { explainer: Explainer }) {
  const headingId = `help-${explainer.key}`
  return (
    <article className="help-card" aria-labelledby={headingId}>
      <div className="help-card-head">
        <span className="help-card-icon"><PixelIcon kind={explainer.icon} /></span>
        <h3 id={headingId} tabIndex={-1}>{explainer.title}</h3>
      </div>
      <p className="help-card-summary">{explainer.summary}</p>
      <ul>
        {explainer.points.map(point => <li key={point}>{point}</li>)}
      </ul>
      <Link className="help-card-link" to={explainer.to}>{explainer.action}</Link>
    </article>
  )
}
