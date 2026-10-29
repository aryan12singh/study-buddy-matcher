import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import { errorMessage } from '../../shared/api/client'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import type { Course } from '../../shared/api/types'
import { getSessions } from '../sessions/api'
import { COURSE_CODE, findCourse } from './findCourse'
import {
  ACCEPTED_EXTENSIONS, checkFiles, isAiUnavailable, MAX_FILES, MAX_FOCUS_CHARACTERS, MAX_NOTES_CHARACTERS,
  sessionLabel, startGroupQuiz, startPersonalQuiz, upcomingSessions,
} from './api'
import type { Quiz, QuizHome, QuizMaterial } from './api'

/** What the bot is waiting for next. */
type Stage = 'material' | 'course' | 'focus' | 'session' | 'writing' | 'done' | 'failed'
type Message = { id: number; from: 'bot' | 'me'; text: string; files?: string[]; error?: boolean }

const AI_UNAVAILABLE = "I couldn't reach Gemini just now. Give it a minute and say \"try again\", or quiz each other from your notes."
const SKIP_WORDS = ['skip', 'no', 'nope', 'none', 'nothing', 'everything', 'all', 'cover everything', 'anything']
const NO_SESSION_WORDS = ['skip', 'no', 'nope', 'none', 'not for a session']
/**
 * The quiz bot as a chat. The student types or pastes into the message box and attaches files with
 * the paperclip; the bot asks for whatever it still needs (notes, a course for a personal quiz, an
 * optional focus and, in a group with upcoming sessions, which session it is for) and then has
 * Gemini write the quiz. A message with files can say everything at
 * once, such as "IS442, focus on interfaces", and the bot skips the questions it no longer needs.
 * Gemini only writes the questions: understanding the replies is plain code, so the chat never
 * spends the student's AI allowance.
 */
