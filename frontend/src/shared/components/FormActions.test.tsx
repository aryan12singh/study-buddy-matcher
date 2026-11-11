import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import FormActions from './FormActions'

describe('FormActions', () => {
  it('reports the save state and keeps its buttons after it, in reading and tab order', () => {
    const { rerender } = render(<FormActions dirty={false} pending={false}><button type="submit">Save profile</button></FormActions>)
    const status = screen.getByRole('status')
    const save = screen.getByRole('button', { name: 'Save profile' })
    expect(status).toHaveTextContent('Ready to save')
    expect(status.compareDocumentPosition(save) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()

    rerender(<FormActions dirty pending={false}><button type="submit">Save profile</button></FormActions>)
    expect(status).toHaveTextContent('Unsaved changes')

    rerender(<FormActions dirty pending><button type="submit">Save profile</button></FormActions>)
    expect(status).toHaveTextContent('Saving changes…')
  })
})
