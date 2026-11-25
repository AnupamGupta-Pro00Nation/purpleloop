// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import App from './App.jsx'

vi.mock('./ActivityChart.jsx', () => ({
  default: ({ events }) => <div data-testid="activity-chart">{events.length} chart events</div>,
}))

const initialData = {
  metrics: {
    totalEvents: 1,
    totalAlerts: 1,
    uniqueSourceIps: 1,
    totalFailedLogins: 1,
    totalSuccessfulLogins: 0,
    bruteForceAlerts: 1,
    distributedAttackAlerts: 0,
    meanTimeToDetectMs: 1200,
  },
  events: [{
    id: 'event-1',
    ip: '192.168.1.10',
    success: false,
    username: 'admin@example.test',
    timestamp: '2026-01-01T12:00:00',
  }],
  alerts: [{
    id: 'alert-1',
    ruleId: 'PL-BF-001',
    ruleName: 'Repeated Failed Logins',
    sourceIp: '192.168.1.10',
    sourceIps: ['192.168.1.10'],
    username: 'admin@example.test',
    failedAttempts: 3,
    severity: 'HIGH',
    mitreTechnique: 'T1110',
    detectedAt: '2026-01-01T12:00:02',
    message: 'Detected repeated failed logins.',
  }],
  rules: [{
    ruleId: 'PL-BF-001',
    ruleName: 'Repeated Failed Logins',
    description: 'Repeated failed login attempts from one source.',
    severity: 'HIGH',
    mitreTechnique: 'T1110',
    enabled: true,
    detectionStatus: 'ENABLED',
  }],
}

const SIMULATION_TOKEN = 'test-simulation-access-token-123'

function ok(body) {
  return { ok: true, status: 200, json: async () => body }
}

function installApi({ data = initialData, onSimulation, healthRequest } = {}) {
  const fetchMock = vi.fn(async (url, options = {}) => {
    const path = new URL(url, 'http://localhost').pathname

    if (path === '/api/health') {
      return healthRequest ?? ok({ project: 'PurpleLoop', status: 'UP' })
    }
    if (options.method === 'POST') {
      return onSimulation ? onSimulation(path) : ok([])
    }
    if (path === '/api/metrics') return ok(data.metrics)
    if (path === '/api/events') return ok(data.events)
    if (path === '/api/alerts') return ok(data.alerts)
    if (path === '/api/rules') return ok(data.rules)

    throw new Error(`Unexpected API request: ${path}`)
  })

  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function expectMetric(title, expectedValue) {
  const card = screen.getByText(title).closest('.metric-card')
  expect(card?.querySelector('.metric-value')?.textContent).toBe(String(expectedValue))
}

function enterSimulationToken() {
  fireEvent.change(screen.getByLabelText('Simulation access token'), {
    target: { value: SIMULATION_TOKEN },
  })
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})

