import type { WeeklySlot } from '../../shared/api/types'
import { label, WEEK_DAYS } from '../../shared/api/types'
import { DAY_PARTS, heatLevel, sharedMinutesByDayPart } from './matchPresentation'

function hoursText(minutes: number) {
  if (!minutes) return 'not shared'
  const hours = Math.round(minutes / 6) / 10
  return `${hours} ${hours === 1 ? 'hour' : 'hours'} shared`
}

/** When in the week two students are both free, shaded by how much time they share. */
export default function SharedHoursHeatmap({ slots }: { slots: WeeklySlot[] }) {
  const grid = sharedMinutesByDayPart(slots)
  const sharedParts = WEEK_DAYS.flatMap(day => DAY_PARTS.flatMap((part, index) =>
    grid[day][index] > 0 ? [`${label(day)} ${part.label.toLowerCase()}: ${hoursText(grid[day][index])}`] : []
  ))
  return (
    <div className="shared-time">
      <table className="shared-heatmap">
        <caption className="visually-hidden">Shared free time by day and part of the day (SGT)</caption>
        <colgroup><col className="heatmap-labels" /></colgroup>
        <thead>
          <tr>
            <td className="heatmap-corner" />
            {WEEK_DAYS.map(day => <th key={day} scope="col" abbr={label(day)}>{day.charAt(0)}</th>)}
          </tr>
        </thead>
        <tbody>
          {DAY_PARTS.map((part, index) => (
            <tr key={part.label}>
              <th scope="row">{part.label}</th>
              {WEEK_DAYS.map(day => (
                <td key={day} className={`heat-${heatLevel(grid[day][index])}`} title={`${label(day)} ${part.label.toLowerCase()}: ${hoursText(grid[day][index])}`}>
                  <span className="visually-hidden">{hoursText(grid[day][index])}</span>
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <details className="shared-time-details">
        <summary>Shared time details</summary>
        {sharedParts.length ? <>
          <ul>{sharedParts.map(part => <li key={part}>{part}</li>)}</ul>
          <p className="field-hint">All times are in SGT. Other day parts have no shared time.</p>
        </> : <p className="field-hint">No shared free time.</p>}
      </details>
    </div>
  )
}
