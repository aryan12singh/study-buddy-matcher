import { useState } from 'react'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import type { QuizHome } from './api'
import QuizBotWindow from './QuizBotWindow'
import './quiz.css'

/** The quiz bot's card in a study room's top corner, beside the music player. */
export default function QuizBotLauncher({ home, canStart }: { home: QuizHome; canStart: boolean }) {
  const [open, setOpen] = useState(false)
  const writes = home.scope === 'PERSONAL' || canStart
  return <>
    <div className="quiz-bot-launcher">
      <div className="quiz-bot-launcher-row">
        <span className="quiz-bot-launcher-icon" aria-hidden="true"><PixelIcon kind="robot" /></span>
        <div className="quiz-bot-launcher-text">
          <span className="quiz-bot-launcher-kicker">AI quiz bot</span>
          <strong>{writes ? 'Quiz me on my notes' : 'Group quizzes'}</strong>
        </div>
      </div>
      <Button variant="primary" className="quiz-bot-launcher-open" onClick={() => setOpen(true)}>Open Quiz Buddy</Button>
    </div>
    {open && <QuizBotWindow home={home} canStart={writes} onClose={() => setOpen(false)} />}
  </>
}
