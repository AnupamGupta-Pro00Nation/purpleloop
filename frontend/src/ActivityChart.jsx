import { useMemo } from 'react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getHourlyActivity } from './activityData.js'

export default function ActivityChart({ events = [] }) {
  const activityData = useMemo(() => getHourlyActivity(events), [events])

  if (!activityData) {
    return (
      <div className="chart-empty-state">
        No valid event timestamps available for the chart.
      </div>
    )
  }

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart
        data={activityData}
        margin={{ top: 10, right: 5, left: -20, bottom: 0 }}
        accessibilityLayer
      >
        <defs>
          <linearGradient
            id="activityGradient"
            x1="0"
            y1="0"
            x2="0"
            y2="1"
          >
            <stop offset="0%" stopColor="#9b78ff" stopOpacity={0.32} />
            <stop offset="100%" stopColor="#9b78ff" stopOpacity={0} />
          </linearGradient>
        </defs>

        <CartesianGrid
          stroke="#272638"
          strokeDasharray="3 5"
          vertical={false}
        />
        <XAxis
          dataKey="time"
          tick={{ fill: '#77768d', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          dy={10}
        />
        <YAxis
          tick={{ fill: '#77768d', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip
          contentStyle={{
            background: '#191827',
            border: '1px solid #353149',
            borderRadius: '8px',
            color: '#f5f3ff',
            fontSize: '12px',
          }}
          labelStyle={{ color: '#b6a1ff' }}
        />
        <Area
          type="monotone"
          dataKey="events"
          name="Login events"
          stroke="#a58aff"
          strokeWidth={2.5}
          fill="url(#activityGradient)"
          activeDot={{ r: 5, fill: '#bdaaff' }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
