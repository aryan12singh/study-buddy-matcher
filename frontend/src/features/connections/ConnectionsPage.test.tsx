import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { deferred, renderPage, response } from '../../test/renderApp'
import ConnectionsPage from './ConnectionsPage'
import SendRequestDialog from './SendRequestDialog'
import { getRelationshipCounts } from './api'

const request = { id: 10, senderId: 2, senderName: 'Jamie', receiverId: 1, receiverName: 'Priya', message: 'Revise together?', status: 'PENDING', createdAt: '2026-10-07T01:00:00Z', respondedAt: null, context: { origin: 'MATCHING', courseId: 3, courseCode: 'IS442', studyGoal: 'EXAM_PREPARATION' } }
afterEach(() => vi.restoreAllMocks())

describe('buddy requests and connections', () => {
  it('renders loading, empty, error and a working retry', async () => {
    const pending = deferred<never>()
    const get = vi.spyOn(api, 'get').mockImplementationOnce(() => pending.promise).mockRejectedValueOnce(new Error('Temporary failure')).mockResolvedValueOnce(response([]))
    renderPage(<ConnectionsPage />, '/connections', '/connections')
    expect(screen.getByRole('status')).toHaveTextContent('Loading')
    pending.resolve(response([]))
    expect(await screen.findByText('No incoming requests')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Outgoing requests' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Temporary failure')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('No outgoing requests')).toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(3)
  })
  it('accepts the pending request and refreshes the answered history', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValueOnce(response([request])).mockResolvedValue(response([{ ...request, status: 'ACCEPTED', respondedAt: '2026-10-07T02:00:00Z' }]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...request, status: 'ACCEPTED' }))
    renderPage(<ConnectionsPage />, '/connections', '/connections')
    expect(await screen.findByText('From matching · IS442 · Exam preparation')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Accept request' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/match-requests/10/accept'))
    expect(await screen.findByText('Accepted', { selector: '.status-badge' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Accept request' })).not.toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(2)
  })
  it('keeps decline available after a rejected action', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([request]))
    const post = vi.spyOn(api, 'post').mockRejectedValueOnce(new Error('The request changed. Refresh and try again.')).mockResolvedValue(response({ ...request, status: 'DECLINED' }))
    renderPage(<ConnectionsPage />, '/connections', '/connections')
    fireEvent.click(await screen.findByRole('button', { name: 'Decline request' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('The request changed')
    expect(screen.getByRole('button', { name: 'Decline request' })).toBeEnabled()
    fireEvent.click(screen.getByRole('button', { name: 'Decline request' }))
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2))
  })
  it('requires confirmation to disconnect and uses the connection endpoint', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response([{ id: 7, otherStudentId: 2, otherStudentName: 'Jamie', createdAt: '2026-10-06T01:00:00Z' }])).mockResolvedValue(response([]))
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(response(undefined))
    renderPage(<ConnectionsPage />, '/connections?view=connected', '/connections')
    fireEvent.click(await screen.findByRole('button', { name: 'Disconnect' }))
    expect(remove).not.toHaveBeenCalled()
    const dialog = screen.getByRole('dialog')
    expect(dialog).toHaveTextContent('no longer share')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Disconnect' }))
    await waitFor(() => expect(remove).toHaveBeenCalledWith('/connections/7'))
    expect(await screen.findByText('No connected buddies yet')).toBeInTheDocument()
  })
  it('sends matching context without invented private fields', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([{ id: 3, code: 'IS442', name: 'OOP' }]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response(request)), close = vi.fn()
    renderPage(<SendRequestDialog receiverId={2} receiverName="Jamie" context={{ origin: 'MATCHING', courseId: 3, studyGoal: 'EXAM_PREPARATION' }} onClose={close} />)
    await screen.findByLabelText('Course')
    fireEvent.change(screen.getByLabelText('Message (optional)'), { target: { value: '  Revise together?  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Send match request' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/match-requests', { receiverId: 2, message: 'Revise together?', context: { origin: 'MATCHING', courseId: 3, studyGoal: 'EXAM_PREPARATION' } }))
    expect(close).toHaveBeenCalledTimes(1)
  })
  it('supplies real pending-only counts to absent dashboard consumers', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/connections' ? [{ id: 7 }] : path.endsWith('/incoming') ? [request, { ...request, status: 'DECLINED' }] : [{ ...request, status: 'ACCEPTED' }]))
    expect(await getRelationshipCounts()).toEqual({ incoming: 1, outgoing: 0, connections: 1 })
  })
})
