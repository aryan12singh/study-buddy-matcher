import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import Dialog from './Dialog'
import Button from './Button'

function DialogExample({ busy = false }: { busy?: boolean }) {
  const [open, setOpen] = useState(false)
  return <><Button onClick={() => setOpen(true)}>Open dialog</Button>{open && <Dialog title="Confirm action" onClose={() => setOpen(false)} busy={busy}><input aria-label="Message" /><Button>Confirm</Button></Dialog>}</>
}
describe('keyboard dialog access', () => {
  it('keeps the page locked until the last dialog closes, including out-of-order removal', () => {
    const previous = document.body.style.overflow
    document.body.style.overflow = 'auto'
    const view = render(<><Dialog key="first" title="First" onClose={() => {}}>First dialog</Dialog><Dialog key="second" title="Second" onClose={() => {}}>Second dialog</Dialog></>)
    try {
      expect(document.body.style.overflow).toBe('hidden')
      view.rerender(<><Dialog key="second" title="Second" onClose={() => {}}>Second dialog</Dialog></>)
      expect(document.body.style.overflow).toBe('hidden')
      view.unmount()
      expect(document.body.style.overflow).toBe('auto')
    } finally { view.unmount(); document.body.style.overflow = previous }
  })
  it('traps focus, closes on Escape and returns focus to the opener', () => {
    render(<DialogExample />)
    const opener = screen.getByRole('button', { name: 'Open dialog' })
    opener.focus(); fireEvent.click(opener)
    const close = screen.getByRole('button', { name: 'Close Confirm action' }), confirm = screen.getByRole('button', { name: 'Confirm' })
    expect(close).toHaveFocus()
    confirm.focus(); fireEvent.keyDown(confirm, { key: 'Tab' }); expect(close).toHaveFocus()
    fireEvent.keyDown(close, { key: 'Tab', shiftKey: true }); expect(confirm).toHaveFocus()
    fireEvent.keyDown(confirm, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })
  it('does not dismiss a pending action with Escape', () => {
    render(<DialogExample busy />)
    fireEvent.click(screen.getByRole('button', { name: 'Open dialog' }))
    expect(screen.getByRole('button', { name: 'Close Confirm action' })).toBeDisabled()
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })
})
