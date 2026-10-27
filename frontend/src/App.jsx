import { useCallback, useEffect, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  CircleDot,
  Clock3,
  Crosshair,
  Database,
  Fingerprint,
  Gauge,
  Globe,
  LayoutDashboard,
  LoaderCircle,
  Play,
  RefreshCw,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Terminal,
  Wifi,
  XCircle,
} from 'lucide-react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import './App.css'

const API = {
  health: '/api/health',
  metrics: '/api/metrics',
  events: '/api/events',
  simulate: '/api/simulate/brute-force',
}

const activityData = [
  { time: '00:00', events: 2 },
  { time: '04:00', events: 4 },
  { time: '08:00', events: 3 },
  { time: '12:00', events: 7 },
  { time: '16:00', events: 5 },
  { time: '20:00', events: 9 },
  { time: '23:59', events: 6 },
]

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options)

  if (!response.ok) {
    throw new Error(`Request failed: ${url} (HTTP ${response.status})`)
  }

  return response.json()
}

function MetricCard({ title, value, description, icon: Icon, accent }) {
  return (
    <article className="metric-card">
      <div className={`metric-icon ${accent}`}>
        <Icon size={19} />
      </div>

      <div className="metric-heading">
        <span>{title}</span>
        <span className="metric-dot" />
      </div>

      <div className="metric-value">{value ?? '—'}</div>
      <div className="metric-description">{description}</div>
    </article>
  )
}

function StatusBadge({ online }) {
  return (
    <span className={`status-badge ${online ? 'online' : 'offline'}`}>
      <span className="status-dot" />
      {online ? 'API Connected' : 'API Offline'}
    </span>
  )
}

function SeverityBadge({ severity }) {
  const value = severity || 'UNKNOWN'

  return (
    <span className={`severity-badge ${value.toLowerCase()}`}>
      <span className="severity-dot" />
      {value}
    </span>
  )
}

