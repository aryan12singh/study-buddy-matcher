import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { useResource } from './useResource'
import { refreshResources } from './client'
import { deferred } from '../../test/renderApp'

afterEach(() => vi.restoreAllMocks())
describe('resource lifecycle', () => {
  it('aborts a superseded profile read and refuses its late response', async () => {
    const first = deferred<string>(), second = deferred<string>()
    const signals: AbortSignal[] = []
    const { result, rerender } = renderHook(({ id }) => useResource(id, signal => { signals.push(signal); return id === 'first' ? first.promise : second.promise }), { initialProps: { id: 'first' } })
    await waitFor(() => expect(signals).toHaveLength(1))
    rerender({ id: 'second' })
    await waitFor(() => expect(signals).toHaveLength(2))
    expect(signals[0].aborted).toBe(true)
    await act(async () => { second.resolve('public second profile'); await second.promise })
    await waitFor(() => expect(result.current.data).toBe('public second profile'))
    await act(async () => { first.resolve('old private profile'); await first.promise })
    expect(result.current.data).toBe('public second profile')
  })
  it('keeps the last response visible while a focus refresh is in flight, then replaces it', async () => {
    const pending = deferred<string>()
    const loader = vi.fn().mockResolvedValueOnce('connected contact').mockImplementationOnce(() => pending.promise)
    const { result } = renderHook(() => useResource('profile', loader))
    await waitFor(() => expect(result.current.data).toBe('connected contact'))
    act(() => window.dispatchEvent(new Event('focus')))
    expect(result.current.data).toBe('connected contact')
    expect(result.current.loading).toBe(true)
    await act(async () => { pending.resolve('public profile'); await pending.promise })
    await waitFor(() => expect(result.current.data).toBe('public profile'))
  })
  it('clears the last response when a background refresh fails', async () => {
    const pending = deferred<string>()
    const loader = vi.fn().mockResolvedValueOnce('connected contact').mockImplementationOnce(() => pending.promise)
    const { result } = renderHook(() => useResource('profile', loader))
    await waitFor(() => expect(result.current.data).toBe('connected contact'))
    act(() => window.dispatchEvent(new Event('focus')))
    await act(async () => { pending.reject(new Error('Network down')); await pending.promise.catch(() => undefined) })
    expect(result.current.data).toBeUndefined()
    expect(result.current.error).toBe('Network down')
  })
  it('purges immediately when permissions change', async () => {
    const pending = deferred<string>()
    const loader = vi.fn().mockResolvedValueOnce('connected contact').mockImplementationOnce(() => pending.promise)
    const { result } = renderHook(() => useResource('profile', loader))
    await waitFor(() => expect(result.current.data).toBe('connected contact'))
    act(() => refreshResources(true))
    expect(result.current.data).toBeUndefined()
  })
})
