import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import type { Problem } from '../../shared/api/client'
import type { MatchSearch } from '../matching/api'
import { fetchMatchExplanation, type MatchExplanation } from './api'

type PanelState = { status: 'idle' } | { status: 'loading' } | { status: 'error' } | { status: 'stale' }
  | { status: 'done'; explanation: MatchExplanation }
type PanelProps = { candidateId: number; evaluationId: string; search: MatchSearch; onRefreshMatches: () => void }

const AI_LABEL = 'Written by Gemini; may be inaccurate. Scores unchanged.'
const STANDARD_LABEL = "Standard explanation; AI wording wasn't available."

/** A new evaluation starts with no completed answer and cancels the old evaluation's request. */
export default function MatchExplanationPanel(props: PanelProps) {
  return <EvaluationExplanation key={`${props.candidateId}:${props.evaluationId}`} {...props} />
}

/** Asks for a written explanation of one immutable card evaluation only on demand. */
function EvaluationExplanation({ candidateId, evaluationId, search, onRefreshMatches }: PanelProps) {
  const [state, setState] = useState<PanelState>({ status: 'idle' })
  const controller = useRef<AbortController | null>(null)
  useEffect(() => () => controller.current?.abort(), [])

  async function load() {
    controller.current?.abort()
    controller.current = new AbortController()
    const signal = controller.current.signal
    setState({ status: 'loading' })
    try {
      const explanation = await fetchMatchExplanation(candidateId, evaluationId, search, signal)
      if (signal.aborted) return
      if (explanation.candidateId !== candidateId || explanation.evaluationId !== evaluationId) {
        setState({ status: 'stale' })
      } else {
        setState({ status: 'done', explanation })
      }
    } catch (error) {
      if (signal.aborted || axios.isCancel(error)) return
      const stale = axios.isAxiosError<Problem>(error) && error.response?.status === 409
        && error.response.data?.code === 'STALE_MATCH_EVALUATION'
      setState({ status: stale ? 'stale' : 'error' })
    }
  }

  // One live region for every state, so screen readers announce the answer when it arrives.
  return (
    <div className="match-explanation-panel" aria-live="polite" aria-busy={state.status === 'loading'}>
      <PanelContent state={state} onLoad={load} onRefreshMatches={onRefreshMatches} />
    </div>
  )
}

type PanelContentProps = { state: PanelState; onLoad: () => void; onRefreshMatches: () => void }

/** What the panel shows in each state; the live region around it stays mounted. */
function PanelContent({ state, onLoad: load, onRefreshMatches }: PanelContentProps) {
  if (state.status === 'done' && state.explanation.source === 'AI') {
    return (
      <div className="match-ai-explanation">
        <p><Badge tone="good">AI</Badge> {state.explanation.text}</p>
        <p className="field-hint">{AI_LABEL}</p>
      </div>
    )
  }
  if (state.status === 'done') {
    return (
      <div className="match-ai-explanation">
        <p>{state.explanation.text}</p>
        <p className="field-hint">{STANDARD_LABEL} <Button onClick={load}>Try AI again</Button></p>
      </div>
    )
  }
  if (state.status === 'stale') {
    return <p className="field-hint" role="alert">This match changed. <Button onClick={onRefreshMatches}>Refresh matches</Button> before requesting an explanation.</p>
  }
  if (state.status === 'error') {
    return <p className="field-hint" role="alert">Couldn't load an explanation. <Button onClick={load}>Try again</Button></p>
  }
  const loading = state.status === 'loading'
  return <Button onClick={load} disabled={loading}>{loading ? 'Writing an explanation…' : 'Explain this match'}</Button>
}
