import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import SharedHoursHeatmap from './SharedHoursHeatmap'

describe('shared time details', () => {
  it('provides a native disclosure with the same day-part totals as the heatmap', () => {
    render(<SharedHoursHeatmap slots={[
      { dayOfWeek: 'MONDAY', startTime: '11:00', endTime: '13:30' },
      { dayOfWeek: 'WEDNESDAY', startTime: '18:00', endTime: '20:00' },
    ]} />)

    const summary = screen.getByText('Shared time details')
    expect(summary.tagName).toBe('SUMMARY')
    expect(summary.parentElement?.tagName).toBe('DETAILS')
    const descriptions = within(summary.parentElement!).getAllByRole('listitem', { hidden: true })
    expect(descriptions.map(item => item.textContent)).toEqual([
      'Monday morning: 1 hour shared',
      'Monday afternoon: 1.5 hours shared',
      'Wednesday evening: 2 hours shared',
    ])
    const table = screen.getByRole('table')
    for (const description of descriptions) {
      expect(within(table).getByTitle(description.textContent!)).toBeInTheDocument()
    }
    expect(screen.getByText('All times are in SGT. Other day parts have no shared time.')).toBeInTheDocument()
  })

  it('updates the text when shared availability changes and explains an empty result', () => {
    const { rerender } = render(<SharedHoursHeatmap slots={[
      { dayOfWeek: 'TUESDAY', startTime: '09:00', endTime: '10:00' },
    ]} />)
    expect(screen.getByText('Tuesday morning: 1 hour shared')).toBeInTheDocument()

    rerender(<SharedHoursHeatmap slots={[]} />)
    expect(screen.queryByText('Tuesday morning: 1 hour shared')).not.toBeInTheDocument()
    expect(screen.getByText('No shared free time.')).toBeInTheDocument()
    expect(screen.queryByRole('listitem', { hidden: true })).not.toBeInTheDocument()
  })
})
