import { useState } from 'react'
import { errorMessage } from '../../shared/api/client'
import { sendReaction } from './api'
import type { RoomReaction, RoomReactionType } from './api'
import ReactionSprite from './ReactionSprite'
import { REACTIONS, REACTION_TYPES } from './reactionSprites'

/** A row of reaction buttons. The sender sees their reaction at once; others see it on their next room refresh. */
export default function ReactionTray({ groupId, joined, onSent }: { groupId: number; joined: boolean; onSent: (reaction: RoomReaction) => void }) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string>()

  async function react(type: RoomReactionType) {
    setPending(true)
    setError(undefined)
    try {
      onSent(await sendReaction(groupId, type))
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="reaction-tray" role="group" aria-label="Reactions">
      <div className="reaction-buttons">
        {REACTION_TYPES.map(type => (
          <button key={type} type="button" className="reaction-button" disabled={!joined || pending}
            aria-label={`Send ${REACTIONS[type].label.toLowerCase()} reaction`} title={REACTIONS[type].label}
            onClick={() => void react(type)}>
            <ReactionSprite type={type} />
          </button>
        ))}
      </div>
      {!joined && <p className="reaction-hint">Join the room to send reactions.</p>}
      {error && <p className="reaction-error" role="alert">{error}</p>}
    </div>
  )
}
