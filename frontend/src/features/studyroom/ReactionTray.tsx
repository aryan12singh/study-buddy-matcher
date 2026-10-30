import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { errorMessage } from '../../shared/api/client'
import { useAction } from '../../shared/api/useAction'
import { sendReaction } from './api'
import type { RoomReaction, RoomReactionType } from './api'
import ReactionSprite from './ReactionSprite'
import { REACTIONS, REACTION_TYPES } from './reactionSprites'

/**
 * How long the buttons rest after the server refuses a reaction as too quick. It matches the
 * default app.study-room.reactions.burst-window; a Retry-After header from the server takes precedence.
 */
export const REACTION_COOLDOWN_MS = 10000

/** The wait the server asked for, if it said, otherwise the default burst window. */
function cooldownAfter(failure: unknown) {
  const seconds = axios.isAxiosError(failure) ? Number(failure.response?.headers?.['retry-after']) : NaN
  return Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : REACTION_COOLDOWN_MS
}

/** A row of reaction buttons. The sender sees their reaction at once; others see it on their next room refresh. */
export default function ReactionTray({ groupId, joined, onSent }: { groupId: number; joined: boolean; onSent: (reaction: RoomReaction) => void }) {
  // Reactions only float over this room, so no other view needs refreshing.
  const action = useAction({ refresh: false })
  // The server's refusal, shown politely while the buttons rest; undefined when they are usable.
  const [cooldown, setCooldown] = useState<string>()
  const cooldownMillis = useRef(REACTION_COOLDOWN_MS)

  useEffect(() => {
    if (!cooldown) return
    const timer = window.setTimeout(() => setCooldown(undefined), cooldownMillis.current)
    return () => window.clearTimeout(timer)
  }, [cooldown])

  async function react(type: RoomReactionType) {
    const sent = await action.run(() => sendReaction(groupId, type))
    if (sent.ok) onSent(sent.value)
    else if (axios.isAxiosError(sent.failure) && sent.failure.response?.status === 409) {
      // Rest the buttons for the rate limit's window instead of letting every click be refused.
      cooldownMillis.current = cooldownAfter(sent.failure)
      action.clear()
      setCooldown(errorMessage(sent.failure))
    }
  }

  return (
    <div className="reaction-tray" role="group" aria-label="Reactions">
      <div className="reaction-buttons">
        {REACTION_TYPES.map(type => (
          <button key={type} type="button" className="reaction-button" disabled={!joined || action.pending || Boolean(cooldown)}
            aria-label={`Send ${REACTIONS[type].label.toLowerCase()} reaction`} title={REACTIONS[type].label}
            onClick={() => void react(type)}>
            <ReactionSprite type={type} />
          </button>
        ))}
      </div>
      {!joined && <p className="reaction-hint">Join the room to send reactions.</p>}
      {/* Kept in the page while empty so screen readers announce the pause when it starts. */}
      <p className={cooldown ? 'reaction-hint' : 'visually-hidden'} role="status">{cooldown}</p>
      {action.error && <p className="reaction-error" role="alert">{action.error}</p>}
    </div>
  )
}
