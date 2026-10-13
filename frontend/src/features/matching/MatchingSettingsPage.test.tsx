import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { adminAccount, renderPage, response } from '../../test/renderApp'
import MatchingSettingsPage from './MatchingSettingsPage'
import type { MatchingConfigUpdate } from './api'

const balanced = { COURSE: 0.3, AVAILABILITY: 0.3, STUDY_MODE: 0.2, STUDY_GOAL: 0.1, GROUP_SIZE: 0.1 }
const availabilityFirst = { AVAILABILITY: 0, COURSE: 0.4, STUDY_MODE: 0.3, STUDY_GOAL: 0.15, GROUP_SIZE: 0.15 }
const courseFirst = { COURSE: 0, AVAILABILITY: 0.4, STUDY_MODE: 0.3, STUDY_GOAL: 0.15, GROUP_SIZE: 0.15 }
const saved = { activeStrategy: 'BALANCED', version: 1, usingDefaults: true, minScore: 0, enabledCriteria: ['COURSE', 'AVAILABILITY', 'STUDY_MODE', 'STUDY_GOAL', 'GROUP_SIZE'], weights: { BALANCED: balanced, AVAILABILITY_FIRST: availabilityFirst, COURSE_FIRST: courseFirst } }
afterEach(() => { vi.restoreAllMocks() })

/** Matches the running total paragraph, whose number sits in its own element. */
function totalLine(text: string) {
  return (_: string, element: Element | null) => element?.tagName === 'P' && element.textContent === text
}

function renderSettings() {
  return renderPage(<MatchingSettingsPage />, '/admin/matching', '/admin/matching', adminAccount)
}

function weightValue(label: string) {
  return Number((screen.getByLabelText(label) as HTMLInputElement).value)
}

