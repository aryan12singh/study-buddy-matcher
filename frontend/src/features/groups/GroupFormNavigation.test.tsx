import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Link, MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { deferred, response } from '../../test/renderApp'
import GroupFormPage from './GroupFormPage'

const course = { id: 3, code: 'IS442', name: 'Object Oriented Programming' }
const group = { id: 5, name: 'OOP study crew', description: '', courseId: 3, maxGroupSize: 4,
  preferredStudyMode: null, studyGoals: [], availability: [], memberCount: 1, active: true,
  viewer: { leader: true, member: true, requestId: null, requestStatus: null } }

function CurrentPath() {
  const location = useLocation()
  return <><p>Current path: {location.pathname}</p><p>Current entry: {location.key}</p></>
}
function renderForm(path: string) {
  render(<MemoryRouter initialEntries={[path]}>
    <CurrentPath />
    <Link to="/groups/6/edit">Open another group</Link>
    <Routes>
      <Route path="/groups/new" element={<GroupFormPage />} />
      <Route path="/groups/:id/edit" element={<GroupFormPage />} />
      <Route path="*" element={<p>Destination page</p>} />
    </Routes>
  </MemoryRouter>)
}

afterEach(() => vi.restoreAllMocks())

describe('group save navigation', () => {
  it.each(['create', 'edit'] as const)('does not redirect after leaving a pending %s form', async mode => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : group))
    const save = deferred<never>()
    const write = vi.spyOn(api, mode === 'create' ? 'post' : 'put').mockImplementation(() => save.promise)
    const changed = vi.fn()
    window.addEventListener('resources-changed', changed)
    try {
      renderForm(mode === 'create' ? '/groups/new' : '/groups/5/edit')
      await screen.findByLabelText('Course')
      if (mode === 'create') {
        fireEvent.change(screen.getByLabelText('Group name'), { target: { value: group.name } })
        fireEvent.change(screen.getByLabelText('Course'), { target: { value: '3' } })
      } else await screen.findByDisplayValue(group.name)
      fireEvent.click(screen.getByRole('button', { name: mode === 'create' ? 'Create study group' : 'Save group changes' }))
      await waitFor(() => expect(write).toHaveBeenCalledTimes(1))
      fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
      const destination = mode === 'create' ? '/groups' : '/groups/5'
      expect(screen.getByText(`Current path: ${destination}`)).toBeInTheDocument()
      const entry = screen.getByText(/^Current entry:/).textContent
      await act(async () => { save.resolve(response(group)) })
      expect(screen.getByText(`Current path: ${destination}`)).toBeInTheDocument()
      expect(screen.getByText(/^Current entry:/)).toHaveTextContent(entry!)
      // The server mutation still happened; other mounted views must refresh.
      expect(changed).toHaveBeenCalledTimes(1)
    } finally { window.removeEventListener('resources-changed', changed) }
  })

  it('does not redirect from an earlier save when the same form component displays another group', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/courses' ? [course] : {
      ...group, id: path.includes('/6') ? 6 : 5, name: path.includes('/6') ? 'Another group' : group.name
    }))
    const save = deferred<never>()
    const write = vi.spyOn(api, 'put').mockImplementation(() => save.promise)
    renderForm('/groups/5/edit')
    await screen.findByDisplayValue(group.name)
    fireEvent.click(screen.getByRole('button', { name: 'Save group changes' }))
    await waitFor(() => expect(write).toHaveBeenCalledTimes(1))
    fireEvent.click(screen.getByRole('link', { name: 'Open another group' }))
    await screen.findByDisplayValue('Another group')
    await act(async () => { save.resolve(response(group)) })
    expect(screen.getByText('Current path: /groups/6/edit')).toBeInTheDocument()
  })
})
