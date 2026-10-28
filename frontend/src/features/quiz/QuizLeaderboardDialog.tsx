import { useEffect } from 'react'
import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import { useResource } from '../../shared/api/useResource'
import { formatDuration, getLeaderboard } from './api'
import type { Quiz } from './api'

/** How often an open quiz's leaderboard refreshes; the same pace as the study room's own polling. */
const POLL_INTERVAL_MS = 2000

/** The group's ranking for one quiz, refreshing while the quiz is open so new submissions appear. */
export default function QuizLeaderboardDialog({ quiz, onClose }: { quiz: Quiz; onClose: () => void }) {
  const board = useResource(`quiz-leaderboard-${quiz.id}`, signal => getLeaderboard(quiz.id, signal))
  const open = board.data ? board.data.status === 'OPEN' : quiz.status === 'OPEN'
  const { reload } = board

  useEffect(() => {
    if (!open) return
    const timer = window.setInterval(() => { if (!document.hidden) reload() }, POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [open, reload])

  const data = board.data
  const taking = data ? data.submittedCount + data.notSubmittedCount : 0
  return <Dialog title={`Leaderboard: ${quiz.title}`} onClose={onClose}>
    <StatePanel loading={board.loading && !data} error={board.error} onRetry={() => reload()}
      empty={!!data && !data.entries.length} emptyTitle="No one has submitted yet"
      emptyMessage={open ? 'Scores appear here as members submit.' : 'This quiz closed before anyone submitted.'} />
    {data && <>
      <p className="quiz-leaderboard-summary" aria-live="polite">
        {data.submittedCount} of {taking} submitted · {data.status === 'OPEN' ? 'updating live' : 'final results'}
      </p>
      {!!data.entries.length && <div className="table-scroll">
        <table className="account-table quiz-leaderboard">
          <thead><tr><th scope="col">Rank</th><th scope="col">Member</th><th scope="col">Score</th><th scope="col">Time</th></tr></thead>
          <tbody>
            {data.entries.map(entry => <tr key={entry.studentId} className={entry.me ? 'is-me' : undefined}>
              <td>#{entry.rank}</td>
              <td>{entry.displayName}{entry.me && <span className="quiz-me"> (you)</span>}</td>
              <td>{entry.score} / {entry.total}</td>
              <td>{formatDuration(entry.timeTakenSeconds)}</td>
            </tr>)}
          </tbody>
        </table>
      </div>}
      <p className="field-hint">Ranked by score, then time taken, then who submitted first.</p>
    </>}
    <div className="actions dialog-actions"><Button onClick={onClose}>Close leaderboard</Button></div>
  </Dialog>
}
