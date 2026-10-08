import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import CopyButton from './CopyButton'
import { deferred } from '../../test/renderApp'

const originalClipboard = Object.getOwnPropertyDescriptor(navigator, 'clipboard')
afterEach(() => {
  vi.restoreAllMocks()
  if (originalClipboard) Object.defineProperty(navigator, 'clipboard', originalClipboard)
  else Reflect.deleteProperty(navigator, 'clipboard')
})

function clipboard(writeText: (text: string) => Promise<void>) {
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
}

describe('explicit copy actions', () => {
  it('copies the provided authorised value once and announces the outcome', async () => {
    const write = vi.fn().mockResolvedValue(undefined)
    clipboard(write)
    render(<CopyButton value="+65 9999 1111" label="Copy contact number" sensitive />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy contact number' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Copied.')
    expect(write).toHaveBeenCalledExactlyOnceWith('+65 9999 1111')
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
  })
  it('provides a selectable link when clipboard permission is denied', async () => {
    clipboard(vi.fn().mockRejectedValue(new Error('Denied')))
    render(<CopyButton value="https://example.test/groups/5" label="Copy group link" />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy group link' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Select and copy the link below.')
    expect(screen.getByText('https://example.test/groups/5')).toHaveClass('copy-fallback')
  })
  it('keeps a denied private copy out of fallback text and resets outcome when the value changes', async () => {
    clipboard(vi.fn().mockRejectedValue(new Error('Unavailable')))
    const page = render(<CopyButton value="private number" label="Copy contact number" sensitive />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy contact number' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Select the contact number above')
    expect(screen.queryByText('private number')).not.toBeInTheDocument()
    page.rerender(<CopyButton value="another number" label="Copy contact number" sensitive />)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
  it('does not announce an earlier value after a pending copy completes', async () => {
    const pending = deferred<void>()
    clipboard(() => pending.promise)
    const page = render(<CopyButton value="first" />)
    fireEvent.click(screen.getByRole('button', { name: 'Copy' }))
    expect(screen.getByRole('button', { name: 'Copying…' })).toBeDisabled()
    page.rerender(<CopyButton value="second" />)
    await act(async () => pending.resolve())
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
