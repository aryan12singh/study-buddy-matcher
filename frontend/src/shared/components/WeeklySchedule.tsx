import type { WeeklySlot } from '../api/types'
import { label, WEEK_DAYS } from '../api/types'

export default function WeeklySchedule({ slots }: { slots: WeeklySlot[] }) {
  return (
    <div className="weekly-schedule">
      <p className="field-hint">Weekly times in Asia/Singapore (SGT)</p>
      {!slots.length
        ? (
          <p className="muted">No weekly availability saved.</p>
        ) : (
          <dl className="schedule-grid">
            {WEEK_DAYS.map(day => {
              const daySlots = slots.filter(slot => slot.dayOfWeek === day)
                .sort((left, right) => left.startTime.localeCompare(right.startTime) || left.endTime.localeCompare(right.endTime))
              return (
                <div key={day}>
                  <dt>{label(day)}</dt>
                  <dd>
                    {daySlots.length ?
                      daySlots.map((slot, index) => (
                        <span key={index}>{slot.startTime.slice(0, 5) || '--:--'}–{slot.endTime.slice(0, 5) || '--:--'}</span>
                      )) : '—'}
                  </dd>
                </div>
              )
            })}
          </dl>
        )}
    </div>
  )
}
