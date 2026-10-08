import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import StudyRoomPage from './StudyRoomPage'
import type { StudyRoom } from './api'

const engine = vi.hoisted(() => ({ enable: vi.fn(), play: vi.fn(), stop: vi.fn(), close: vi.fn() }))
vi.mock('./RoomAudioEngine', () => ({ RoomAudioEngine: class {
  enable = engine.enable; play = engine.play; stop = engine.stop; close = engine.close
} }))

const room: StudyRoom = {
  groupId: 5, groupName: 'Revision crew', version: 0, serverTime: '2026-10-09T00:00:00Z',
  focusMinutes: 25, breakMinutes: 5, maxFocusMinutes: 180, maxBreakMinutes: 60,
  participantLimit: 4, groupLimit: 4, hostId: 1, coHostId: null, hostOnline: false,
  leader: false, canControl: false, joined: false, pollIntervalMillis: 2000, leaseLifetimeMillis: 30000,
  timer: { phase: 'FOCUS', status: 'IDLE', remainingMillis: 1500000, focusMillis: 1500000, breakMillis: 300000 },
  audio: { preset: 'CALM_MUSIC', playing: false },
  audioPresets: [{ id: 'CALM_MUSIC', label: 'Calm melody', kind: 'MUSIC' }, { id: 'RAIN', label: 'Soft rain', kind: 'AMBIENT' }],
  participants: [], members: [{ studentId: 1, name: 'Priya' }, { studentId: 2, name: 'Jamie' }],
}
const present = { studentId: 1, name: 'Priya', presence: 'PRESENT' as const, expiresAt: '2026-10-09T00:00:30Z', leader: false, host: true, coHost: false }
function joinedRoom(overrides: Partial<StudyRoom> = {}): StudyRoom {
  return { ...room, joined: true, hostOnline: true, canControl: true, participants: [present], ...overrides }
}
function open(overrides: Partial<StudyRoom> = {}) {
  vi.spyOn(api, 'get').mockResolvedValue(response({ ...room, ...overrides }))
  return renderPage(<StudyRoomPage />, '/groups/5/room', '/groups/:id/room')
}
afterEach(() => { vi.restoreAllMocks(); Object.values(engine).forEach(mock => mock.mockReset()) })

