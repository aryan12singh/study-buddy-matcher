import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { api } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import StudyRoomPage from './StudyRoomPage'
import type { StudyRoom } from './api'


const weather = { available: false, condition: 'CLEAR', temperatureCelsius: null, isDay: true, observedAt: null, stale: false, location: 'SMU campus' }
const room: StudyRoom = {
  groupId: 5, groupName: 'Revision crew', version: 0, serverTime: '2026-10-09T00:00:00Z',
  focusMinutes: 25, breakMinutes: 5, maxFocusMinutes: 180, maxBreakMinutes: 60,
  participantLimit: 4, groupLimit: 4, hostId: 1, coHostId: null, hostOnline: false,
  leader: false, canControl: false, joined: false, pollIntervalMillis: 2000, leaseLifetimeMillis: 30000,
  timer: { phase: 'FOCUS', status: 'IDLE', remainingMillis: 1500000, focusMillis: 1500000, breakMillis: 300000 },
  participants: [], members: [{ studentId: 1, name: 'Priya' }, { studentId: 2, name: 'Jamie' }],
}
const present = { studentId: 1, name: 'Priya', presence: 'PRESENT' as const, expiresAt: '2026-10-09T00:00:30Z', leader: false, host: true, coHost: false }
function joinedRoom(overrides: Partial<StudyRoom> = {}): StudyRoom {
  return { ...room, joined: true, hostOnline: true, canControl: true, participants: [present], ...overrides }
}
function open(overrides: Partial<StudyRoom> = {}) {
  vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/weather' ? weather : { ...room, ...overrides }))
  return renderPage(<StudyRoomPage />, '/groups/5/room', '/groups/:id/room')
}
async function joinStudyRoom() {
  const button = await screen.findByRole('button', { name: 'Join study room' })
  // Finish the joined panel's effects before the test continues.
  await act(async () => { fireEvent.click(button) })
}
async function openSettingsTab(name: string) {
  fireEvent.click(await screen.findByRole('button', { name: 'Settings' }))
  fireEvent.click(screen.getByRole('tab', { name }))
}
afterEach(() => { vi.restoreAllMocks(); localStorage.clear() })

