import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import { errorMessage } from '../../shared/api/client'
import { useAction } from '../../shared/api/useAction'
import { aiUnavailableMessage, isAiUnavailable } from '../quiz/api'
import { askAssistant, MAX_MESSAGE_CHARACTERS } from './api'
import type { AssistantReply } from './api'
import AssistantReplyView from './AssistantReplyView'

type Message = { id: number; from: 'me' | 'assistant'; text: string; reply?: AssistantReply; error?: boolean }

const GREETING = "Hi! I'm Pixel, your study assistant. I can tell you what's waiting for you, help you find your way around, find you a study buddy, find a time your group can meet, quiz you on your notes, or plan your revision."
const SUGGESTIONS = ['What do I need to do?', 'Find me an IS442 buddy who prefers small groups', 'When can my group meet for 2 hours?',
  'How do I change my availability?', 'Quiz me on my notes', 'What are we covering next session?']
const UNAVAILABLE = aiUnavailableMessage('You can still use the navigation menu to find your way in the meantime.')

/**
 * Pixel, the study assistant's chat panel. Each message is answered on its own: the AI reads it, and the app
 * answers from its own data. The conversation stays while the student moves between pages.
 */
export default function AssistantPanel({ onClose, onOpenQuizBuddy }: { onClose: () => void; onOpenQuizBuddy: () => void }) {
  const nextId = useRef(1)
  const log = useRef<HTMLDivElement>(null)
  const composer = useRef<HTMLTextAreaElement>(null)
  const [messages, setMessages] = useState<Message[]>([{ id: 0, from: 'assistant', text: GREETING }])
  const [draft, setDraft] = useState('')
  // Pixel only reads the student's data, so a reply leaves the other views as they are.
  const { pending, run } = useAction({ refresh: false })

  useEffect(() => { composer.current?.focus() }, [])
  useEffect(() => { if (log.current) log.current.scrollTop = log.current.scrollHeight }, [messages, pending])

  function add(message: Omit<Message, 'id'>) {
    const withId = { ...message, id: nextId.current++ }
    setMessages(previous => [...previous, withId])
  }

  async function send(text: string) {
    const message = text.trim()
    if (!message || pending) return
    if (message.length > MAX_MESSAGE_CHARACTERS) {
      add({ from: 'assistant', text: `Can you say that in under ${MAX_MESSAGE_CHARACTERS} characters?`, error: true })
      return
    }
    add({ from: 'me', text: message })
    setDraft('')
    const answered = await run(() => askAssistant(message))
    // Failures are answered in the conversation rather than in a notice.
    if (answered.ok) add({ from: 'assistant', text: answered.value.text, reply: answered.value })
    else add({ from: 'assistant', text: isAiUnavailable(answered.failure) ? UNAVAILABLE : errorMessage(answered.failure), error: true })
    composer.current?.focus()
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    void send(draft)
  }

  function sendOnEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      void send(draft)
    }
  }

  return <section className="assistant-panel" role="dialog" aria-modal="false" aria-labelledby="assistant-title"
    onKeyDown={event => { if (event.key === 'Escape') onClose() }}>
    <header className="assistant-header">
      <span className="assistant-header-icon" aria-hidden="true"><PixelIcon kind="robot" /></span>
      <div>
        <h2 id="assistant-title">Pixel</h2>
        <p>Your AI study assistant · Ask me anything</p>
      </div>
      <button type="button" className="assistant-close" aria-label="Close Pixel" onClick={onClose}>×</button>
    </header>

    <div ref={log} className="assistant-log" role="log" aria-live="polite" aria-label="Conversation with Pixel">
      {messages.map(message => message.from === 'assistant'
        ? <div key={message.id} className={`quiz-bot-message is-bot${message.error ? ' is-error' : ''}`}>
            <span className="quiz-bot-avatar" aria-hidden="true"><PixelIcon kind="robot" /></span>
            <div className="assistant-bubble">
              <p><span className="visually-hidden">Pixel: </span>{message.text}</p>
              {message.reply && <AssistantReplyView reply={message.reply} onOpenQuizBuddy={onOpenQuizBuddy} onNavigate={onClose} />}
            </div>
          </div>
        : <div key={message.id} className="quiz-bot-message is-me">
            <p><span className="visually-hidden">You: </span>{message.text}</p>
          </div>)}
      {pending && <div className="quiz-bot-message is-bot">
        <span className="quiz-bot-avatar" aria-hidden="true"><PixelIcon kind="robot" /></span>
        <p role="status"><span className="quiz-bot-typing">Thinking</span></p>
      </div>}
    </div>

    {messages.length === 1 && <div className="assistant-suggestions" aria-label="Try asking">
      {SUGGESTIONS.map(suggestion => <Button key={suggestion} onClick={() => void send(suggestion)}>{suggestion}</Button>)}
    </div>}

    <form className="assistant-composer" onSubmit={submit}>
      <label htmlFor="assistant-message" className="visually-hidden">Message Pixel</label>
      <textarea ref={composer} id="assistant-message" rows={1} value={draft} maxLength={MAX_MESSAGE_CHARACTERS} disabled={pending}
        placeholder="Message Pixel…" onChange={event => setDraft(event.target.value)} onKeyDown={sendOnEnter} />
      <Button type="submit" variant="primary" disabled={pending || !draft.trim()}>Send</Button>
    </form>
  </section>
}
