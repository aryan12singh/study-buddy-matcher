import type { Achievements } from './api'

function weeks(count: number) {
  return `${count} ${count === 1 ? 'week' : 'weeks'}`
}

/** Level, badge count and streak figures in the window-chrome stat style. */
export default function AchievementSummary({ data }: { data: Achievements }) {
  const levelProgress = data.pointsPerLevel > 0 ? 1 - data.pointsToNextLevel / data.pointsPerLevel : 0
  return (
    <dl className="achievement-summary">
      <div className="summary-level">
        <dt>Level</dt>
        <dd>
          <strong>{data.level}</strong>
          <span className="level-bar" aria-hidden="true"><span style={{ width: `${levelProgress * 100}%` }} /></span>
          <small>{data.pointsToNextLevel} badge {data.pointsToNextLevel === 1 ? 'point' : 'points'} to level {data.level + 1}</small>
        </dd>
      </div>
      <div><dt>Badges earned</dt><dd><strong>{data.earnedCount}</strong><small>of {data.totalCount}</small></dd></div>
      <div><dt>Current streak</dt><dd><strong>{data.streak.currentWeeks}</strong><small>{weeks(data.streak.currentWeeks)} in a row</small></dd></div>
      <div><dt>Best streak</dt><dd><strong>{data.streak.bestWeeks}</strong><small>{weeks(data.streak.bestWeeks)} at most</small></dd></div>
    </dl>
  )
}
