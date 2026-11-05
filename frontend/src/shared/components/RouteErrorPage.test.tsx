import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import RouteErrorPage from './RouteErrorPage'

function Broken(): never {
  throw new Error('chunk failed to load')
}

describe('RouteErrorPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('replaces a page that throws with a retro error window and a way to reload', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const router = createMemoryRouter([{ path: '/', element: <Broken />, errorElement: <RouteErrorPage /> }])
    render(<RouterProvider router={router} />)

    expect(screen.getByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
