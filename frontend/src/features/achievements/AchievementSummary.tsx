import type { Achievements } from './api'

function weeks(count: number) {
  return `${count} ${count === 1 ? 'week' : 'weeks'}`
}

/** Current standing, including an attainable final level rather than an endless next target. */
export default function AchievementSummary({ data, compact = false }: { data: Achievements; compact?: boolean }) {
  const capped = data.pointsToNextLevel === 0
  const levelProgress = data.maxPoints > 0 ? data.points / data.maxPoints : 0
  return (
    <dl className={`achievement-summary${compact ? ' achievement-summary-compact' : ''}`}>
      <div className="summary-level">
        <dt>Current level</dt>
        <dd>
          <strong>{data.level}</strong>
          <small>of {data.maxLevel}</small>
          {!compact && <span className="level-bar" role="progressbar" aria-label="Current badge points"
            aria-valuemin={0} aria-valuemax={data.maxPoints} aria-valuenow={data.points}
            aria-valuetext={`${data.points} of ${data.maxPoints} badge points; ${capped ? 'maximum level' : `${data.pointsToNextLevel} to the next level`}`}>
            <span style={{ width: `${Math.max(0, Math.min(1, levelProgress)) * 100}%` }} />
          </span>}
          {!compact && <small>{capped ? 'Maximum level · all badges Gold' : `${data.pointsToNextLevel} badge ${data.pointsToNextLevel === 1 ? 'point' : 'points'} to level ${data.level + 1}`}</small>}
        </dd>
      </div>
      <div><dt>Badges with a tier</dt><dd><strong>{data.earnedCount}</strong><small>of {data.totalCount}</small></dd></div>
      <div><dt>Current planned streak</dt><dd><strong>{data.streak.currentWeeks}</strong><small>{weeks(data.streak.currentWeeks)} in a row</small></dd></div>
      {!compact && <div><dt>Best eligible run</dt><dd><strong>{data.streak.bestWeeks}</strong><small>{weeks(data.streak.bestWeeks)} in current records</small></dd></div>}
    </dl>
  )
}
