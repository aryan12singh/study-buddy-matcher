import { afterEach, expect, it, vi } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { renderPage, response } from '../../test/renderApp'
import { api } from '../../shared/api/client'
import RoomWeatherPanel from './RoomWeatherPanel'
import { campusNight } from './weatherTheme'

afterEach(() => { vi.restoreAllMocks(); localStorage.clear() })
it('uses Singapore daylight and lets a local atmosphere change without an external request', async () => {
  expect(campusNight(new Date('2026-10-14T00:00:00Z'))).toBe(false)
  expect(campusNight(new Date('2026-10-14T14:00:00Z'))).toBe(true)
  const get = vi.spyOn(api, 'get')
  const change = vi.fn()
  renderPage(<RoomWeatherPanel onChange={change} />)
  fireEvent.change(screen.getByLabelText('Window view'), { target: { value: 'STORM' } })
  expect(screen.getByText(/Selected atmosphere/)).toBeInTheDocument()
  expect(localStorage.getItem('study-room-atmosphere')).toBe('STORM')
  expect(change).toHaveBeenLastCalledWith('STORM')
  expect(get).not.toHaveBeenCalled()
})
it('labels live versus last-known data and keeps selectable themes available after provider failure', async () => {
  vi.spyOn(api, 'get').mockResolvedValue(response({ available:true,condition:'RAIN',temperatureCelsius:28,isDay:true,observedAt:'2026-10-14T00:00:00Z',stale:true,location:'SMU campus' }))
  renderPage(<RoomWeatherPanel liveEnabled />)
  expect(await screen.findByText(/Last known conditions/)).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Window view'), { target: { value:'CLEAR' } })
  expect(screen.getByText(/Selected atmosphere/)).toBeInTheDocument()
})
