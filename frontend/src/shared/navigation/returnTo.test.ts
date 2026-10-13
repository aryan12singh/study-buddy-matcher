import { describe, expect, it } from 'vitest'
import type { Location } from 'react-router-dom'
import { returnToFrom, returnToState } from './returnTo'

const fallback = { path: '/connections', label: 'Back to connections' }
const at = (state: unknown) => ({ pathname: '/students/2', search: '', hash: '', key: 'k', state }) as Location

describe('returnToFrom', () => {
  it('uses the return target the linking page passed', () => {
    expect(returnToFrom(at(returnToState('/find-buddies', 'Back to find buddies')), fallback))
      .toEqual({ path: '/find-buddies', label: 'Back to find buddies' })
  })

  it('falls back when there is no state', () => {
    expect(returnToFrom(at(null), fallback)).toEqual(fallback)
  })

  it('ignores a target outside the app', () => {
    expect(returnToFrom(at(returnToState('https://example.com', 'Away')), fallback)).toEqual(fallback)
    expect(returnToFrom(at(returnToState('//example.com', 'Away')), fallback)).toEqual(fallback)
  })
})
