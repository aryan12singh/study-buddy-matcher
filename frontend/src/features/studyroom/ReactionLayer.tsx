import { useEffect, useRef, useState } from 'react'
import type { RoomReaction } from './api'
import ReactionSprite from './ReactionSprite'
import { REACTIONS } from './reactionSprites'

const FLOAT_MILLIS = 4000
/** Reactions already this old when first seen (such as on opening the room) are not replayed. */
const REPLAY_LIMIT_MILLIS = 3000

/**
 * Floats each new reaction up over the room scene once. Snapshots repeat a reaction for a few
 * seconds, so ids already shown are remembered and skipped.
 */
export default function ReactionLayer({ reactions, serverTime }: { reactions: RoomReaction[]; serverTime: string }) {
  const seen = useRef(new Set<number>())
  const timers = useRef(new Set<number>())
  const [floating, setFloating] = useState<RoomReaction[]>([])

  // Timers belong to the reactions, not to one snapshot, so they are cleared only on unmount.
  useEffect(() => {
    const pending = timers.current
    return () => { pending.forEach(timer => window.clearTimeout(timer)); pending.clear() }
  }, [])

  useEffect(() => {
    const now = Date.parse(serverTime)
    const fresh = reactions.filter(reaction => !seen.current.has(reaction.id))
    const shown = fresh.filter(reaction => !(now - Date.parse(reaction.sentAt) > REPLAY_LIMIT_MILLIS))
    fresh.filter(reaction => !shown.includes(reaction)).forEach(reaction => seen.current.add(reaction.id))
    if (!shown.length) return
    const ids = new Set(shown.map(reaction => reaction.id))
    const later = (action: () => void, delay: number) => {
      const timer = window.setTimeout(() => { timers.current.delete(timer); action() }, delay)
      timers.current.add(timer)
    }
    // Marked as seen only once on screen, so a cancelled timer leaves it to be shown again.
    later(() => {
      const unseen = shown.filter(reaction => !seen.current.has(reaction.id))
      unseen.forEach(reaction => seen.current.add(reaction.id))
      setFloating(current => [...current, ...unseen])
    }, 0)
    later(() => setFloating(current => current.filter(reaction => !ids.has(reaction.id))), FLOAT_MILLIS)
  }, [reactions, serverTime])

  return (
    <div className="reaction-layer" aria-live="polite">
      {floating.map(reaction => (
        <span key={reaction.id} className="reaction-float" style={{ left: `${8 + (reaction.id * 37) % 80}%` }}>
          <ReactionSprite type={reaction.type} />
          <span className="reaction-name">{reaction.senderName}</span>
          <span className="visually-hidden">sent {REACTIONS[reaction.type].label.toLowerCase()}</span>
        </span>
      ))}
    </div>
  )
}
