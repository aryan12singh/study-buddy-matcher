import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import { errorMessage } from '../../shared/api/client'
import { AI_QUIZ_LABEL, beginAttempt, formatDuration, submitAnswers } from './api'
import type { AttemptResult, Quiz, QuizQuestions } from './api'

/**
 * Takes one quiz: asks the server for the questions (which records the start time), then marks the
 * answers. Every question must be answered before submitting, as the server also requires.
 */
export default function TakeQuizDialog({ quiz, onClose, onSubmitted, onReview }: {
  quiz: Quiz; onClose: () => void; onSubmitted: () => void; onReview: () => void
}) {
  const [questions, setQuestions] = useState<QuizQuestions>()
  const [loadError, setLoadError] = useState<string>()
  const [chosen, setChosen] = useState<Record<number, number>>({})
  const [missing, setMissing] = useState(false)
  const [result, setResult] = useState<AttemptResult>()
  const [error, setError] = useState<string>()
  const [pending, setPending] = useState(false)

  useEffect(() => {
    let current = true
    beginAttempt(quiz.id)
      .then(loaded => { if (current) setQuestions(loaded) })
      .catch(failure => { if (current) setLoadError(errorMessage(failure)) })
    return () => { current = false }
  }, [quiz.id])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!questions) return
    const unanswered = questions.questions.some(question => chosen[question.id] === undefined)
    setMissing(unanswered)
    if (unanswered) return
    setPending(true)
    setError(undefined)
    try {
      setResult(await submitAnswers(quiz.id, questions.questions.map(question => ({ questionId: question.id, optionIndex: chosen[question.id] }))))
      onSubmitted()
    } catch (failure) {
      setError(errorMessage(failure))
    } finally {
      setPending(false)
    }
  }

  if (result) {
    return <Dialog title="Quiz submitted" onClose={onClose}>
      <div className="quiz-result">
        <p className="quiz-result-score"><strong>{result.score}</strong> / {result.total}</p>
        <p>Time taken {formatDuration(result.timeTakenSeconds)}{quiz.scope === 'GROUP' && <>, rank <strong>#{result.rank}</strong> so far</>}.</p>
        {quiz.scope === 'GROUP' && <p className="field-hint">The answers are shown once the quiz closes.</p>}
      </div>
      <div className="actions dialog-actions">
        <Button onClick={onClose}>Back to quizzes</Button>
        {quiz.scope === 'PERSONAL' && <Button variant="primary" onClick={onReview}>See the answers</Button>}
      </div>
    </Dialog>
  }

  return <Dialog title={`Quiz: ${quiz.title}`} busy={pending} onClose={onClose}>
    <StatePanel loading={!questions && !loadError} error={loadError} />
    {questions && <form className="quiz-take-form" onSubmit={event => void submit(event)} noValidate>
      <p className="field-hint">{AI_QUIZ_LABEL} Closes at {new Date(questions.closesAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}.</p>
      <ol className="quiz-questions">
        {questions.questions.map((question, number) => <li key={question.id}>
          <fieldset className={missing && chosen[question.id] === undefined ? 'quiz-question is-missing' : 'quiz-question'}>
            <legend>{number + 1}. {question.prompt}</legend>
            {question.options.map((option, index) => <label key={index} className="quiz-option">
              <input type="radio" name={`question-${question.id}`} checked={chosen[question.id] === index} disabled={pending}
                onChange={() => setChosen(previous => ({ ...previous, [question.id]: index }))} />
              <span>{option}</span>
            </label>)}
          </fieldset>
        </li>)}
      </ol>
      {missing && <p className="field-error" role="alert">Answer every question before submitting. The unanswered ones are outlined.</p>}
      {error && <p className="field-error" role="alert">{error}</p>}
      <div className="actions dialog-actions">
        <Button onClick={onClose} disabled={pending}>Finish later</Button>
        <Button type="submit" variant="primary" disabled={pending}>{pending ? 'Submitting…' : 'Submit answers'}</Button>
      </div>
    </form>}
  </Dialog>
}
