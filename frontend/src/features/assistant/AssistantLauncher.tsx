import { useRef, useState } from 'react'
import PixelIcon from '../../shared/components/PixelIcon'
import QuizBotWindow from '../quiz/QuizBotWindow'
import type { QuizPrefill } from '../quiz/QuizBotChat'
import AssistantPanel from './AssistantPanel'
import '../quiz/quiz.css'
import './assistant.css'

/**
 * The button for Pixel, the study assistant, fixed to the bottom-right corner of every student page. The panel
 * stays mounted while closed, so the conversation survives closing it and moving between pages.
 */
export default function AssistantLauncher() {
  const [open, setOpen] = useState(false)
  const [started, setStarted] = useState(false)
  const [quizBuddy, setQuizBuddy] = useState<QuizPrefill>()
  const fab = useRef<HTMLButtonElement>(null)

  function toggle() {
    setStarted(true)
    setOpen(value => !value)
  }

  /** Focus goes back to the corner button, not to the page body, when the panel hides. */
  function close() {
    setOpen(false)
    fab.current?.focus()
  }

  return <div className="assistant-launcher">
    {started && <div hidden={!open}>
      <AssistantPanel open={open} onClose={close} onOpenQuizBuddy={prefill => {
        setOpen(false)
        // What the student told Pixel ("quiz me on IS442 interfaces") carries into Quiz Buddy.
        setQuizBuddy({ courseId: Number(prefill.courseId) || undefined, topic: prefill.topic || undefined })
      }} />
    </div>}
    <button ref={fab} type="button" className={`assistant-fab${open ? ' is-open' : ''}`} aria-expanded={open}
      aria-label={open ? 'Close Pixel, your study assistant' : 'Open Pixel, your study assistant'} title={open ? undefined : 'Ask Pixel'} onClick={toggle}>
      {open ? <span aria-hidden="true">×</span> : <PixelIcon kind="robot" />}
    </button>
    {quizBuddy && <QuizBotWindow home={{ scope: 'PERSONAL' }} canStart prefill={quizBuddy} onClose={() => setQuizBuddy(undefined)} />}
  </div>
}
