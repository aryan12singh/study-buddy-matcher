export default function CapacityMeter({ members, capacity, active = true }: { members: number; capacity: number; active?: boolean }) {
  const proportion = capacity > 0 ? Math.min(1, Math.max(0, members / capacity)) : 0
  return <div className={`capacity-meter${active ? '' : ' capacity-closed'}`}>
    <div className="capacity-caption">
      <span>{members}/{capacity} members</span>
      <span>{!active ? 'Closed group' : members >= capacity ? 'Full' : `${capacity - members} ${capacity - members === 1 ? 'place' : 'places'} available`}</span>
    </div>
    <div className="capacity-track" role="meter" aria-label="Group capacity" aria-valuemin={0}
      aria-valuemax={Math.max(capacity, 1)} aria-valuenow={Math.min(Math.max(members, 0), Math.max(capacity, 1))}
      aria-valuetext={`${members} of ${capacity} members${active ? '' : ', group closed'}`}>
      <span style={{ width: `${proportion * 100}%` }} />
    </div>
  </div>
}
