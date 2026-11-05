import { fireEvent, render, screen } from '@testing-library/react'
import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../../shared/api/client'
import CalendarButton from './CalendarButton'

describe('CalendarButton', () => {
  afterEach(() => vi.restoreAllMocks())

  it("shows the server's reason when the download is refused", async () => {
    const body = new Blob([JSON.stringify({ message: 'You are no longer in this study session' })], { type: 'application/json' })
    vi.spyOn(api, 'get').mockRejectedValue(new AxiosError('Not found', 'ERR_BAD_REQUEST', undefined, undefined,
      { config: { headers: new AxiosHeaders() }, status: 404, statusText: 'Not Found', headers: {}, data: body }))
    render(<CalendarButton sessionId={11} />)

    fireEvent.click(screen.getByRole('button', { name: 'Download calendar (.ics)' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You are no longer in this study session')
  })
})
