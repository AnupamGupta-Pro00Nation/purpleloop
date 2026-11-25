import {
  lazy,
  Suspense,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
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
  Users,
  Wifi,
  XCircle,
} from 'lucide-react'
import './App.css'

const ActivityChart = lazy(() => import('./ActivityChart.jsx'))

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/+$/, '')

const buildApiUrl = (path) => `${API_BASE_URL}${path}`

const API = {
  health: buildApiUrl('/api/health'),
  metrics: buildApiUrl('/api/metrics'),
  events: buildApiUrl('/api/events'),
  alerts: buildApiUrl('/api/alerts'),
  simulate: buildApiUrl('/api/simulate/brute-force'),
  simulateDistributed: buildApiUrl('/api/simulate/distributed-brute-force'),
  rules: buildApiUrl('/api/rules'),
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options)

  if (!response.ok) {
    throw new Error(`Request failed with HTTP ${response.status}`)
  }

  return response.json()
}

function getErrorMessage(error) {
  return error instanceof Error ? error.message : String(error)
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

function StatusBadge({ status }) {
  const labels = {
    checking: 'Checking API...',
    online: 'API Connected',
    offline: 'API Offline',
  }

  return (
    <span className={`status-badge ${status}`} role="status" aria-live="polite">
      <span className="status-dot" />
      {labels[status]}
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
  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(true)
  const [simulating, setSimulating] = useState(false)
  const [simulationToken, setSimulationToken] = useState('')
  const [error, setError] = useState('')
  const [eventsError, setEventsError] = useState('')
  const [alertsError, setAlertsError] = useState('')
  const [rulesError, setRulesError] = useState('')
  const [simulationMessage, setSimulationMessage] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)
  const [shouldLoadChart, setShouldLoadChart] = useState(false)
  const chartContainerRef = useRef(null)

  const loadDashboardData = useCallback(async () => {
    const [metricsResult, eventsResult, alertsResult, rulesResult] =
      await Promise.allSettled([
        fetchJson(API.metrics),
        fetchJson(API.events),
        fetchJson(API.alerts),
        fetchJson(API.rules),
      ])

    const failures = []
    if (metricsResult.status === 'fulfilled') {
      const value = metricsResult.value
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        setMetrics(value)
      } else {
        setMetrics(null)
        failures.push('Metrics response was not a JSON object.')
      }
    } else {
      failures.push(getErrorMessage(metricsResult.reason))
    }
    if (eventsResult.status === 'fulfilled') {
      if (Array.isArray(eventsResult.value)) {
        setEvents(eventsResult.value)
        setEventsError('')
      } else {
        setEvents([])
        setEventsError('Events response was not a JSON array.')
        failures.push('Events response was not a JSON array.')
      }
    } else {
      setEventsError(getErrorMessage(eventsResult.reason))
      failures.push(getErrorMessage(eventsResult.reason))
    }
    if (alertsResult.status === 'fulfilled') {
      if (Array.isArray(alertsResult.value)) {
        setAlerts(alertsResult.value)
        setAlertsError('')
      } else {
        setAlerts([])
        setAlertsError('Alerts response was not a JSON array.')
        failures.push('Alerts response was not a JSON array.')
      }
    } else {
      setAlertsError(getErrorMessage(alertsResult.reason))
      failures.push(getErrorMessage(alertsResult.reason))
    }
    if (rulesResult.status === 'fulfilled') {
      if (Array.isArray(rulesResult.value)) {
        setRules(rulesResult.value)
        setRulesError('')
      } else {
        setRules([])
        setRulesError('Detection rules response was not a JSON array.')
        failures.push('Detection rules response was not a JSON array.')
      }
    } else {
      setRules([])
      setRulesError(`Detection rules unavailable: ${getErrorMessage(rulesResult.reason)}`)
      failures.push(getErrorMessage(rulesResult.reason))
    }

    setError(failures.join(' '))
    setLastUpdated(new Date())
  }, [])

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      await fetchJson(API.health)
      setHealth('online')
      await loadDashboardData()
    } catch (err) {
      setHealth('offline')
      const message = getErrorMessage(err)
      setEventsError(message)
      setAlertsError(message)
      setRulesError(message)
      setError(`Backend health check failed: ${message}`)
    } finally {
      setLoading(false)
    }
  }, [loadDashboardData])

  const refreshDashboard = useCallback(() => {
    setError('')
    void loadDashboard()
  }, [loadDashboard])

  useEffect(() => {
    void Promise.resolve().then(loadDashboard)
  }, [loadDashboard])

  useEffect(() => {
    const chartContainer = chartContainerRef.current
    if (!chartContainer) return

    if (typeof IntersectionObserver === 'undefined') {
      void Promise.resolve().then(() => setShouldLoadChart(true))
      return
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setShouldLoadChart(true)
        observer.disconnect()
      }
    }, { rootMargin: '200px' })

    observer.observe(chartContainer)
    return () => observer.disconnect()
  }, [])

  async function runSimulation(endpoint = API.simulate, attackType = 'brute-force') {
    if (simulating || !simulationToken.trim()) return

    setSimulating(true)
    setError('')
    setSimulationMessage(`Running ${attackType} simulation...`)

    try {
      const results = await fetchJson(endpoint, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${simulationToken}`,
        },
      })

      if (!Array.isArray(results)) {
        throw new Error('Simulation response was not a JSON array.')
      }

      const detectionResults = results
      setAlerts(detectionResults)
      setAlertsError('')

      await loadDashboardData()

      if (detectionResults.length > 0) {
        const count = detectionResults.length

        setSimulationMessage(
          `${attackType} simulation complete: ${count} detection alert${count === 1 ? '' : 's'} generated.`,
        )
      } else {
        setSimulationMessage(
          'Simulation completed. No detection alerts were generated.',
        )
      }
    } catch (err) {
      setSimulationMessage('')
      setError(getErrorMessage(err))
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

        <nav className="sidebar-nav" aria-label="Dashboard sections">
          <a
            className="nav-link active"
            href="#overview"
            aria-current="page"
            aria-label="Overview"
            title="Overview"
          >
            <LayoutDashboard size={18} />
            <span>Overview</span>
          </a>

          <a
            className="nav-link"
            href="#events"
            aria-label="Event Telemetry"
            title="Event Telemetry"
          >
            <Activity size={18} />
            <span>Event Telemetry</span>
          </a>

          <a
            className="nav-link"
            href="#detections"
            aria-label="Detections"
            title="Detections"
          >
            <ShieldAlert size={18} />
            <span>Detections</span>
          </a>

          <a
            className="nav-link"
            href="#mitre"
            aria-label="MITRE ATT&CK"
            title="MITRE ATT&CK"
          >
            <Crosshair size={18} />
            <span>MITRE ATT&CK</span>
          </a>
        </nav>

        <div className="sidebar-bottom">
          <div className="environment-card">
            <div className="environment-heading">
              <span className={`environment-dot ${health}`} />
              BACKEND STATUS
            </div>

            <div className="environment-row">
              <span>Health check</span>
              <span>{health === 'online' ? 'CONNECTED' : health === 'offline' ? 'OFFLINE' : 'CHECKING'}</span>
            </div>

            <div className="environment-row">
              <span>Data mode</span>
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
            <StatusBadge status={health} />

            <button
              className="icon-button"
              onClick={refreshDashboard}
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

          <div className="simulation-controls">
            <label className="simulation-token-field" htmlFor="simulation-token">
              <span>Simulation access token</span>
              <input
                id="simulation-token"
                type="password"
                autoComplete="off"
                aria-describedby="simulation-token-help"
                value={simulationToken}
                onChange={(event) => setSimulationToken(event.target.value)}
              />
            </label>
            <p className="simulation-token-hint" id="simulation-token-help">
              Required for simulations. Kept in memory only; never saved by this page.
            </p>
            <div className="simulation-actions">
              <button
                className="primary-button"
                onClick={() => runSimulation()}
                disabled={simulating || health !== 'online' || !simulationToken.trim()}
              >
                {simulating ? (
                  <LoaderCircle size={17} className="spin" />
                ) : (
                  <Play size={17} fill="currentColor" />
                )}
                {simulating ? 'Running Simulation...' : 'Run Attack Simulation'}
              </button>
              <button
                className="secondary-button"
                onClick={() => runSimulation(API.simulateDistributed, 'distributed brute-force')}
                disabled={simulating || health !== 'online' || !simulationToken.trim()}
              >
                <Users size={16} />
                Distributed Attack
              </button>
            </div>
          </div>
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
            className={`alert-banner ${simulationMessage.includes('simulation complete')
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
            value={metrics?.totalFailedLogins ?? failedLogins}
            description="Failed authentication events"
            icon={Fingerprint}
            accent="orange"
          />
          <MetricCard
            title="Successful Logins"
            value={metrics?.totalSuccessfulLogins}
            description="Successful authentication events"
            icon={ShieldCheck}
            accent="blue"
          />
          <MetricCard
            title="Brute-Force Alerts"
            value={metrics?.bruteForceAlerts}
            description="Single-source detections"
            icon={ShieldAlert}
            accent="red"
          />
          <MetricCard
            title="Distributed Alerts"
            value={metrics?.distributedAttackAlerts}
            description="Multi-source account detections"
            icon={Users}
            accent="orange"
          />
          <MetricCard
            title="Mean Time To Detect"
            value={metrics ? `${metrics.meanTimeToDetectMs} ms` : '—'}
            description="Average attack-start to alert time"
            icon={Clock3}
            accent="purple"
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
                <p>Login events grouped by timestamp hour across retained API history</p>
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
                Login events
              </div>
            </div>

            <div className="chart-container" ref={chartContainerRef}>
              {loading ? (
                <div className="chart-empty-state" role="status">Loading event activity...</div>
              ) : events.length === 0 ? (
                <div className="chart-empty-state">
                  {eventsError || 'No event activity available.'}
                </div>
              ) : shouldLoadChart ? (
                <Suspense fallback={<div className="chart-empty-state" role="status">Loading chart...</div>}>
                  <ActivityChart events={events} />
                </Suspense>
              ) : (
                <div className="chart-empty-state" role="status">Preparing event chart...</div>
              )}
            </div>

            <div className="chart-note">
              <span>
                Counts use valid event timestamps from the retained API history;
                timestamps are grouped by hour of day.
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
                    key={alert.id ?? `${alert.ruleName}-${alert.sourceIp}-${index}`}
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
                        <code>{alert.sourceIps?.length > 1
                          ? alert.sourceIps.join(', ')
                          : alert.sourceIp}</code>
                      </div>

                      {alert.username && (
                        <div className="detail-row">
                          <span>Target account</span>
                          <code>{alert.username}</code>
                        </div>
                      )}

                      <div className="detail-row">
                        <span>Failed attempts</span>
                        <strong>{alert.failedAttempts}</strong>
                      </div>

                      <div className="detail-row">
                        <span>Rule ID</span>
                        <code>{alert.ruleId}</code>
                      </div>

                      <div className="detail-row">
                        <span>MITRE technique</span>
                        <code>{alert.mitreTechnique}</code>
                      </div>
                    </div>

                    {alert.message && <p className="detection-message">{alert.message}</p>}

                    <div className="detection-footer">
                      <CheckCircle2 size={14} />
                      Detected {formatTime(alert.detectedAt)}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state">
                {loading ? (
                  <>
                    <LoaderCircle size={25} className="spin" />
                    <p>Loading alerts...</p>
                  </>
                ) : alertsError ? (
                  <>
                    <XCircle size={25} />
                    <h4>Alerts unavailable</h4>
                    <p>{alertsError}</p>
                  </>
                ) : (
                  <>
                    <ShieldCheck size={32} />
                    <h4>No alerts returned</h4>
                    <p>Run the attack simulation to test the detection engine.</p>
                  </>
                )}
              </div>
            )}

            <button
              className="secondary-button full-width"
              onClick={() => runSimulation()}
              disabled={simulating || health !== 'online' || !simulationToken.trim()}
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
                      <th scope="col">EVENT</th>
                      <th scope="col">SOURCE IP</th>
                      <th scope="col">TARGET ACCOUNT</th>
                      <th scope="col">RESULT</th>
                      <th scope="col">TIMESTAMP</th>
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
                            <code>{event.username ?? 'Unknown'}</code>
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
                {loading ? (
                  <>
                    <LoaderCircle size={22} className="spin" />
                    <p>Loading events...</p>
                  </>
                ) : eventsError ? (
                  <>
                    <XCircle size={22} />
                    <p>Events unavailable: {eventsError}</p>
                  </>
                ) : (
                  <>
                    <Database size={25} />
                    <p>No login events available.</p>
                  </>
                )}
              </div>
            )}

            <div className="table-footer">
              <span>
                <span className="live-dot" />
                Data retrieved from PurpleLoop API
              </span>

              <button onClick={refreshDashboard} disabled={loading}>
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

            {rules.length > 0 ? (
              <div className="rule-coverage-list">
                {rules.map((rule) => (
                  <article className="rule-coverage-item" key={rule.ruleId}>
                    <div className="rule-coverage-heading">
                      <code>{rule.mitreTechnique}</code>
                      <span className={`rule-status ${rule.enabled ? 'enabled' : 'disabled'}`}>
                        {rule.detectionStatus}
                      </span>
                    </div>
                    <strong>{rule.ruleName}</strong>
                    <p>{rule.description}</p>
                    <div className="rule-coverage-footer">
                      <span>{rule.ruleId}</span>
                      <SeverityBadge severity={rule.severity} />
                    </div>
                  </article>
                ))}
                <div className="mitre-footer">
                  <Crosshair size={15} />
                  Implemented rule mappings only; this is not full ATT&amp;CK coverage.
                </div>
              </div>
            ) : (
              <div className="empty-state compact">
                <Crosshair size={25} />
                <p>
                  {rulesError ||
                    (loading ? 'Loading detection rules...' : 'No detection rules returned.')}
                </p>
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
                <span>Stateful windowed rules</span>
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
                <span>Coverage and MTTD metrics</span>
              </div>
            </div>
          </div>
        </section>

        <footer className="app-footer">
          <div>
            <span className={`footer-status-dot ${health}`} />
            PURPLELOOP <span className="footer-divider">/</span> BACKEND
            {health === 'online' ? ' CONNECTED' : health === 'offline' ? ' OFFLINE' : ' CHECKING'}
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