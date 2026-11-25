import { describe, expect, it } from 'vitest'
import { getHourlyActivity } from './activityData.js'

describe('getHourlyActivity', () => {
  it('groups actual event timestamps by hour and ignores invalid timestamps', () => {
    const timestamp = new Date(2026, 0, 1, 13, 30).toISOString()
    const hour = new Date(timestamp).getHours()
    const activity = getHourlyActivity([
      { timestamp },
      { timestamp },
      { timestamp: 'not-a-timestamp' },
    ])

    expect(activity).toHaveLength(24)
    expect(activity[hour]).toEqual({
      time: `${String(hour).padStart(2, '0')}:00`,
      events: 2,
    })
    expect(activity.reduce((total, point) => total + point.events, 0)).toBe(2)
  })

  it('returns no chart data when there are no valid timestamps', () => {
    expect(getHourlyActivity([])).toBeNull()
    expect(getHourlyActivity([{ timestamp: 'invalid' }])).toBeNull()
  })
})