function App() {
  const [health, setHealth] = useState('checking')
  const [metrics, setMetrics] = useState(null)
  const [events, setEvents] = useState([])
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [simulating, setSimulating] = useState(false)
  const [error, setError] = useState('')
  const [simulationMessage, setSimulationMessage] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)

  const loadDashboard = useCallback(async () => {
    setError('')

    try {
      await fetchJson(API.health)
      setHealth('online')

      const [metricsData, eventsData] = await Promise.all([
        fetchJson(API.metrics),git status
        fetchJson(API.events),
      ])

      setMetrics(metricsData)
      setEvents(Array.isArray(eventsData) ? eventsData : [])

      
      setLastUpdated(new Date())
    } catch (err) {
      setHealth('offline')
      setError(
        `${err.message}. Make sure the PurpleLoop backend is running on port 8080.`,
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  async function runSimulation() {
    if (simulating) return

    setSimulating(true)
    setError('')
    setSimulationMessage('Running brute-force simulation...')

    try {
      const results = await fetchJson(API.simulate, {
        method: 'POST',
      })

      const detectionResults = Array.isArray(results) ? results : []
      setAlerts(detectionResults)

      const [metricsData, eventsData] = await Promise.all([
        fetchJson(API.metrics),
        fetchJson(API.events),
      ])

      setMetrics(metricsData)
      setEvents(Array.isArray(eventsData) ? eventsData : [])
      setLastUpdated(new Date())

      if (detectionResults.length > 0) {
        const count = detectionResults.length

        setSimulationMessage(
          `Simulation complete: ${count} detection alert${count === 1 ? '' : 's'} generated.`,
        )
      } else {
        setSimulationMessage(
          'Simulation completed. No detection alerts were generated.',
        )
      }
    } catch (err) {
      setSimulationMessage('')
      setError(
        `${err.message}. Check that the Java backend is running on port 8080.`,
      )
    } finally {
      setSimulating(false)
    }
  }

  const failedLogins = events.filter(
    (event) => event.success === false,
  ).length

  function formatTime(timestamp) {
    if (!timestamp) return '—'

    const date = new Date(timestamp)

    if (Number.isNaN(date.getTime())) return String(timestamp)

    return date.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Shield size={24} />
          </div>

          <div className="brand-copy">
            <h1>PurpleLoop</h1>
            <span>DETECTION ENGINEERING</span>
          </div>
        </div>

        <div className="workspace-label">WORKSPACE</div>

        <nav className="sidebar-nav">
          <a className="nav-link active" href="#overview">
            <LayoutDashboard size={18} />
            <span>Overview</span>
          </a>

          <a className="nav-link" href="#events">
            <Activity size={18} />
            <span>Event Telemetry</span>
          </a>

          <a className="nav-link" href="#detections">
            <ShieldAlert size={18} />
            <span>Detections</span>
          </a>

          <a className="nav-link" href="#mitre">
            <Crosshair size={18} />
            <span>MITRE ATT&CK</span>
          </a>
        </nav>

        <div className="sidebar-bottom">
          <div className="environment-card">
            <div className="environment-heading">
              <span className="environment-dot" />
              LOCAL ENVIRONMENT
            </div>

            <div className="environment-row">
              <span>Backend API</span>
              <span>8080</span>
            </div>

            <div className="environment-row">
              <span>Frontend</span>
              <span>5173</span>
            </div>

            <div className="environment-row">
              <span>Mode</span>
              <span className="simulation-tag">SIMULATION</span>
            </div>
          </div>

          <div className="sidebar-footer">
            <div className="footer-avatar">
              <Fingerprint size={19} />
            </div>
            <div>
              <strong>PurpleLoop Engine</strong>
              <span>Detection &amp; Response</span>
            </div>
          </div>
        </div>
      </aside>

      <main className="main-content" id="overview">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <span className="breadcrumb-separator">/</span>
            <strong>Overview</strong>
          </div>

          <div className="topbar-right">
            <StatusBadge online={health === 'online'} />

            <button
              className="icon-button"
              onClick={loadDashboard}
              title="Refresh dashboard"
              aria-label="Refresh dashboard"
              disabled={loading}
            >
              <RefreshCw size={17} className={loading ? 'spin' : ''} />
            </button>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <div className="eyebrow">
              <span className="eyebrow-line" />
              SECURITY OPERATIONS
            </div>

            <h2>
              Purple Team <span>Overview</span>
            </h2>

            <p>
              Simulate attack behavior, validate detection rules, and measure
              defensive visibility.
            </p>
          </div>

          <button
            className="primary-button"
            onClick={runSimulation}
            disabled={simulating || health !== 'online'}
          >
            {simulating ? (
              <LoaderCircle size={17} className="spin" />
            ) : (
              <Play size={17} fill="currentColor" />
            )}

            {simulating ? 'Running Simulation...' : 'Run Attack Simulation'}
          </button>
        </section>

        {error && (
          <div className="alert-banner error-banner" role="alert">
            <XCircle size={19} />
            <span>{error}</span>
            <button
              onClick={() => setError('')}
              aria-label="Dismiss error"
            >
              ×
            </button>
          </div>
        )}

        {simulationMessage && (
          <div
            className={`alert-banner ${simulationMessage.startsWith('Simulation complete')
                ? 'success-banner'
                : 'info-banner'
              }`}
            role="status"
          >
            {simulating ? (
              <LoaderCircle size={19} className="spin" />
            ) : (
              <CheckCircle2 size={19} />
            )}

            <span>{simulationMessage}</span>

            <button
              onClick={() => setSimulationMessage('')}
              aria-label="Dismiss message"
            >
              ×
            </button>
          </div>
        )}

        <section className="metrics-grid">
          <MetricCard
            title="Total Events"
            value={metrics?.totalEvents}
            description="Events in current dataset"
            icon={Database}
            accent="purple"
          />

          <MetricCard
            title="Detection Alerts"
            value={metrics?.totalAlerts}
            description="Alerts returned by API"
            icon={ShieldAlert}
            accent="red"
          />

          <MetricCard
            title="Unique Source IPs"
            value={metrics?.uniqueSourceIps}
            description="Distinct event sources"
            icon={Globe}
            accent="blue"
          />

          <MetricCard
            title="Failed Logins"
            value={failedLogins}
            description="Failed authentication events"
            icon={Fingerprint}
            accent="orange"
          />
        </section>

        <section className="dashboard-grid">
          <article className="panel activity-panel">
            <div className="panel-header">
              <div>
                <div className="panel-title">
                  <Activity size={18} />
                  <h3>Event Activity</h3>
                </div>
                <p>Illustrative activity distribution across the day</p>
              </div>

              <span className="panel-badge">
                <CircleDot size={12} />
                ACTIVITY
              </span>
            </div>

            <div className="chart-summary">
              <div>
                <span className="chart-total">
                  {metrics?.totalEvents ?? '—'}
                </span>
                <span className="chart-total-label">Current dataset events</span>
              </div>

              <div className="chart-legend">
                <span className="legend-dot" />
                Event activity
              </div>
            </div>

            <div className="chart-container">
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
                      <stop
                        offset="0%"
                        stopColor="#9b78ff"
                        stopOpacity={0.32}
                      />
                      <stop
                        offset="100%"
                        stopColor="#9b78ff"
                        stopOpacity={0}
                      />
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
            </div>

            <div className="chart-note">
              <AlertTriangle size={14} />
              <span>
                Chart data is illustrative. It is not a live time-series feed
                from the backend.
              </span>
            </div>
          </article>

          <article className="panel detection-panel" id="detections">
            <div className="panel-header">
              <div>
                <div className="panel-title">
                  <ShieldAlert size={18} />
                  <h3>Detection Results</h3>
                </div>
                <p>Behavioral detection engine output</p>
              </div>

              <span className="result-count">
                {alerts.length} {alerts.length === 1 ? 'ALERT' : 'ALERTS'}
              </span>
            </div>

            {alerts.length > 0 ? (
              <div className="detection-list">
                {alerts.map((alert, index) => (
                  <div
                    className="detection-card"
                    key={`${alert.ruleName}-${alert.sourceIp}-${index}`}
                  >
                    <div className="detection-card-top">
                      <div className="detection-icon">
                        <ShieldAlert size={19} />
                      </div>

                      <SeverityBadge severity={alert.severity} />
                    </div>

                    <h4>{alert.ruleName}</h4>

                    <div className="detection-details">
                      <div className="detail-row">
                        <span>Source IP</span>
                        <code>{alert.sourceIp}</code>
                      </div>

                      <div className="detail-row">
                        <span>Failed attempts</span>
                        <strong>{alert.failedAttempts}</strong>
                      </div>

                      <div className="detail-row">
                        <span>MITRE technique</span>
                        <code>{alert.mitreTechnique}</code>
                      </div>
                    </div>

                    <div className="detection-footer">
                      <CheckCircle2 size={14} />
                      Detection rule matched
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state">
                <ShieldCheck size={32} />
                <h4>No alerts returned</h4>
                <p>
                  Run the attack simulation to test the detection engine.
                </p>
              </div>
            )}

            <button
              className="secondary-button full-width"
              onClick={runSimulation}
              disabled={simulating || health !== 'online'}
            >
              {simulating ? (
                <LoaderCircle size={16} className="spin" />
              ) : (
                <Terminal size={16} />
              )}
              Test Detection Rule
            </button>
          </article>
        </section>

        <section className="lower-grid">
          <article className="panel events-panel" id="events">
            <div className="panel-header">
              <div>
                <div className="panel-title">
                  <Database size={18} />
                  <h3>Recent Login Events</h3>
                </div>
                <p>Authentication telemetry from the backend</p>
              </div>

              <span className="table-count">
                {events.length} EVENTS
              </span>
            </div>

            {events.length > 0 ? (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>EVENT</th>
                      <th>SOURCE IP</th>
                      <th>RESULT</th>
                      <th>TIMESTAMP</th>
                    </tr>
                  </thead>

                  <tbody>
                    {events.map((event, index) => {
                      const successful =
                        event.success === true ||
                        event.success === 'true'

                      return (
                        <tr key={event.id ?? `${event.ip}-${index}`}>
                          <td>
                            <div className="event-name">
                              <span
                                className={`event-status ${successful ? 'passed' : 'failed'
                                  }`}
                              >
                                {successful ? (
                                  <CheckCircle2 size={15} />
                                ) : (
                                  <XCircle size={15} />
                                )}
                              </span>

                              <span>Login Attempt</span>
                            </div>
                          </td>

                          <td>
                            <code>{event.ip ?? 'Unknown'}</code>
                          </td>

                          <td>
                            <span
                              className={`result-pill ${successful ? 'success' : 'failure'
                                }`}
                            >
                              {successful ? 'SUCCESS' : 'FAILED'}
                            </span>
                          </td>

                          <td className="timestamp">
                            <Clock3 size={13} />
                            {formatTime(
                              event.timestamp ??
                              event.time ??
                              event.createdAt,
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="empty-state compact">
                <Database size={25} />
                <p>
                  {loading
                    ? 'Loading events...'
                    : 'No login events available.'}
                </p>
              </div>
            )}

            <div className="table-footer">
              <span>
                <span className="live-dot" />
                Data retrieved from PurpleLoop API
              </span>

              <button onClick={loadDashboard}>
                <RefreshCw size={13} />
                Refresh
              </button>
            </div>
          </article>

          <article className="panel mitre-panel" id="mitre">
            <div className="panel-header">
              <div>
                <div className="panel-title">
                  <Crosshair size={18} />
                  <h3>MITRE ATT&amp;CK</h3>
                </div>
                <p>Detection technique mapping</p>
              </div>
            </div>

            {alerts.length > 0 ? (
              <div className="mitre-content">
                <div className="mitre-technique">
                  <div className="mitre-icon">
                    <Crosshair size={22} />
                  </div>

                  <div>
                    <span className="mitre-label">TECHNIQUE ID</span>
                    <h4>{alerts[0].mitreTechnique || 'Unmapped'}</h4>
                  </div>
                </div>

                <div className="mitre-name">
                  <span>Technique</span>
                  <strong>
                    {alerts[0].mitreTechnique === 'T1110'
                      ? 'Brute Force'
                      : 'Detection technique'}
                  </strong>
                </div>

                <div className="mitre-name">
                  <span>Detection rule</span>
                  <strong>{alerts[0].ruleName}</strong>
                </div>

                <div className="mitre-name">
                  <span>Severity</span>
                  <SeverityBadge severity={alerts[0].severity} />
                </div>

                <div className="mitre-footer">
                  <CheckCircle2 size={15} />
                  Technique mapped by detection output
                </div>
              </div>
            ) : (
              <div className="empty-state compact">
                <Crosshair size={25} />
                <p>Run a simulation to display technique mapping.</p>
              </div>
            )}
          </article>
        </section>

        <section className="pipeline-panel">
          <div className="pipeline-heading">
            <div className="panel-title">
              <Gauge size={18} />
              <h3>Detection Pipeline</h3>
            </div>
            <span>PROCESS FLOW</span>
          </div>

          <div className="pipeline-flow">
            <div className="pipeline-step">
              <div className="pipeline-icon telemetry">
                <Wifi size={19} />
              </div>
              <div>
                <strong>Telemetry</strong>
                <span>Login events</span>
              </div>
            </div>

            <div className="pipeline-connector" />

            <div className="pipeline-step">
              <div className="pipeline-icon engine">
                <Terminal size={19} />
              </div>
              <div>
                <strong>Detection Engine</strong>
                <span>Threshold-based rule</span>
              </div>
            </div>

            <div className="pipeline-connector" />

            <div className="pipeline-step">
              <div className="pipeline-icon alert">
                <AlertTriangle size={19} />
              </div>
              <div>
                <strong>Alert Generation</strong>
                <span>Severity and technique</span>
              </div>
            </div>

            <div className="pipeline-connector" />

            <div className="pipeline-step">
              <div className="pipeline-icon measurement">
                <Gauge size={19} />
              </div>
              <div>
                <strong>Measurement</strong>
                <span>Coverage and MTTD planned</span>
              </div>
            </div>
          </div>
        </section>

        <footer className="app-footer">
          <div>
            <span className="footer-status-dot" />
            PURPLELOOP <span className="footer-divider">/</span> LOCAL
            ENVIRONMENT
          </div>

          <div className="footer-right">
            <span>
              {lastUpdated
                ? `Last updated ${lastUpdated.toLocaleTimeString()}`
                : 'Waiting for API'}
            </span>
            <span className="footer-divider">·</span>
            <span>Java Backend + React</span>
          </div>
        </footer>
      </main>
    </div>
  )
}

export default App