import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import { useResource } from '../../shared/api/useResource'
import { AI_QUIZ_LABEL, getReview } from './api'
import type { Quiz } from './api'

/** A closed quiz's correct answers and explanations, beside the student's own choices. */
export default function QuizReviewDialog({ quiz, onClose }: { quiz: Quiz; onClose: () => void }) {
  const review = useResource(`quiz-review-${quiz.id}`, signal => getReview(quiz.id, signal), true, false)
  const data = review.data
  return <Dialog title={`Answers: ${quiz.title}`} onClose={onClose}>
    <StatePanel loading={review.loading} error={review.error} onRetry={() => review.reload()} />
    {data && <>
      <p>{data.score === null ? 'You did not submit this quiz.' : <>You scored <strong>{data.score} / {data.total}</strong>.</>}</p>
      <p className="field-hint">{AI_QUIZ_LABEL}</p>
      <ol className="quiz-questions">
        {data.questions.map((question, number) => <li key={question.id} className="quiz-question">
          <p className="quiz-review-prompt">{number + 1}. {question.prompt}</p>
          <ul className="quiz-review-options">
            {question.options.map((option, index) => {
              const correct = index === question.correctOption
              const mine = index === question.chosenOption
              return <li key={index} className={`${correct ? 'is-correct' : ''}${mine && !correct ? ' is-wrong' : ''}`}>
                <span aria-hidden="true">{correct ? '✓' : mine ? '✗' : '·'}</span> {option}
                {correct && <span className="quiz-tag"> correct answer</span>}
                {mine && <span className="quiz-tag"> your answer</span>}
              </li>
            })}
          </ul>
          <p className="quiz-explanation">{question.explanation}</p>
        </li>)}
      </ol>
    </>}
    <div className="actions dialog-actions"><Button onClick={onClose}>Close answers</Button></div>
  </Dialog>
}
