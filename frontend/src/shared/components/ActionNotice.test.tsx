import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import ActionNotice from './ActionNotice'

describe('ActionNotice', () => {
  afterEach(() => { vi.useRealTimers() })

  it('hides a success message after five seconds', () => {
    vi.useFakeTimers()
    render(<ActionNotice success="Room settings were saved." />)
    expect(screen.getByRole('status')).toHaveTextContent('Room settings were saved.')
    act(() => { vi.advanceTimersByTime(4900) })
    expect(screen.getByRole('status')).toBeInTheDocument()
    act(() => { vi.advanceTimersByTime(100) })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('lets either message be dismissed, and keeps an error until it is', () => {
    vi.useFakeTimers()
    render(<ActionNotice error="Something went wrong." success="Saved." />)
    act(() => { vi.advanceTimersByTime(10000) })
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong.')
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss error message' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows the same message again when an action repeats it', () => {
    const { rerender } = render(<ActionNotice success="Saved." />)
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss success message' }))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    // An action clears its message before reporting again.
    rerender(<ActionNotice />)
    rerender(<ActionNotice success="Saved." />)
    expect(screen.getByRole('status')).toHaveTextContent('Saved.')
  })
})
