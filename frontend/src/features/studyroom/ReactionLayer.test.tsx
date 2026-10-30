import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import ReactionLayer from './ReactionLayer'
import ReactionTray, { REACTION_COOLDOWN_MS } from './ReactionTray'
import type { RoomReaction } from './api'

const NOW = '2026-10-29T02:00:10Z'
const clap: RoomReaction = { id: 7, type: 'CLAP', studentId: 2, senderName: 'Jamie', sentAt: '2026-10-29T02:00:09Z' }

afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })

describe('room reactions', () => {
  it('floats a new reaction once, then removes it', () => {
    vi.useFakeTimers()
    const { rerender } = render(<ReactionLayer reactions={[clap]} serverTime={NOW} />)
    act(() => { vi.advanceTimersByTime(1) })
    expect(screen.getAllByText('Jamie')).toHaveLength(1)
    rerender(<ReactionLayer reactions={[clap]} serverTime="2026-10-29T02:00:12Z" />)
    act(() => { vi.advanceTimersByTime(1) })
    expect(screen.getAllByText('Jamie')).toHaveLength(1)
    act(() => { vi.advanceTimersByTime(3600) })
    expect(screen.queryByText('Jamie')).not.toBeInTheDocument()
  })

  it('does not replay reactions that were already old when the room opened', () => {
    vi.useFakeTimers()
    render(<ReactionLayer reactions={[{ ...clap, sentAt: '2026-10-29T02:00:02Z' }]} serverTime={NOW} />)
    act(() => { vi.advanceTimersByTime(1) })
    expect(screen.queryByText('Jamie')).not.toBeInTheDocument()
  })

  it('sends a reaction from the tray only after joining and reports a refusal', async () => {
    const sent: RoomReaction = { id: 9, type: 'HEART', studentId: 1, senderName: 'Priya', sentAt: NOW }
    const post = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: sent })
      .mockRejectedValueOnce(new Error('You are reacting too quickly'))
    const onSent = vi.fn()
    const { rerender } = render(<ReactionTray groupId={5} joined={false} onSent={onSent} />)
    expect(screen.getByRole('button', { name: 'Send heart reaction' })).toBeDisabled()
    expect(screen.getByText('Join the room to send reactions.')).toBeInTheDocument()
    rerender(<ReactionTray groupId={5} joined onSent={onSent} />)
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Send heart reaction' })) })
    expect(post).toHaveBeenCalledWith('/groups/5/room/reactions', { type: 'HEART' })
    expect(onSent).toHaveBeenCalledWith(sent)
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Send growing reaction' })) })
    expect(screen.getByRole('alert')).toHaveTextContent('You are reacting too quickly')
    expect(onSent).toHaveBeenCalledTimes(1)
  })

  it('rests the buttons for the cooldown window after the server says the student is reacting too quickly', async () => {
    vi.useFakeTimers()
    const tooQuick = new AxiosError('Conflict', 'ERR_BAD_REQUEST', undefined, undefined, { config: { headers: new AxiosHeaders() }, status: 409,
      statusText: 'Conflict', headers: {}, data: { code: 'STATE_CONFLICT', message: 'You are reacting too quickly; wait a moment and try again' } })
    const post = vi.spyOn(api, 'post').mockRejectedValue(tooQuick)
    render(<ReactionTray groupId={5} joined onSent={vi.fn()} />)
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Send heart reaction' })) })
    expect(screen.getByRole('status')).toHaveTextContent('You are reacting too quickly; wait a moment and try again')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send clap reaction' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Send clap reaction' }))
    expect(post).toHaveBeenCalledTimes(1)
    act(() => { vi.advanceTimersByTime(REACTION_COOLDOWN_MS) })
    expect(screen.getByRole('button', { name: 'Send clap reaction' })).toBeEnabled()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })

  it('floats the sender\'s own reaction at once and does not repeat it when the snapshot catches up', () => {
    vi.useFakeTimers()
    const own: RoomReaction = { ...clap, id: 12, senderName: 'Priya', sentAt: '2026-10-29T02:00:11Z' }
    const { rerender } = render(<ReactionLayer reactions={[own]} serverTime={NOW} />)
    act(() => { vi.advanceTimersByTime(1) })
    expect(screen.getAllByText('Priya')).toHaveLength(1)
    rerender(<ReactionLayer reactions={[own, own]} serverTime="2026-10-29T02:00:12Z" />)
    act(() => { vi.advanceTimersByTime(1) })
    expect(screen.getAllByText('Priya')).toHaveLength(1)
  })
})