export default function QuizBotChat({ home, onStarted, onTake }: {
  home: QuizHome; onStarted: (quiz: Quiz) => void; onTake: (quiz: Quiz) => void
}) {
  const personal = home.scope === 'PERSONAL'
  const courses = useResource('quiz-bot-courses', signal => getCourses(signal), personal, false)
  const groupId = home.scope === 'GROUP' ? home.groupId : 0
  // A failed or slow load only skips the optional session question; it never blocks a quiz.
  const sessions = useResource(`quiz-bot-sessions-${groupId}`, signal => getSessions({ kind: 'GROUP', id: groupId }, signal), !personal, false)
  const upcoming = upcomingSessions(sessions.data ?? [])
  const nextId = useRef(1)
  const log = useRef<HTMLDivElement>(null)
  const fileInput = useRef<HTMLInputElement>(null)
  const composer = useRef<HTMLTextAreaElement>(null)
  const [messages, setMessages] = useState<Message[]>(() => [{
    id: 0, from: 'bot', text: `Hi! I'm Quiz Buddy. Send me your lecture slides or notes and I'll write a quiz from them`
      + (personal ? ' for you to practise on.' : ' for everyone in the group to take.')
      + ' Attach files with the paperclip, or paste your notes into the message box.',
  }])
  const [stage, setStage] = useState<Stage>('material')
  const [draft, setDraft] = useState('')
  const [attached, setAttached] = useState<File[]>([])
  const [material, setMaterial] = useState<QuizMaterial>()
  const [course, setCourse] = useState<Course>()
  const [focus, setFocus] = useState('')
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [quiz, setQuiz] = useState<Quiz>()

  // Keep the newest message in view, like any chat.
  useEffect(() => { if (log.current) log.current.scrollTop = log.current.scrollHeight }, [messages, stage])

  function add(from: Message['from'], text: string, extra: Partial<Message> = {}) {
    const message = { id: nextId.current++, from, text, ...extra }
    setMessages(previous => [...previous, message])
  }

  function attach(list: FileList | null) {
    const files = [...attached, ...Array.from(list ?? [])]
    if (fileInput.current) fileInput.current.value = ''
    const problem = checkFiles(files)
    if (problem) { add('bot', problem, { error: true }); return }
    setAttached(files)
    composer.current?.focus()
  }

  async function write(chosenMaterial: QuizMaterial, chosenCourse: Course | undefined, chosenFocus: string, chosenSession: number | null = null) {
    setStage('writing')
    try {
      const written = home.scope === 'GROUP'
        ? await startGroupQuiz(home.groupId, chosenMaterial, chosenFocus, chosenSession)
        : await startPersonalQuiz(chosenCourse!.id, chosenMaterial, chosenFocus)
      const forSession = upcoming.find(session => session.id === written.sessionId)
      setQuiz(written)
      setStage('done')
      add('bot', `Your quiz on “${written.title}” is ready: ${written.questionCount} questions`
        + (forSession ? `, for ${forSession.title}.` : '.')
        + (written.scope === 'GROUP' ? ' Everyone in the group can take it now; the leaderboard is below.' : ' Good luck!')
        + ' Send me more notes any time for another one.')
      onStarted(written)
    } catch (error) {
      setStage('failed')
      add('bot', isAiUnavailable(error) ? AI_UNAVAILABLE : errorMessage(error), { error: true })
    }
  }

  /** Asks for the next thing still missing, or writes the quiz once nothing is. */
  function continueWith(chosenMaterial: QuizMaterial, chosenCourse: Course | undefined, chosenFocus: string | undefined) {
    if (personal && !chosenCourse) {
      setStage('course')
      add('bot', 'Got them! Which course are these notes for? Type its code, like IS442, or pick one below.')
    } else if (chosenFocus === undefined) {
      setStage('focus')
      add('bot', 'Anything I should focus on? Tell me a topic, or say "skip" and I\'ll cover everything.')
    } else if (!personal && upcoming.length) {
      setStage('session')
      add('bot', 'Is this quiz for one of the group\'s upcoming sessions? Pick one below, or say "no".')
    } else {
      void write(chosenMaterial, chosenCourse, chosenFocus)
    }
  }

  /** A new set of notes: files with an optional message, or a pasted message on its own. */
  function receiveMaterial(text: string, files: File[]) {
    let chosen: QuizMaterial
    let chosenFocus: string | undefined
    if (files.length) {
      chosen = { kind: 'files', files }
      // With files attached, the words are instructions: a course and perhaps a focus.
      const named = personal ? findCourse(text, courses.data ?? []) : null
      const rest = (named ? text.replace(COURSE_CODE, '') : text).replace(/^[\s,.;:-]*(focus on|about|on)?\s*/i, '').trim()
      if (named) setCourse(named)
      if (rest) chosenFocus = rest.slice(0, MAX_FOCUS_CHARACTERS)
      setMaterial(chosen)
      setFocus(chosenFocus ?? '')
      continueWith(chosen, named ?? undefined, chosenFocus)
      return
    }
    if (text.length > MAX_NOTES_CHARACTERS) {
      add('bot', `That's more than ${MAX_NOTES_CHARACTERS} characters. Save it as a .txt file and attach it instead.`, { error: true })
      return
    }
    if (text.length < 20) {
      add('bot', 'That looks a bit short to quiz on. Attach your slides with the paperclip, or paste a few paragraphs of notes.')
      return
    }
    chosen = { kind: 'notes', text }
    setMaterial(chosen)
    continueWith(chosen, undefined, undefined)
  }

  function send(text: string, files: File[] = attached) {
    const message = text.trim()
    if (!message && !files.length) return
    add('me', message, files.length ? { files: files.map(file => file.name) } : {})
    setDraft('')
    setAttached([])
    if (stage === 'material' || stage === 'done') {
      setCourse(undefined); setFocus(''); setSessionId(null); setQuiz(undefined)
      receiveMaterial(message, files)
    } else if (stage === 'course') {
      const named = findCourse(message, courses.data ?? [])
      if (!named) { add('bot', `I couldn't find a course called “${message}”. Try its code, like IS442.`); return }
      setCourse(named)
      continueWith(material!, named, undefined)
    } else if (stage === 'focus') {
      const chosenFocus = SKIP_WORDS.includes(message.toLowerCase()) ? '' : message
      if (chosenFocus.length > MAX_FOCUS_CHARACTERS) { add('bot', `Can you say that in under ${MAX_FOCUS_CHARACTERS} characters?`); return }
      setFocus(chosenFocus)
      continueWith(material!, course, chosenFocus)
    } else if (stage === 'session') {
      const answer = message.toLowerCase()
      const chosen = NO_SESSION_WORDS.includes(answer) ? null
        : upcoming.find(session => sessionLabel(session).toLowerCase() === answer)
          ?? upcoming.find(session => session.title.toLowerCase().includes(answer))
      if (chosen === undefined) { add('bot', `I couldn't find an upcoming session called “${message}”. Pick one below, or say "no".`); return }
      setSessionId(chosen?.id ?? null)
      void write(material!, course, focus, chosen?.id ?? null)
    } else if (stage === 'failed') {
      if (/try again|retry|again/i.test(message) && material) { void write(material, course, focus, sessionId); return }
      receiveMaterial(message, files)
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    send(draft)
  }

  function sendOnEnter(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      send(draft)
    }
  }

  const writing = stage === 'writing'
  const courseChoices = (courses.data ?? []).filter(option => !draft.trim()
    || `${option.code} ${option.name}`.toLowerCase().includes(draft.trim().toLowerCase())).slice(0, 6)
  return <div className="quiz-bot-chat">
    <div ref={log} className="quiz-bot-log" role="log" aria-live="polite" aria-label="Conversation with Quiz Buddy">
      {messages.map(message => message.from === 'bot'
        ? <div key={message.id} className={`quiz-bot-message is-bot${message.error ? ' is-error' : ''}`}>
            <span className="quiz-bot-avatar" aria-hidden="true"><PixelIcon kind="robot" /></span>
            <p><span className="visually-hidden">Quiz Buddy: </span>{message.text}</p>
          </div>
        : <div key={message.id} className="quiz-bot-message is-me">
            <p><span className="visually-hidden">You: </span>
              {message.files?.map(name => <span key={name} className="quiz-bot-file">📎 {name}</span>)}
              {message.text && <span className="quiz-bot-text">{message.text}</span>}
            </p>
          </div>)}
      {writing && <div className="quiz-bot-message is-bot">
        <span className="quiz-bot-avatar" aria-hidden="true"><PixelIcon kind="robot" /></span>
        <p role="status"><span className="quiz-bot-typing">Reading your notes and writing questions. This can take up to a minute</span></p>
      </div>}
    </div>

    <div className="quiz-bot-quick-replies" aria-label="Suggested replies">
      {stage === 'course' && courseChoices.map(option =>
        <Button key={option.id} onClick={() => send(`${option.code} ${option.name}`, [])}>{option.code}</Button>)}
      {stage === 'focus' && <Button onClick={() => send('Cover everything', [])}>Cover everything</Button>}
      {stage === 'session' && <>
        {upcoming.map(session => <Button key={session.id} onClick={() => send(sessionLabel(session), [])}>{sessionLabel(session)}</Button>)}
        <Button onClick={() => send('Not for a session', [])}>Not for a session</Button>
      </>}
      {stage === 'done' && quiz && <Button variant="primary" onClick={() => onTake(quiz)}>{personal ? 'Start quiz' : 'Take it now'}</Button>}
      {stage === 'failed' && material && <Button variant="primary" onClick={() => send('Try again', [])}>Try again</Button>}
    </div>

    <form className="quiz-bot-composer" onSubmit={submit}>
      {!!attached.length && <ul className="quiz-bot-attachments" aria-label="Attached files">
        {attached.map(file => <li key={file.name}>📎 {file.name}
          <button type="button" aria-label={`Remove ${file.name}`} onClick={() => setAttached(attached.filter(other => other !== file))}>×</button>
        </li>)}
      </ul>}
      <div className="quiz-bot-composer-row">
        <input ref={fileInput} id="quiz-bot-files" type="file" multiple hidden accept={ACCEPTED_EXTENSIONS.join(',')}
          onChange={event => attach(event.target.files)} />
        <button type="button" className="quiz-bot-attach" aria-label="Attach notes" title="Attach PDF, .txt or .md notes"
          disabled={writing || stage === 'course'} onClick={() => fileInput.current?.click()}>📎</button>
        <label htmlFor="quiz-bot-message" className="visually-hidden">Message Quiz Buddy</label>
        <textarea ref={composer} id="quiz-bot-message" rows={2} value={draft} disabled={writing} maxLength={MAX_NOTES_CHARACTERS}
          placeholder={placeholderFor(stage)} onChange={event => setDraft(event.target.value)} onKeyDown={sendOnEnter} />
        <Button type="submit" variant="primary" disabled={writing || (!draft.trim() && !attached.length)}>Send</Button>
      </div>
      <p className="field-hint">PDF, .txt or .md, up to {MAX_FILES} files and 10MB. Enter sends; Shift+Enter adds a line. Your notes go to Google Gemini to write the questions and are not saved.</p>
    </form>
  </div>
}

function placeholderFor(stage: Stage) {
  switch (stage) {
    case 'course': return 'Type a course code, like IS442'
    case 'focus': return 'A topic to focus on, or "skip"'
    case 'session': return 'A session name, or "no"'
    case 'writing': return 'Writing your quiz…'
    default: return 'Paste notes, or attach files and add a message'
  }
}
