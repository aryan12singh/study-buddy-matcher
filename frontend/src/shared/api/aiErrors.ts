import axios from 'axios'
import type { Problem } from './client'

/** True when the server answered AI_UNAVAILABLE: AI is switched off on the server, or the provider is busy or failed. */
export function isAiUnavailable(error: unknown) {
  return axios.isAxiosError<Problem>(error) && error.response?.data?.code === 'AI_UNAVAILABLE'
}

/** The one wording for AI_UNAVAILABLE, which covers both causes, followed by the screen's way to carry on without AI. */
export function aiUnavailableMessage(alternative: string) {
  return `The AI helper isn't available right now — it may be switched off on this server or busy. ${alternative}`
}

/** For useAction: the AI_UNAVAILABLE wording, or undefined to keep the server's message for any other failure. */
export function describeAiFailure(alternative: string) {
  return (failure: unknown) => isAiUnavailable(failure) ? aiUnavailableMessage(alternative) : undefined
}