describe('private room screen', () => {
  it('keeps timer, atmosphere and roles behind a tabbed settings menu that closes back to its button', async () => {
    open()
    const settings = await screen.findByRole('button', { name: 'Settings' })
    expect(settings).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByRole('tab', { name: 'Timer' })).not.toBeInTheDocument()
    fireEvent.click(settings)
    const timer = screen.getByRole('tab', { name: 'Timer' })
    expect(timer).toHaveAttribute('aria-selected', 'true')
    expect(timer).toHaveFocus()
    expect(screen.getByText('25 minutes', { selector: 'dd' })).toBeVisible()
    expect(screen.queryByLabelText('Focus minutes')).not.toBeInTheDocument()
    fireEvent.keyDown(timer, { key: 'ArrowRight' })
    const atmosphere = screen.getByRole('tab', { name: 'Room atmosphere' })
    expect(atmosphere).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('Window view')).toBeVisible()
    fireEvent.keyDown(atmosphere, { key: 'ArrowRight' })
    expect(screen.getByRole('tab', { name: 'Music' })).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Music' }), { key: 'End' })
    expect(screen.getByRole('tab', { name: 'Room roles' })).toHaveAttribute('aria-selected', 'true')
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Room roles' }), { key: 'Escape' })
    expect(settings).toHaveAttribute('aria-expanded', 'false')
    expect(settings).toHaveFocus()
  })

  it('offers a personal music player that says only this student hears it', async () => {
    open()
    expect(await screen.findByRole('button', { name: 'Open music settings' })).toBeInTheDocument()
    expect(screen.getByText('Only you will hear it, not the group.')).toBeInTheDocument()
  })

  it('shows the people studying as a crew inside the room panel', async () => {
    open({ participants: [present, { ...present, studentId: 2, name: 'Jamie', presence: 'BREAK', host: false }] })
    const crew = within(await screen.findByRole('region', { name: 'Studying together' }))
    expect(crew.getByText('2 / 4')).toBeInTheDocument()
    expect(crew.getByText('Host')).toBeInTheDocument()
    expect(crew.getByText('On a break')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Studying together' }).closest('.group-room')).not.toBeNull()
  })

  it('explains what leaving does: ending the session when last, or handing over as host', async () => {
    const jamie = { ...present, studentId: 2, name: 'Jamie', host: false }
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom({ participants: [present, jamie] })))
    vi.spyOn(api, 'put').mockResolvedValue(response(joinedRoom({ participants: [present, jamie] })))
    open()
    await joinStudyRoom()
    fireEvent.click(await screen.findByRole('button', { name: 'Leave room' }))
    expect(within(screen.getByRole('dialog')).getByText(/Jamie will take over as host/)).toBeInTheDocument()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Keep studying' }))
  })

  it('warns the last person that leaving ends the session', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    vi.spyOn(api, 'put').mockResolvedValue(response(joinedRoom()))
    open()
    await joinStudyRoom()
    fireEvent.click(await screen.findByRole('button', { name: 'Leave room' }))
    expect(within(screen.getByRole('dialog')).getByText(/You're the last one here, so leaving ends the session/)).toBeInTheDocument()
  })

  it('shows an empty preview and keeps controls inactive before joining', async () => {
    open()
    expect(await screen.findByText('The room is quiet.')).toBeInTheDocument()
    expect(screen.getByText('25:00')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Start focus' })).not.toBeInTheDocument()
  })

  it('joins as a host and confirms ending the session before sending a versioned reset', async () => {
    const post = vi.spyOn(api, 'post').mockImplementation(async (path, input) => {
      if (path.endsWith('/join')) return response(joinedRoom())
      const command = (input as { command: string }).command
      return response(joinedRoom({ version: command === 'START' ? 1 : 2,
        timer: { ...room.timer, status: command === 'START' ? 'RUNNING' : 'IDLE' } }))
    })
    open()
    await joinStudyRoom()
    fireEvent.click(await screen.findByRole('button', { name: 'Start focus' }))
    await screen.findByRole('button', { name: 'Pause for everyone' })
    expect(post).toHaveBeenCalledWith('/groups/5/room/timer', { command: 'START', expectedVersion: 0 })
    fireEvent.click(screen.getByRole('button', { name: 'End session' }))
    expect(post).not.toHaveBeenCalledWith('/groups/5/room/timer', expect.objectContaining({ command: 'RESET' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'End session for everyone' }))
    await screen.findByRole('button', { name: 'Start focus' })
    expect(post).toHaveBeenCalledWith('/groups/5/room/timer', { command: 'RESET', expectedVersion: 1 })
  })

  it('ordinary members cannot control the timer and leaving affects only this tab', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom({ canControl: false })))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    open()
    await joinStudyRoom()
    await screen.findByRole('button', { name: 'Leave room' })
    expect(screen.queryByRole('button', { name: 'Start focus' })).not.toBeInTheDocument()
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
    await joinStudyRoom()
    fireEvent.click(await screen.findByRole('button', { name: 'Leave room' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Leave study room' }))
    await screen.findByRole('button', { name: 'Join study room' })
    expect(screen.getByRole('timer')).toBeInTheDocument()
    expect(screen.queryByText(/Your room presence expired/)).not.toBeInTheDocument()
    expect(heartbeat).not.toHaveBeenCalled()
  })

  it('purges the private snapshot after a rejected heartbeat', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    vi.spyOn(api, 'put').mockRejectedValue(new Error('Membership removed'))
    open()
    await joinStudyRoom()
    await screen.findByRole('button', { name: 'Leave room' })
    fireEvent(window, new Event('focus'))
    expect(await screen.findByText('Membership removed')).toBeInTheDocument()
    expect(screen.queryByText('Priya (you)')).not.toBeInTheDocument()
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('stays in the room when a heartbeat fails on the network or server', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    const heartbeat = vi.spyOn(api, 'put')
      .mockRejectedValueOnce(new AxiosError('Network Error', 'ERR_NETWORK'))
      .mockRejectedValueOnce(new AxiosError('Unavailable', 'ERR_BAD_RESPONSE', undefined, undefined,
        { status: 503, statusText: 'Unavailable', data: {}, headers: {}, config: { headers: new AxiosHeaders() } }))
      .mockResolvedValue(response(joinedRoom()))
    open()
    await joinStudyRoom()
    await screen.findByRole('button', { name: 'Leave room' })
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(heartbeat).toHaveBeenCalledTimes(1))
    expect(await screen.findByText(/Reconnecting to the room/)).toBeInTheDocument()
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(heartbeat).toHaveBeenCalledTimes(2))
    expect(screen.getByRole('button', { name: 'Leave room' })).toBeInTheDocument()
    expect(screen.getByRole('timer')).toBeInTheDocument()
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(heartbeat).toHaveBeenCalledTimes(3))
    await waitFor(() => expect(screen.queryByText(/Reconnecting to the room/)).not.toBeInTheDocument())
  })

  it('allows a present host to change duration and capacity while preserving leader-only roles', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(joinedRoom({ version: 1, focusMinutes: 30 })))
    open()
    await joinStudyRoom()
    await openSettingsTab('Timer')
    expect(screen.queryByLabelText('Host')).not.toBeInTheDocument()
    fireEvent.change(await screen.findByLabelText('Focus minutes'), { target: { value: '30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save timer settings' }))
    await screen.findByText('Room settings were saved.')
    expect(put).toHaveBeenCalledWith('/groups/5/room/settings', expect.objectContaining({ focusMinutes: 30, hostId: 1, coHostId: null, expectedVersion: 0 }))
    fireEvent.click(screen.getByRole('tab', { name: 'Room roles' }))
    expect(screen.getByLabelText('Host')).toBeDisabled()
    expect(screen.getByLabelText('Co-host')).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Room capacity'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save room roles' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith('/groups/5/room/settings',
      expect.objectContaining({ focusMinutes: 30, participantLimit: 3, hostId: 1, coHostId: null, expectedVersion: 1 })))
  })

  it('lets the leader assign a co-host and shows current roles to every member', async () => {
    const put = vi.spyOn(api, 'put').mockResolvedValue(response({ ...room, leader: true, canControl: true, version: 1, coHostId: 2 }))
    open({ leader: true, canControl: true })
    fireEvent.click(await screen.findByRole('button', { name: 'Settings' }))
    fireEvent.click(screen.getByRole('tab', { name: 'Room roles' }))
    const roles = within(screen.getByRole('tabpanel'))
    expect(roles.getByText('No co-host', { selector: 'dd' })).toBeInTheDocument()
    fireEvent.change(roles.getByLabelText('Co-host'), { target: { value: '2' } })
    fireEvent.click(roles.getByRole('button', { name: 'Save room roles' }))
    await screen.findByText('Room settings were saved.')
    expect(put).toHaveBeenCalledWith('/groups/5/room/settings', expect.objectContaining({ hostId: 1, coHostId: 2, expectedVersion: 0 }))
    expect(roles.getByText('Jamie', { selector: 'dd' })).toBeInTheDocument()
  })

  it('shows ordinary members the room roles without an editing form', async () => {
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Settings' }))
    fireEvent.click(screen.getByRole('tab', { name: 'Room roles' }))
    const roles = within(screen.getByRole('tabpanel'))
    expect(roles.getByText('Priya', { selector: 'dd' })).toBeInTheDocument()
    expect(roles.getByText('No co-host', { selector: 'dd' })).toBeInTheDocument()
    expect(roles.queryByLabelText('Host')).not.toBeInTheDocument()
    expect(roles.getByText(/The group leader assigns the host and co-host/)).toBeInTheDocument()
  })

  it('locks the timer durations once the shared timer has started', async () => {
    open({ leader: true, canControl: true, timer: { ...room.timer, status: 'RUNNING' } })
    await openSettingsTab('Timer')
    expect(screen.getByLabelText('Focus minutes')).toBeDisabled()
    expect(screen.getByLabelText('Break minutes')).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Save timer settings' })).toBeDisabled()
    expect(screen.getByText('A session is in progress. End it before changing the durations.')).toBeInTheDocument()
  })

  it('validates settings and retains the edited version through background refreshes', async () => {
    const put = vi.spyOn(api, 'put').mockRejectedValue(new Error('Refresh the room before saving'))
    open({ leader: true, canControl: true })
    await openSettingsTab('Timer')
    fireEvent.change(await screen.findByLabelText('Focus minutes'), { target: { value: '181' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save timer settings' }))
    expect(screen.getByText('Choose a whole number from 1 to 180.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Focus minutes'), { target: { value: '30' } })
    vi.mocked(api.get).mockImplementation(async path => response(path === '/weather' ? weather : { ...room, leader: true, canControl: true, version: 7, focusMinutes: 40 }))
    fireEvent(window, new Event('focus'))
    await waitFor(() => expect(vi.mocked(api.get).mock.calls.filter(([path]) => path === '/groups/5/room')).toHaveLength(2))
    fireEvent.click(screen.getByRole('button', { name: 'Save timer settings' }))
    await screen.findByText('Refresh the room before saving')
    expect(put).toHaveBeenCalledWith('/groups/5/room/settings', expect.objectContaining({ focusMinutes: 30, expectedVersion: 0 }))
    expect(screen.getByLabelText('Focus minutes')).toHaveValue(30)
    fireEvent.click(screen.getByRole('button', { name: 'Discard timer changes' }))
    const discard = within(screen.getByRole('dialog', { name: 'Discard timer changes?' })).getByRole('button', { name: 'Discard changes' })
    // Pressing a confirmation portalled outside the menu must not close the menu and its form first.
    fireEvent.pointerDown(discard)
    expect(screen.getByRole('button', { name: 'Settings' })).toHaveAttribute('aria-expanded', 'true')
    fireEvent.click(discard)
    expect(screen.getByLabelText('Focus minutes')).toHaveValue(40)
  })

  it('does not render a late response after navigating to another group', async () => {
    const delayed = deferred<never>()
    vi.spyOn(api, 'get').mockImplementation(path => path === '/groups/5/room' ? delayed.promise : Promise.resolve(response(path === '/weather' ? weather : { ...room, groupId: 6, groupName: 'New room' })))
    const { router } = renderPage(<StudyRoomPage />, '/groups/5/room', '/groups/:id/room')
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))
    await router.navigate('/groups/6/room')
    await screen.findByRole('heading', { name: 'New room · study room' })
    delayed.resolve(response(room))
    expect(screen.queryByRole('heading', { name: 'Revision crew · study room' })).not.toBeInTheDocument()
  })

  it('marks your presence from another tab instead of showing this preview as joined', async () => {
    open({ joined: true, participants: [present] })
    expect(await screen.findByText('Priya (you, in another tab)')).toBeInTheDocument()
    expect(screen.getByText('You are in this room in another tab.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Join study room' })).toBeInTheDocument()
  })

  it('leaves the room when a joined tab is closed or reloaded', async () => {
    vi.spyOn(api, 'post').mockResolvedValue(response(joinedRoom()))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    open()
    fireEvent(window, new Event('pagehide'))
    expect(remove).not.toHaveBeenCalled()
    await joinStudyRoom()
    await screen.findByRole('button', { name: 'Leave room' })
    fireEvent(window, new Event('pagehide'))
    const tab = vi.mocked(api.post).mock.calls[0][1] as { clientId: string }
    expect(remove).toHaveBeenCalledWith(`/groups/5/room/presence/${tab.clientId}`, expect.objectContaining({ fetchOptions: { keepalive: true } }))
  })

  it('expands the room to full screen and back', async () => {
    const request = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(document.documentElement, 'requestFullscreen', { value: request, configurable: true })
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Full screen' }))
    expect(request).toHaveBeenCalled()
    expect(document.querySelector('.group-room-workspace')).toHaveClass('is-expanded')
    fireEvent.click(screen.getByRole('button', { name: 'Exit full screen' }))
    expect(document.querySelector('.group-room-workspace')).not.toHaveClass('is-expanded')
    delete (document.documentElement as Partial<HTMLElement>).requestFullscreen
  })

  it('collapses the expanded room with Escape when the browser has no full screen', async () => {
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Full screen' }))
    expect(document.querySelector('.group-room-workspace')).toHaveClass('is-expanded')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.getByRole('button', { name: 'Full screen' })).toBeInTheDocument()
  })

  it('hides the panel to show only the timer over the background, and brings it back', async () => {
    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Hide panel' }))
    expect(document.querySelector('.group-room-workspace')).toHaveClass('is-minimal')
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    const show = screen.getByRole('button', { name: 'Show panel' })
    expect(show).toHaveFocus()
    fireEvent.click(show)
    expect(document.querySelector('.group-room-workspace')).not.toHaveClass('is-minimal')
    expect(screen.queryByRole('button', { name: 'Show panel' })).not.toBeInTheDocument()
  })

  it('does not call the API for an invalid group link', () => {
    const get = vi.spyOn(api, 'get')
    renderPage(<StudyRoomPage />, '/groups/invalid/room', '/groups/:id/room')
    expect(screen.getByText('This room link is invalid.')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })
})