describe('private room screen', () => {
  it('shows an empty preview and keeps controls and audio inactive before joining', async () => {
    open()
    expect(await screen.findByText('The room is quiet')).toBeInTheDocument()
    expect(screen.getByText('25:00')).toBeInTheDocument()
    expect(screen.getByText(/not contact numbers/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Start focus' })).not.toBeInTheDocument()
    expect(screen.queryByText('Leader settings')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Enable audio in this browser' })).toBeDisabled()
    expect(engine.enable).not.toHaveBeenCalled()
    expect(engine.play).not.toHaveBeenCalled()
  })

  it('joins as a host and confirms reset before sending a versioned command', async () => {
    const post = vi.spyOn(api, 'post').mockImplementation(async (path, input) => {
      if (path.endsWith('/join')) return response(joinedRoom())
      const command = (input as { command: string }).command
      return response(joinedRoom({ version: command === 'START' ? 1 : 2,
        timer: { ...room.timer, status: command === 'START' ? 'RUNNING' : 'IDLE' } }))
    })
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Start focus' }))
    await screen.findByRole('button', { name: 'Pause for everyone' })
    expect(post).toHaveBeenCalledWith('/groups/5/room/timer', { command: 'START', expectedVersion: 0 })
    fireEvent.click(screen.getByRole('button', { name: 'Reset timer' }))
    expect(post).not.toHaveBeenCalledWith('/groups/5/room/timer', expect.objectContaining({ command: 'RESET' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Reset for everyone' }))
    await screen.findByRole('button', { name: 'Start focus' })
    expect(post).toHaveBeenCalledWith('/groups/5/room/timer', { command: 'RESET', expectedVersion: 1 })
  })

  it('lets the leader recover shared controls without occupying a full room', async () => {
    open({ leader: true, canControl: true, participantLimit: 1, participants: [present] })
    expect(await screen.findByRole('button', { name: 'Start focus' })).toBeEnabled()
    expect(screen.getByLabelText('Shared selection')).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Play room audio' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Enable audio in this browser' })).toBeDisabled()
    expect(engine.enable).not.toHaveBeenCalled()
  })

  it('ordinary members cannot control the timer/audio and leaving affects only this tab', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom({ canControl: false })))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    await screen.findByRole('button', { name: 'Leave room' })
    expect(screen.queryByRole('button', { name: 'Start focus' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Shared selection')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Leave room' }))
    expect(remove).not.toHaveBeenCalled()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Leave study room' }))
    await screen.findByRole('button', { name: 'Join study room' })
    const tab = post.mock.calls[0][1] as { clientId: string }
    expect(remove).toHaveBeenCalledWith(`/groups/5/room/presence/${tab.clientId}`)
  })

  it('switches to preview before mutation refresh can heartbeat a deleted lease', async () => {
    let removed = false
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    vi.spyOn(api, 'delete').mockImplementation(async () => { removed = true; return response(undefined) })
    const heartbeat = vi.spyOn(api, 'put').mockImplementation(async () => {
      if (removed) throw new Error('Your room presence expired; join the room again')
      return response(joinedRoom())
    })
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Leave room' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Leave study room' }))
    await screen.findByRole('button', { name: 'Join study room' })
    expect(screen.getByRole('timer')).toBeInTheDocument()
    expect(screen.queryByText(/Your room presence expired/)).not.toBeInTheDocument()
    expect(heartbeat).not.toHaveBeenCalled()
  })

  it('purges the private snapshot and stops local audio after a rejected heartbeat', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom({ audio: { preset: 'RAIN', playing: true } })))
    vi.spyOn(api, 'put').mockRejectedValue(new Error('Membership removed'))
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    await screen.findByRole('button', { name: 'Leave room' })
    fireEvent.click(await screen.findByRole('button', { name: 'Enable audio in this browser' }))
    await waitFor(() => expect(engine.play).toHaveBeenCalledWith('RAIN', 0.25))
    fireEvent(window, new Event('focus'))
    expect(await screen.findByText('Membership removed')).toBeInTheDocument()
    expect(screen.queryByText('Priya (you)')).not.toBeInTheDocument()
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(engine.close).toHaveBeenCalled()
  })

  it('keeps audio silent until consent and applies volume/mute only locally', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom({ audio: { preset: 'RAIN', playing: true } })))
    const put = vi.spyOn(api, 'put')
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    await screen.findByRole('button', { name: 'Leave room' })
    expect(engine.play).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Enable audio in this browser' }))
    await waitFor(() => expect(engine.play).toHaveBeenCalledWith('RAIN', 0.25))
    fireEvent.change(screen.getByRole('slider'), { target: { value: '42' } })
    expect(engine.play).toHaveBeenLastCalledWith('RAIN', 0.42)
    fireEvent.click(screen.getByLabelText('Mute only my audio'))
    expect(engine.play).toHaveBeenLastCalledWith('RAIN', 0)
    expect(put).not.toHaveBeenCalled()
  })

  it('handles audio failure without disabling room controls', async () => {
    engine.enable.mockRejectedValueOnce(new Error('Audio unavailable'))
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Join study room' }))
    await screen.findByRole('button', { name: 'Leave room' })
    fireEvent.click(await screen.findByRole('button', { name: 'Enable audio in this browser' }))
    expect(await screen.findByText('Audio unavailable')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start focus' })).toBeEnabled()
  })

  it('validates settings and retains the edited version through background refreshes', async () => {
    const put = vi.spyOn(api, 'put').mockRejectedValue(new Error('Refresh the room before saving'))
    open({ leader: true, canControl: true })
    fireEvent.change(await screen.findByLabelText('Focus minutes'), { target: { value: '181' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save room settings' }))
    expect(screen.getByText('Choose a whole number from 1 to 180.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Focus minutes'), { target: { value: '30' } })
    vi.mocked(api.get).mockResolvedValue(response({ ...room, leader: true, canControl: true, version: 7, focusMinutes: 40 }))
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2))
    fireEvent.click(screen.getByRole('button', { name: 'Save room settings' }))
    await screen.findByText('Refresh the room before saving')
    expect(put).toHaveBeenCalledWith('/groups/5/room/settings', expect.objectContaining({ focusMinutes: 30, expectedVersion: 0 }))
    expect(screen.getByLabelText('Focus minutes')).toHaveValue(30)
    fireEvent.click(screen.getByRole('button', { name: 'Discard settings changes' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Discard changes' }))
    expect(screen.getByLabelText('Focus minutes')).toHaveValue(40)
  })

  it('does not render a late response after navigating to another group', async () => {
    const delayed = deferred<never>()
    vi.spyOn(api, 'get').mockReturnValueOnce(delayed.promise).mockResolvedValue(response({ ...room, groupId: 6, groupName: 'New room' }))
    const { router } = renderPage(<StudyRoomPage />, '/groups/5/room', '/groups/:id/room')
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))
    await router.navigate('/groups/6/room')
    await screen.findByRole('heading', { name: 'New room · study room' })
    delayed.resolve(response(room))
    expect(screen.queryByRole('heading', { name: 'Revision crew · study room' })).not.toBeInTheDocument()
  })

  it('does not call the API for an invalid group link', () => {
    const get = vi.spyOn(api, 'get')
    renderPage(<StudyRoomPage />, '/groups/invalid/room', '/groups/:id/room')
    expect(screen.getByText('This room link is invalid.')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })
})
