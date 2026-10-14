import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import type { GroupDetail } from './api'
import GroupFormPage from './GroupFormPage'

const course = { id: 3, code: 'IS442', name: 'Object Oriented Programming' }
const group: GroupDetail = {
  id: 5, name: 'OOP study crew', description: '', courseId: 3, courseCode: course.code, courseName: course.name,
  leaderId: 1, leaderName: 'Priya', maxGroupSize: 4, preferredStudyMode: null, studyGoals: [], memberCount: 1,
  active: true, viewer: { leader: true, member: true, requestId: null, requestStatus: null }, members: [],
  createdAt: '2026-10-13T08:00:00Z', pendingApplications: 0,
  availability: [{ dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '11:00:00' }]
}

function serve(data = group) {
  return vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : data))
}

afterEach(() => vi.restoreAllMocks())

describe('group schedule drafts', () => {
  it.each([
    ['create', '25:00'], ['create', ''], ['create', '12:'],
    ['edit', '25:00'], ['edit', ''], ['edit', '12:']
  ] as const)('blocks a %s save for the visible %j draft until it is corrected', async (mode, text) => {
    serve()
    const write = vi.spyOn(api, mode === 'create' ? 'post' : 'put').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, mode === 'create' ? '/groups/new' : '/groups/5/edit', mode === 'create' ? '/groups/new' : '/groups/:id/edit')
    if (mode === 'create') {
      await screen.findByLabelText('Course')
      fireEvent.change(screen.getByLabelText('Group name'), { target: { value: group.name } })
      fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
      fireEvent.click(screen.getByRole('button', { name: 'Add time block' }))
    } else await screen.findByDisplayValue(group.name)
    const start = screen.getByLabelText('Start 1')
    fireEvent.change(start, { target: { value: text } })
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    fireEvent.submit(start.closest('form')!)
    expect(write).not.toHaveBeenCalled()
    expect(start).toHaveValue(text)
    expect(start).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Correct the highlighted weekly times before saving.')).toBeInTheDocument()
    fireEvent.change(start, { target: { value: '09:30' } })
    fireEvent.click(screen.getByRole('button', { name: mode === 'create' ? 'Create study group' : 'Save group changes' }))
    await waitFor(() => expect(write).toHaveBeenCalledWith(mode === 'create' ? '/groups' : '/groups/5', expect.objectContaining({
      availability: [{ dayOfWeek: 'MONDAY', startTime: '09:30', endTime: mode === 'create' ? '10:00' : '11:00:00' }]
    })))
    expect(await screen.findByText('Destination page')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('finishes valid shorthand on submit without a blur event', async () => {
    serve()
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    await screen.findByDisplayValue(group.name)
    const start = screen.getByLabelText('Start 1')
    fireEvent.change(start, { target: { value: '930' } })
    fireEvent.submit(start.closest('form')!)
    await waitFor(() => expect(put).toHaveBeenCalledWith('/groups/5', expect.objectContaining({
      availability: [{ ...group.availability[0], startTime: '09:30' }]
    })))
  })

  it('guards an incomplete draft, keeps it when requested, and discards it only after confirmation', async () => {
    serve()
    const put = vi.spyOn(api, 'put')
    const { router } = renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    await screen.findByDisplayValue(group.name)
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '12:' } })
    const reload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(reload)
    expect(reload.defaultPrevented).toBe(true)
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    expect(screen.getByRole('dialog', { name: 'Discard unsaved changes?' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Keep editing' }))
    expect(screen.getByLabelText('Start 1')).toHaveValue('12:')
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/5'))
    expect(put).not.toHaveBeenCalled()
    const cleanReload = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(cleanReload)
    expect(cleanReload.defaultPrevented).toBe(false)
  })

  it('clears the draft and its validation when a discarded form opens another group in the same route', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : {
      ...group, id: path === '/groups/6' ? 6 : 5, name: path === '/groups/6' ? 'Another crew' : group.name,
      availability: [{ ...group.availability[0], startTime: path === '/groups/6' ? '10:00:00' : '09:00:00' }]
    }))
    const { router } = renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    await screen.findByDisplayValue(group.name)
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '25:00' } })
    fireEvent.submit(screen.getByLabelText('Start 1').closest('form')!)
    await act(async () => { await router.navigate('/groups/6/edit') })
    fireEvent.click(screen.getByRole('button', { name: 'Discard changes' }))
    await screen.findByDisplayValue('Another crew')
    expect(screen.getByLabelText('Start 1')).toHaveValue('10:00')
    expect(screen.queryByText(/25:00 is not a valid start time/)).not.toBeInTheDocument()
    expect(screen.queryByText('Correct the highlighted weekly times before saving.')).not.toBeInTheDocument()
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/6'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('saves overlapping meeting blocks and leaves their rows valid', async () => {
    const availability = [group.availability[0], { ...group.availability[0], startTime: '10:00:00', endTime: '12:00:00' }]
    serve({ ...group, availability })
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    await screen.findByDisplayValue(group.name)
    expect(screen.queryByText(/This clashes with/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save group changes' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/groups/5', expect.objectContaining({ availability })))
  })

  it('allows saving after removing the row that contained an invalid draft', async () => {
    serve()
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(group))
    renderPage(<GroupFormPage />, '/groups/5/edit', '/groups/:id/edit')
    await screen.findByDisplayValue(group.name)
    fireEvent.change(screen.getByLabelText('Start 1'), { target: { value: '25:00' } })
    fireEvent.submit(screen.getByLabelText('Start 1').closest('form')!)
    expect(put).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Remove time 1' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save group changes' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/groups/5', expect.objectContaining({ availability: [] })))
  })
})
