import { useState } from 'react'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import { useAction } from '../../shared/api/useAction'
import { useResource } from '../../shared/api/useResource'
import { closeQuiz, listQuizzes } from './api'
import type { Quiz, QuizHome } from './api'
import QuizBotChat from './QuizBotChat'
import TakeQuizDialog from './TakeQuizDialog'
import QuizLeaderboardDialog from './QuizLeaderboardDialog'
import QuizReviewDialog from './QuizReviewDialog'
import './quiz.css'

type OpenDialog = { kind: 'take' | 'leaderboard' | 'review' | 'close'; quiz: Quiz }

/**
 * The quiz bot's window: the chat that writes a new quiz from the student's notes (for anyone who
 * may start one), and the quizzes already written, each with what the student can do next.
 */
export default function QuizBotWindow({ home, canStart, onClose }: { home: QuizHome; canStart: boolean; onClose: () => void }) {
  const key = home.scope === 'GROUP' ? `group-quizzes-${home.groupId}` : 'personal-quizzes'
  const quizzes = useResource(key, signal => listQuizzes(home, signal))
  const [dialog, setDialog] = useState<OpenDialog>()
  const action = useAction()
  const group = home.scope === 'GROUP'

  async function close(quiz: Quiz) {
    const result = await action.run(() => closeQuiz(quiz.id), 'The quiz was closed and the answers are now shown.')
    if (result.ok) setDialog(undefined)
  }

  return <Dialog title="Quiz Buddy" onClose={onClose}>
    <div className="quiz-bot-window">
      {canStart
        ? <QuizBotChat home={home} onStarted={() => quizzes.reload()} onTake={quiz => setDialog({ kind: 'take', quiz })} />
        : <p className="quiz-bot-message is-bot"><span>The group leader or the room's host or co-host can ask me to write a quiz from their notes. When they do, it appears below for you to take.</span></p>}

      <section className="quiz-bot-history" aria-labelledby="quiz-bot-history-heading">
        <h3 id="quiz-bot-history-heading">{group ? 'Group quizzes' : 'Your practice quizzes'}</h3>
        <ActionNotice error={dialog?.kind === 'close' ? undefined : action.error} success={action.success} />
        <StatePanel loading={quizzes.loading && !quizzes.data} error={quizzes.error} onRetry={() => quizzes.reload()}
          empty={!!quizzes.data && !quizzes.data.length} emptyTitle="No quizzes yet"
          emptyMessage={group ? 'Quizzes written for this group appear here.' : 'Quizzes written from your notes appear here.'} />
        {!!quizzes.data?.length && <ul className="quiz-list">
          {quizzes.data.map(quiz => <QuizRow key={quiz.id} quiz={quiz} onOpen={kind => setDialog({ kind, quiz })} />)}
        </ul>}
      </section>
    </div>

    {dialog?.kind === 'take' && <TakeQuizDialog quiz={dialog.quiz} onClose={() => setDialog(undefined)}
      onSubmitted={() => quizzes.reload()} onReview={() => setDialog({ kind: 'review', quiz: dialog.quiz })} />}
    {dialog?.kind === 'leaderboard' && <QuizLeaderboardDialog quiz={dialog.quiz} onClose={() => setDialog(undefined)} />}
    {dialog?.kind === 'review' && <QuizReviewDialog quiz={dialog.quiz} onClose={() => setDialog(undefined)} />}
    {dialog?.kind === 'close' && <ConfirmDialog title="Close this quiz?" confirmLabel="Close quiz for everyone"
      pending={action.pending} error={action.error} onClose={() => setDialog(undefined)} onConfirm={() => void close(dialog.quiz)}>
      <p>Members who have not submitted can no longer take it, and everyone can see the answers.</p>
    </ConfirmDialog>}
  </Dialog>
}

function QuizRow({ quiz, onOpen }: { quiz: Quiz; onOpen: (kind: OpenDialog['kind']) => void }) {
  const open = quiz.status === 'OPEN'
  const group = quiz.scope === 'GROUP'
  const submitted = quiz.myAttempt === 'SUBMITTED'
  return <li className="quiz-row">
    <div className="quiz-row-heading">
      <strong>{quiz.title}</strong>
      <Badge tone={open ? 'good' : 'neutral'}>{open ? 'Open' : 'Closed'}</Badge>
    </div>
    <p className="quiz-row-meta">
      {quiz.course.code} · {quiz.questionCount} questions
      {group && <> · {quiz.submittedCount} submitted</>}
      {submitted && <> · you submitted</>}
      {open && !submitted && <> · closes {new Date(quiz.closesAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</>}
    </p>
    <div className="actions">
      {open && !submitted && <Button variant="primary" onClick={() => onOpen('take')}>{quiz.myAttempt === 'STARTED' ? 'Continue quiz' : 'Take quiz'}</Button>}
      {group && <Button onClick={() => onOpen('leaderboard')}>View leaderboard</Button>}
      {!open && <Button onClick={() => onOpen('review')}>Review answers</Button>}
      {group && open && quiz.canHost && <Button variant="danger" onClick={() => onOpen('close')}>Close quiz</Button>}
    </div>
  </li>
}
