import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import SendRequestDialog from './SendRequestDialog'
import ApplyGroupDialog from '../groups/ApplyGroupDialog'

afterEach(() => vi.restoreAllMocks())

describe('request and application draft protection', () => {
  it('protects a buddy draft on Cancel and Escape, preserving edits until explicitly discarded', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const close = vi.fn()
    const post = vi.spyOn(api, 'post')
    renderPage(<SendRequestDialog receiverId={2} receiverName="Jamie" onClose={close} />)
    await screen.findByLabelText('Course')
    fireEvent.change(screen.getByLabelText('Message (optional)'), { target: { value: 'Study on Tuesday?' } })
    const draft = screen.getByRole('dialog', { name: 'Send match request to Jamie' })
    fireEvent.click(within(draft).getByRole('button', { name: 'Cancel' }))
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(within(draft).getByLabelText('Message (optional)')).toHaveValue('Study on Tuesday?')
    fireEvent.keyDown(draft, { key: 'Escape' })
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft' }))
    expect(close).toHaveBeenCalledTimes(1)
    expect(post).not.toHaveBeenCalled()
  })
  it('protects an application draft when its window close control is used', () => {
    const close = vi.fn()
    renderPage(<ApplyGroupDialog groupId={5} groupName="OOP crew" onClose={close} />)
    fireEvent.change(screen.getByLabelText('Message (optional)'), { target: { value: 'I can join Fridays.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Close Request to join OOP crew' }))
    expect(close).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Discard draft' }))
    expect(close).toHaveBeenCalledTimes(1)
  })
  it('closes an untouched application without an extra confirmation', () => {
    const close = vi.fn()
    renderPage(<ApplyGroupDialog groupId={5} groupName="OOP crew" onClose={close} />)
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(close).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog', { name: 'Discard this draft?' })).not.toBeInTheDocument()
  })
  it('refreshes data but never invokes old dialog callbacks after leaving a pending request', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const pending = deferred<never>()
    const post = vi.spyOn(api, 'post').mockImplementation(() => pending.promise)
    const close = vi.fn(), sent = vi.fn(), changed = vi.fn()
    window.addEventListener('resources-changed', changed)
    try {
      const { router } = renderPage(<SendRequestDialog receiverId={2} onClose={close} onSent={sent} />, '/draft', '/draft')
      fireEvent.click(screen.getByRole('button', { name: 'Send match request' }))
      await waitFor(() => expect(post).toHaveBeenCalledTimes(1))
      await act(async () => { await router.navigate('/other-page') })
      fireEvent.click(await screen.findByRole('button', { name: 'Leave page' }))
      await screen.findByText('Destination page')
      await act(async () => pending.resolve(response({ id: 10 })))
      expect(close).not.toHaveBeenCalled()
      expect(sent).not.toHaveBeenCalled()
      expect(changed).toHaveBeenCalledTimes(1)
    } finally { window.removeEventListener('resources-changed', changed) }
  })
})
