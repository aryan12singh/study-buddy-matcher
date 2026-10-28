import { useState } from 'react'
import PixelIcon from '../../shared/components/PixelIcon'
import QuizBotWindow from '../quiz/QuizBotWindow'
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
  const [quizBuddy, setQuizBuddy] = useState(false)

  function toggle() {
    setStarted(true)
    setOpen(value => !value)
  }

  return <div className="assistant-launcher">
    {started && <div hidden={!open}>
      <AssistantPanel onClose={() => setOpen(false)} onOpenQuizBuddy={() => { setOpen(false); setQuizBuddy(true) }} />
    </div>}
    <button type="button" className={`assistant-fab${open ? ' is-open' : ''}`} aria-expanded={open}
      aria-label={open ? 'Close Pixel, your study assistant' : 'Open Pixel, your study assistant'} title={open ? undefined : 'Ask Pixel'} onClick={toggle}>
      {open ? <span aria-hidden="true">×</span> : <PixelIcon kind="robot" />}
    </button>
    {quizBuddy && <QuizBotWindow home={{ scope: 'PERSONAL' }} canStart onClose={() => setQuizBuddy(false)} />}
  </div>
}
