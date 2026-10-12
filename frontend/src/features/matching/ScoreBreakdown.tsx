import type { MatchResult } from './api'
import { CRITERION_LABELS, criterionVerdict } from './matchPresentation'

/** One bar per matching criterion, so a student can see why a match ranks where it does. */
export default function ScoreBreakdown({ match }: { match: MatchResult }) {
  return (
    <ul className="score-breakdown" aria-label="Score breakdown">
      {match.breakdown.map(({ criterion, score }) => {
        const verdict = criterionVerdict(criterion, score, match.sharedHoursPerWeek)
        return (
          <li key={criterion} className={`score-${criterion.toLowerCase().replaceAll('_', '-')}`}>
            <div className="score-caption"><span>{CRITERION_LABELS[criterion]}</span><span>{verdict}</span></div>
            <div className="score-track" role="meter" aria-label={`${CRITERION_LABELS[criterion]} score`}
              aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(score * 100)} aria-valuetext={verdict}>
              <span style={{ width: `${score * 100}%` }} />
            </div>
          </li>
        )
      })}
    </ul>
  )
}
