import type { StudyPlan, StudyPlanEntry } from './api'
import { formatMinutes, formatPlanDate } from './planDates'

/**
 * A plan's entries grouped into its weeks (or a session's meetings), each topic with its planned
 * time, tasks and a checkbox. Ticking is only offered to someone who may manage the plan.
 */
export default function RoadmapView({ plan, onToggle, savingEntryId }: {
  plan: StudyPlan
  onToggle?: (entry: StudyPlanEntry, completed: boolean) => void
  savingEntryId?: number
}) {
  const blockName = plan.scope === 'SESSION' ? 'Meeting' : 'Week'
  const blocks = [...new Set(plan.entries.map(entry => entry.sequence))].sort((left, right) => left - right)
    .map(sequence => ({ sequence, entries: plan.entries.filter(entry => entry.sequence === sequence) }))
  return <ol className="roadmap" aria-label="Roadmap">
    {blocks.map(block => {
      const planned = block.entries.reduce((total, entry) => total + entry.plannedMinutes, 0)
      const done = block.entries.every(entry => entry.completed)
      return <li key={block.sequence} className={`roadmap-block${done ? ' is-done' : ''}`}>
        <h3>{blockName} {block.sequence}
          <span className="row-meta"> · {plan.scope === 'SESSION' ? 'on' : 'from'} {formatPlanDate(block.entries[0].scheduledDate)}
            {' '}· {formatMinutes(planned)} of {formatMinutes(plan.minutesPerBlock)} planned</span>
        </h3>
        <ul className="roadmap-entries">
          {block.entries.map(entry => <li key={entry.id} className={entry.completed ? 'is-done' : undefined}>
            <label className="roadmap-entry">
              <input type="checkbox" checked={entry.completed} disabled={!onToggle || savingEntryId !== undefined}
                onChange={event => onToggle?.(entry, event.target.checked)} />
              <span className="roadmap-title">{entry.title}</span>
              <span className="row-meta">{formatMinutes(entry.plannedMinutes)}</span>
            </label>
            {savingEntryId === entry.id && <span className="row-meta" role="status">Saving…</span>}
            {!!entry.tasks.length && <ul className="roadmap-tasks" aria-label={`Tasks for ${entry.title}`}>
              {entry.tasks.map(task => <li key={task}>{task}</li>)}
            </ul>}
          </li>)}
        </ul>
      </li>
    })}
  </ol>
}
