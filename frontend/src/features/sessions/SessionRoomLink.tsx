import { Link } from 'react-router-dom'
import type { StudySession } from './api'
import { nextSessionOccurrence, safeRoomLink } from './sessionPresentation'

export default function SessionRoomLink({ session, now }: { session: StudySession; now: number }) {
  const link = nextSessionOccurrence(session, now) ? safeRoomLink(session) : null
  if (!link) return null
  return link.startsWith('/') ? <Link className="retro-button" to={link}>Open study room</Link>
    : <a className="retro-button" href={link} target="_blank" rel="noopener noreferrer">Open meeting link</a>
}
