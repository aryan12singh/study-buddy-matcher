import { describe, expect, it } from 'vitest'
import { campusWeekday, feedTime, isToday, timeAgo, todayLabel } from './homeTime'

// 14:00 on Friday 9 October in Singapore.
const now = new Date('2026-10-09T06:00:00Z')

describe('home time labels', () => {
  it('names the campus day', () => {
    expect(todayLabel(now)).toBe('Friday, 9 October')
    expect(campusWeekday(now)).toBe('FRIDAY')
    // 23:30 UTC on the 8th is already the 9th in Singapore.
    expect(campusWeekday(new Date('2026-10-08T23:30:00Z'))).toBe('FRIDAY')
  })
  it('compares days in campus time, not UTC', () => {
    expect(isToday('2026-10-08T17:00:00Z', now)).toBe(true)
    expect(isToday('2026-10-08T15:00:00Z', now)).toBe(false)
  })
  it('says how long ago a request arrived', () => {
    expect(timeAgo('2026-10-09T05:59:30Z', now)).toBe('Just now')
    expect(timeAgo('2026-10-09T05:59:00Z', now)).toBe('1 minute ago')
    expect(timeAgo('2026-10-09T04:00:00Z', now)).toBe('2 hours ago')
    expect(timeAgo('2026-10-08T10:00:00Z', now)).toBe('Yesterday')
    expect(timeAgo('2026-10-06T10:00:00Z', now)).toBe('6 Oct')
  })
  it('stamps activity with a 24-hour campus time', () => {
    expect(feedTime('2026-10-09T04:20:00Z', now)).toBe('12:20')
    expect(feedTime('2026-10-08T10:42:00Z', now)).toBe('Yesterday, 18:42')
    expect(feedTime('2026-10-06T13:10:00Z', now)).toBe('6 Oct, 21:10')
  })
})
