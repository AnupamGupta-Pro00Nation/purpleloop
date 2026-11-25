export function getHourlyActivity(events) {
  const counts = Array.from({ length: 24 }, () => 0)
  let validTimestampCount = 0

  for (const event of events) {
    const timestamp = event.timestamp ?? event.time ?? event.createdAt
    if (!timestamp) continue

    const date = new Date(timestamp)
    if (Number.isNaN(date.getTime())) continue

    counts[date.getHours()] += 1
    validTimestampCount += 1
  }

  if (validTimestampCount === 0) return null

  return counts.map((count, hour) => ({
    time: `${String(hour).padStart(2, '0')}:00`,
    events: count,
  }))
}