describe('PurpleLoop dashboard', () => {
  it('shows loading state before the health request completes', async () => {
    let resolveHealth
    const healthRequest = new Promise((resolve) => {
      resolveHealth = resolve
    })
    installApi({ healthRequest })

    render(<App />)

    expect(screen.getByText('Checking API...')).toBeTruthy()

    await act(async () => {
      resolveHealth(ok({ project: 'PurpleLoop', status: 'UP' }))
    })
    await screen.findAllByText('192.168.1.10')
    expect(screen.getByText('API Connected')).toBeTruthy()
  })

  it('renders metrics, event telemetry, alerts, and rules from API responses', async () => {
    installApi()
    render(<App />)

    await screen.findByText('API Connected')

    expectMetric('Total Events', 1)
    expectMetric('Detection Alerts', 1)
    expect(screen.getAllByText('192.168.1.10').length).toBeGreaterThan(0)
    expect(screen.getAllByText('admin@example.test').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Repeated Failed Logins')).toHaveLength(2)
    expect(screen.getByText('Detected repeated failed logins.')).toBeTruthy()
    expect(screen.getAllByText('PL-BF-001')).toHaveLength(2)
  })

  it('shows explicit empty states for empty API datasets', async () => {
    installApi({
      data: {
        metrics: {
          totalEvents: 0,
          totalAlerts: 0,
          uniqueSourceIps: 0,
          totalFailedLogins: 0,
          totalSuccessfulLogins: 0,
          bruteForceAlerts: 0,
          distributedAttackAlerts: 0,
          meanTimeToDetectMs: 0,
        },
        events: [],
        alerts: [],
        rules: [],
      },
    })
    render(<App />)

    expect(await screen.findByText('No alerts returned')).toBeTruthy()
    expect(screen.getByText('No login events available.')).toBeTruthy()
    expect(screen.getByText('No detection rules returned.')).toBeTruthy()
    expect(screen.getByText('No event activity available.')).toBeTruthy()
  })

  it('shows backend errors and disables simulation when health check fails', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: false,
      status: 503,
      json: async () => ({}),
    })))
    render(<App />)

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByText('Backend health check failed: Request failed with HTTP 503')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Run Attack Simulation' }).disabled).toBe(true)
    expect(screen.getByRole('button', { name: 'Distributed Attack' }).disabled).toBe(true)
  })

  it('requires an operator-entered token before enabling simulations', async () => {
    installApi()
    render(<App />)
    await screen.findByText('API Connected')

    const simulationButton = screen.getByRole('button', { name: 'Run Attack Simulation' })
    expect(simulationButton.disabled).toBe(true)

    enterSimulationToken()

    expect(simulationButton.disabled).toBe(false)
  })

  it('does not simulate on mount or dashboard refresh', async () => {
    const fetchMock = installApi()
    const view = render(<App />)
    await screen.findByText('API Connected')
    enterSimulationToken()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Refresh dashboard' }).disabled).toBe(false)
    })
    fireEvent.click(screen.getByRole('button', { name: 'Refresh dashboard' }))
    await waitFor(() => {
      expect(fetchMock.mock.calls.filter(([, options]) => options?.method === 'POST')).toHaveLength(0)
      expect(fetchMock.mock.calls.filter(([url]) => url === '/api/health')).toHaveLength(2)
      expect(screen.getByRole('button', { name: 'Refresh dashboard' }).disabled).toBe(false)
    })

    view.unmount()
    render(<App />)
    await waitFor(() => {
      expect(fetchMock.mock.calls.filter(([url]) => url === '/api/health')).toHaveLength(3)
      expect(fetchMock.mock.calls.filter(([, options]) => options?.method === 'POST')).toHaveLength(0)
      expect(screen.getByRole('button', { name: 'Refresh dashboard' }).disabled).toBe(false)
    })
    expect(screen.getByLabelText('Simulation access token').value).toBe('')
    expect(screen.getByRole('button', { name: 'Run Attack Simulation' }).disabled).toBe(true)
  })

  it('runs a distributed simulation on demand and reflects the refreshed API data', async () => {
    const updatedAlert = {
      ...initialData.alerts[0],
      id: 'alert-2',
      ruleId: 'PL-DBF-001',
      ruleName: 'Distributed Brute Force',
    }
    const updatedData = {
      ...initialData,
      metrics: { ...initialData.metrics, totalEvents: 7, totalAlerts: 2, distributedAttackAlerts: 1 },
      events: [...initialData.events, {
        id: 'event-2',
        ip: '192.168.1.20',
        success: false,
        username: 'admin@example.test',
        timestamp: '2026-01-01T12:01:00',
      }],
      alerts: [...initialData.alerts, updatedAlert],
    }
    let simulationCompleted = false
    const fetchMock = installApi({
      data: initialData,
      onSimulation: (path) => {
        expect(path).toBe('/api/simulate/distributed-brute-force')
        simulationCompleted = true
        return ok([updatedAlert])
      },
    })
    const originalFetch = fetchMock.getMockImplementation()
    fetchMock.mockImplementation(async (url, options = {}) => {
      if (simulationCompleted && !options.method) {
        const path = new URL(url, 'http://localhost').pathname
        if (path === '/api/metrics') return ok(updatedData.metrics)
        if (path === '/api/events') return ok(updatedData.events)
        if (path === '/api/alerts') return ok(updatedData.alerts)
        if (path === '/api/rules') return ok(updatedData.rules)
      }
      return originalFetch(url, options)
    })
    render(<App />)
    await screen.findByText('API Connected')
    enterSimulationToken()

    fireEvent.click(screen.getByRole('button', { name: 'Distributed Attack' }))

    expect(await screen.findByText('distributed brute-force simulation complete: 1 detection alert generated.')).toBeTruthy()
    await waitFor(() => {
      expectMetric('Total Events', 7)
      expect(screen.getByText('Distributed Brute Force')).toBeTruthy()
    })
    expect(fetchMock.mock.calls.some(([url, options]) =>
      url === '/api/simulate/distributed-brute-force'
        && options?.method === 'POST'
        && options?.headers?.Authorization === `Bearer ${SIMULATION_TOKEN}`)).toBe(true)
  })

  it('surfaces simulation failures instead of reporting success', async () => {
    const fetchMock = installApi({
      onSimulation: () => Promise.resolve({
        ok: false,
        status: 429,
        json: async () => ({ status: 429 }),
      }),
    })
    render(<App />)
    await screen.findByText('API Connected')
    enterSimulationToken()

    fireEvent.click(screen.getByRole('button', { name: 'Run Attack Simulation' }))

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByText('Request failed with HTTP 429')).toBeTruthy()
    expect(screen.queryByText(/simulation complete/)).toBeNull()
    expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'POST')).toBe(true)
  })

  it('rejects malformed simulation responses instead of showing false success', async () => {
    installApi({ onSimulation: () => ok({ alert: 'unexpected shape' }) })
    render(<App />)
    await screen.findByText('API Connected')
    enterSimulationToken()

    fireEvent.click(screen.getByRole('button', { name: 'Run Attack Simulation' }))

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByText('Simulation response was not a JSON array.')).toBeTruthy()
    expect(screen.queryByText(/simulation complete/)).toBeNull()
  })
})
