import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

const activityData = [
  { time: '00:00', events: 2 },
  { time: '04:00', events: 4 },
  { time: '08:00', events: 3 },
  { time: '12:00', events: 7 },
  { time: '16:00', events: 5 },
  { time: '20:00', events: 9 },
  { time: '23:59', events: 6 },
]

export default function ActivityChart() {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart
        data={activityData}
        margin={{ top: 10, right: 5, left: -20, bottom: 0 }}
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
          name="Illustrative events"
          stroke="#a58aff"
          strokeWidth={2.5}
          fill="url(#activityGradient)"
          activeDot={{ r: 5, fill: '#bdaaff' }}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
