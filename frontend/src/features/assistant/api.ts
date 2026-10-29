import { api } from '../../shared/api/client'
import type { WeeklySlot } from '../../shared/api/types'
import type { MatchResult } from '../matching/api'

export type AssistantIntent = 'FIND_BUDDIES' | 'WHEN_TO_MEET' | 'QUIZ_ME' | 'PLAN_STUDY' | 'PLAN_SESSION' | 'SESSION_TOPICS'
  | 'MY_TO_DO' | 'NAVIGATE' | 'UNKNOWN'
/** Something waiting for the student, with a button to where they can deal with it. */
export type AssistantTodo = { text: string; label: string; link: string }
export type AssistantAction = { kind: 'OPEN_PAGE' | 'OPEN_QUIZ_BUDDY'; label: string; link: string | null; prefill: Record<string, string> }
/** The assistant's answer: the words come from the app's templates, the matches and slots from its own data. */
export type AssistantReply = {
  intent: AssistantIntent; text: string; filters: string[]; matches: MatchResult[]; slots: WeeklySlot[]; todos: AssistantTodo[]
  action: AssistantAction | null
}

/** Matches app.ai.assistant.chat.max-message-characters; the server checks it again. */
export const MAX_MESSAGE_CHARACTERS = 500

/** Understanding a message is one Gemini call, which can take several seconds. */
const ASSISTANT_TIMEOUT_MS = 60000

export async function askAssistant(message: string) {
  return (await api.post<AssistantReply>('/assistant/messages', { message }, { timeout: ASSISTANT_TIMEOUT_MS })).data
}
