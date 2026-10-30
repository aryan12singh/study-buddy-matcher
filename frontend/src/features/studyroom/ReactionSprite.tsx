import { PixelRects } from '../achievements/pixelArt'
import type { RoomReactionType } from './api'
import { REACTIONS } from './reactionSprites'

export default function ReactionSprite({ type }: { type: RoomReactionType }) {
  const reaction = REACTIONS[type]
  return (
    <svg viewBox="0 0 12 12" aria-hidden="true" focusable="false" shapeRendering="crispEdges">
      <PixelRects rows={reaction.rows} palette={reaction.palette} />
    </svg>
  )
}