describe('admin matching settings', () => {
  it('shows the saved strategy, weights and each weight\'s share', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    renderSettings()
    expect(await screen.findByLabelText('Course')).toHaveValue(0.3)
    expect(screen.getByRole('radio', { name: /Balanced/ })).toBeChecked()
    expect(screen.getAllByText('30% of the total score')).toHaveLength(2)
    expect(screen.getByText('Ready to save')).toBeInTheDocument()
  })

  it('saves the chosen strategy and weights as numbers', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    // Course 0.4 leaves 0.6 for the others, shared in their saved 3 : 2 : 1 : 1 proportions.
    const updated = { ...saved, activeStrategy: 'COURSE_FIRST', version: 2, usingDefaults: false, weights: {
      BALANCED: { COURSE: 0.4, AVAILABILITY: 9 / 35, STUDY_MODE: 6 / 35, STUDY_GOAL: 3 / 35, GROUP_SIZE: 3 / 35 },
      AVAILABILITY_FIRST: availabilityFirst,
      COURSE_FIRST: courseFirst
    } }
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(updated))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '0.4' } })
    fireEvent.click(screen.getByRole('radio', { name: /Course first/ }))
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/admin/matching-config', {
      activeStrategy: updated.activeStrategy, enabledCriteria: saved.enabledCriteria, minScore: 0, expectedVersion: 1,
      weights: { ...updated.weights, BALANCED: {
        COURSE: 0.4, AVAILABILITY: expect.closeTo(9 / 35, 14), STUDY_MODE: expect.closeTo(6 / 35, 14),
        STUDY_GOAL: expect.closeTo(3 / 35, 14), GROUP_SIZE: expect.closeTo(3 / 35, 14)
      } }
    }))
    expect(await screen.findByText(/Matching settings saved/)).toBeInTheDocument()
    expect(screen.getByText('Ready to save')).toBeInTheDocument()
  })

  it('rejects a blank, negative or above-1 weight next to its field without saving', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    const put = vi.spyOn(api, 'put')
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Study mode'), { target: { value: '-1' } })
    fireEvent.change(screen.getByLabelText('Group size'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(screen.getByText('Enter a weight for study mode from 0 to 1.')).toBeInTheDocument()
    expect(screen.getByText('Enter a weight for group size from 0 to 1.')).toBeInTheDocument()
    expect(screen.getByText('Enter a weight for course from 0 to 1.')).toBeInTheDocument()
    expect(screen.getByLabelText('Study mode')).toHaveAttribute('aria-invalid', 'true')
    expect(put).not.toHaveBeenCalled()
  })

  it('shows all five weights including each strategy priority criterion', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    renderSettings()
    expect(await screen.findByLabelText('Course')).toHaveValue(0.3)
    fireEvent.click(screen.getByRole('radio', { name: /Availability first/ }))
    expect(screen.getByLabelText('Course')).toHaveValue(0.4)
    expect(screen.getByLabelText('Availability overlap')).toHaveValue(0)
    expect(screen.getByLabelText('Enable availability overlap')).toBeChecked()
    fireEvent.click(screen.getByRole('radio', { name: /Course first/ }))
    expect(screen.getByLabelText('Course')).toHaveValue(0)
    expect(screen.getByLabelText('Availability overlap')).toHaveValue(0.4)
  })

  it('changes only the selected strategy\'s weights', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    renderSettings()
    fireEvent.click(await screen.findByRole('radio', { name: /Availability first/ }))
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '0.7' } })
    // The other three keep their 2 : 1 : 1 proportions of the remaining 0.3.
    expect(weightValue('Study mode')).toBeCloseTo(0.15, 14)
    expect(weightValue('Study goals')).toBeCloseTo(0.075, 14)
    expect(weightValue('Group size')).toBeCloseTo(0.075, 14)
    fireEvent.click(screen.getByRole('radio', { name: /Balanced/ }))
    expect(screen.getByLabelText('Course')).toHaveValue(0.3)
  })

  it('adjusts the other weights so the total stays at 1', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '0.7' } })
    // The remaining 0.3 keeps the others' 3 : 2 : 1 : 1 proportions.
    expect(weightValue('Availability overlap')).toBeCloseTo(9 / 70, 14)
    expect(weightValue('Study mode')).toBeCloseTo(3 / 35, 14)
    expect(weightValue('Study goals')).toBeCloseTo(3 / 70, 14)
    expect(weightValue('Group size')).toBeCloseTo(3 / 70, 14)
    expect(screen.getByText(totalLine('Total: 1 (adds up to 1)'))).toBeInTheDocument()
  })

  it('clears an earlier error as soon as a weight changes', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(screen.getByText('Enter a weight for course from 0 to 1.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '0.5' } })
    expect(screen.queryByText('Enter a weight for course from 0 to 1.')).not.toBeInTheDocument()
  })

  it('shows the server\'s reason when saving fails', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    vi.spyOn(api, 'put').mockRejectedValue(Object.assign(new Error('Request failed'), {
      isAxiosError: true,
      response: { status: 400, data: { message: 'A weight for COURSE is required' } }
    }))
    renderSettings()
    await screen.findByLabelText('Course')
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(await screen.findByText('A weight for COURSE is required')).toBeInTheDocument()
  })

  it('offers a retry when the settings cannot be loaded', async () => {
    const get = vi.spyOn(api, 'get').mockRejectedValueOnce(new Error('Network down')).mockResolvedValue(response(saved))
    renderSettings()
    expect(await screen.findByText('Unable to load this view')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByLabelText('Course')).toHaveValue(0.3)
    expect(get).toHaveBeenCalledTimes(2)
  })

  it.each([
    { name: 'fine-grained weights', weights: { COURSE: 0.004, AVAILABILITY: 0.996, STUDY_MODE: 0, STUDY_GOAL: 0, GROUP_SIZE: 0 } },
    { name: 'equal thirds', weights: { COURSE: 1 / 3, AVAILABILITY: 1 / 3, STUDY_MODE: 1 / 3, STUDY_GOAL: 0, GROUP_SIZE: 0 } }
  ])('saves fetched $name unchanged without editing', async ({ weights }) => {
    const precise = { ...saved, weights: { ...saved.weights, BALANCED: weights } }
    vi.spyOn(api, 'get').mockResolvedValue(response(precise))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response(precise))
    renderSettings()
    expect(await screen.findByLabelText('Course')).toHaveValue(weights.COURSE)
    expect(screen.getByLabelText('Availability overlap')).toHaveValue(weights.AVAILABILITY)
    expect(screen.getByText(totalLine('Total: 1 (adds up to 1)'))).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/admin/matching-config', { activeStrategy: precise.activeStrategy, weights: precise.weights, enabledCriteria: precise.enabledCriteria, minScore: precise.minScore, expectedVersion: precise.version }))
    expect(await screen.findByText(/Matching settings saved/)).toBeInTheDocument()
    expect(screen.getByLabelText('Course')).toHaveValue(weights.COURSE)
    expect(screen.getByText('Ready to save')).toBeInTheDocument()
  })

  it('saves a fine-grained edit and its normalized proportional weights', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    const put = vi.spyOn(api, 'put').mockImplementation((_url, config) => Promise.resolve(response({ ...(config as MatchingConfigUpdate), version: 2, usingDefaults: false })))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '0.004' } })
    expect(screen.getByLabelText('Course')).toHaveValue(0.004)
    expect(screen.getByLabelText('Course')).toHaveAttribute('step', 'any')
    expect(screen.getByText('0.4% of the total score')).toBeInTheDocument()
    expect(weightValue('Availability overlap')).toBeCloseTo(0.996 * 3 / 7, 14)
    expect(weightValue('Study mode')).toBeCloseTo(0.996 * 2 / 7, 14)
    expect(screen.getByText(totalLine('Total: 1 (adds up to 1)'))).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    await waitFor(() => expect(put).toHaveBeenCalledTimes(1))
    const posted = put.mock.calls[0][1] as MatchingConfigUpdate
    expect(posted.weights.BALANCED.COURSE).toBe(0.004)
    expect(Object.values(posted.weights.BALANCED).reduce((sum, value) => sum + value, 0)).toBeCloseTo(1, 14)
    expect(posted.weights.AVAILABILITY_FIRST).toEqual(availabilityFirst)
    expect(posted.weights.COURSE_FIRST).toEqual(courseFirst)
    expect(await screen.findByText(/Matching settings saved/)).toBeInTheDocument()
    expect(screen.getByLabelText('Course')).toHaveValue(0.004)
  })

  it('disables a priority criterion globally and normalizes only enabled weights', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    const put = vi.spyOn(api, 'put').mockImplementation((_url, config) => Promise.resolve(response({ ...(config as MatchingConfigUpdate), version: 2, usingDefaults: false })))
    renderSettings()
    await screen.findByLabelText('Course')
    fireEvent.click(screen.getByRole('radio', { name: /Availability first/ }))
    fireEvent.click(screen.getByLabelText('Enable availability overlap'))
    expect(screen.getByLabelText('Availability overlap')).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Course'), { target: { value: '0.5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    await waitFor(() => expect(put).toHaveBeenCalledTimes(1))
    const config = put.mock.calls[0][1] as MatchingConfigUpdate
    expect(config.enabledCriteria).not.toContain('AVAILABILITY')
    Object.values(config.weights).forEach(weights => {
      expect(weights.AVAILABILITY).toBe(0)
      expect(Object.values(weights).reduce((sum, weight) => sum + weight, 0)).toBeCloseTo(1, 14)
    })
    expect(await screen.findByText(/Saved configuration version 2/)).toBeInTheDocument()
    expect(screen.getByText(/Saved database override/)).toBeInTheDocument()
  })

  it('rejects no enabled criteria and an invalid minimum score without saving', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    const put = vi.spyOn(api, 'put')
    renderSettings()
    await screen.findByLabelText('Course')
    screen.getAllByRole('checkbox').forEach(checkbox => { fireEvent.click(checkbox) })
    fireEvent.change(screen.getByLabelText('Minimum compatibility score'), { target: { value: '1.2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(screen.getByText('Enable at least one matching criterion.')).toBeInTheDocument()
    expect(screen.getByText('Enter a minimum compatibility score from 0 to 1.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
  })

  it('keeps edits after a stale-version failure and explicitly reloads the current settings', async () => {
    const latest = { ...saved, version: 2, usingDefaults: false, minScore: 0.7 }
    vi.spyOn(api, 'get').mockResolvedValueOnce(response(saved)).mockResolvedValue(response(latest))
    const put = vi.spyOn(api, 'put').mockRejectedValue(Object.assign(new Error('Request failed'), { isAxiosError: true, response: { status: 409, data: { message: 'Matching settings changed. Reload the current version.' } } }))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Minimum compatibility score'), { target: { value: '0.5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(await screen.findByText('Matching settings changed. Reload the current version.')).toBeInTheDocument()
    expect(screen.getByLabelText('Minimum compatibility score')).toHaveValue(0.5)
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
    expect(put.mock.calls[0][1]).toEqual(expect.objectContaining({ minScore: 0.5, expectedVersion: 1 }))
    fireEvent.click(screen.getByRole('button', { name: 'Reload saved settings' }))
    const dialog = screen.getByRole('dialog', { name: 'Reload saved settings?' })
    expect(dialog).toHaveTextContent('Your unsaved edits will be discarded.')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Reload settings' }))
    expect(await screen.findByText(/Saved configuration version 2/)).toBeInTheDocument()
    expect(screen.getByLabelText('Minimum compatibility score')).toHaveValue(0.7)
    expect(screen.getByText('Ready to save')).toBeInTheDocument()
    expect(put).toHaveBeenCalledTimes(1)
  })

  it('restores startup defaults only after confirmation and uses the returned version', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...saved, usingDefaults: false }))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...saved, version: 3, usingDefaults: true }))
    const put = vi.spyOn(api, 'put').mockResolvedValue(response({ ...saved, version: 4, usingDefaults: false }))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Minimum compatibility score'), { target: { value: '0.8' } })
    fireEvent.click(screen.getByRole('button', { name: 'Restore startup defaults' }))
    const dialog = screen.getByRole('dialog', { name: 'Restore startup defaults?' })
    expect(dialog).toHaveTextContent('Your unsaved edits will be discarded.')
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Restore defaults' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/matching-config/restore-defaults', { expectedVersion: 1 }))
    expect(await screen.findByText(/Saved configuration version 3/)).toBeInTheDocument()
    expect(screen.getByLabelText('Minimum compatibility score')).toHaveValue(0)
    expect(screen.getByText('Ready to save')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    await waitFor(() => expect(put).toHaveBeenCalledWith('/admin/matching-config', expect.objectContaining({ expectedVersion: 3 })))
  })

  it('preserves the last valid saved configuration and edits when restoration fails', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    vi.spyOn(api, 'post').mockRejectedValue(new Error('Restore failed'))
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Minimum compatibility score'), { target: { value: '0.8' } })
    fireEvent.click(screen.getByRole('button', { name: 'Restore startup defaults' }))
    const dialog = screen.getByRole('dialog', { name: 'Restore startup defaults?' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Restore defaults' }))
    expect(await within(dialog).findByText('Restore failed')).toBeInTheDocument()
    expect(screen.getByLabelText('Minimum compatibility score')).toHaveValue(0.8)
    expect(screen.getByText(/Saved configuration version 1/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(screen.getByText('Unsaved changes')).toBeInTheDocument()
  })


  it('blocks a half-typed value left in another strategy before saving', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(saved))
    const put = vi.spyOn(api, 'put')
    renderSettings()
    fireEvent.change(await screen.findByLabelText('Course'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('radio', { name: /Course first/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Save matching settings' }))
    expect(screen.getByText('Check the Balanced weights: enabled criteria must have valid weights that add up to 1.')).toBeInTheDocument()
    expect(put).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('radio', { name: /Balanced/ }))
    expect(screen.getByLabelText('Course')).toHaveValue(null)
  })

})
