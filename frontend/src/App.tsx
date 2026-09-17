import { type FormEvent, useEffect, useMemo, useState } from 'react'
import {
  createExchangeRate,
  createReceivable,
  createSettlement,
  listSettlements,
  simulatePricing,
} from './api'
import './App.css'
import type {
  Currency,
  ExchangeRateResponse,
  PricingSimulationResponse,
  ReceivableResponse,
  ReceivableType,
  SettlementResult,
  SpringPage,
} from './types'

type NoticeKind = 'success' | 'error' | 'info'
type SimulationStatus = 'idle' | 'loading' | 'success' | 'error'

interface Notice {
  kind: NoticeKind
  message: string
}

const RECEIVABLE_TYPES: ReceivableType[] = [
  'DUPLICATA_MERCANTIL',
  'CHEQUE_PRE_DATADO',
]

const CURRENCIES: Currency[] = ['BRL', 'USD']

const initialSettlementPage: SpringPage<SettlementResult> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
  first: true,
  last: true,
  numberOfElements: 0,
  empty: true,
}

function App() {
  const [notice, setNotice] = useState<Notice>({
    kind: 'info',
    message: 'Ready to connect with the backend API.',
  })
  const [loading, setLoading] = useState(false)

  const [assignor, setAssignor] = useState('Fornecedor XPTO')
  const [receivableType, setReceivableType] =
    useState<ReceivableType>('DUPLICATA_MERCANTIL')
  const [faceValue, setFaceValue] = useState('100000.00')
  const [dueDate, setDueDate] = useState('2026-12-15')
  const [lastReceivable, setLastReceivable] =
    useState<ReceivableResponse | null>(null)

  const [sourceCurrency, setSourceCurrency] = useState<Currency>('USD')
  const [targetCurrency, setTargetCurrency] = useState<Currency>('BRL')
  const [rate, setRate] = useState('5.4321')
  const [effectiveAt, setEffectiveAt] = useState('2026-09-17T10:00:00-03:00')
  const [lastExchangeRate, setLastExchangeRate] =
    useState<ExchangeRateResponse | null>(null)

  const [settlementReceivableId, setSettlementReceivableId] = useState('')
  const [paymentCurrency, setPaymentCurrency] = useState<Currency>('BRL')
  const [idempotencyKey, setIdempotencyKey] = useState<string>(() =>
    crypto.randomUUID(),
  )
  const [lastSettlement, setLastSettlement] =
    useState<SettlementResult | null>(null)
  const [simulation, setSimulation] =
    useState<PricingSimulationResponse | null>(null)
  const [simulationStatus, setSimulationStatus] =
    useState<SimulationStatus>('idle')
  const [simulationError, setSimulationError] = useState('')

  const [settlements, setSettlements] =
    useState<SpringPage<SettlementResult>>(initialSettlementPage)
  const [page, setPage] = useState(0)

  const canGoBack = page > 0
  const canGoNext = !settlements.last && settlements.totalPages > 0

  const settlementSummary = useMemo(() => {
    if (!lastSettlement) {
      return 'No settlement created in this session yet.'
    }

    return `${lastSettlement.paymentCurrency} ${formatMoney(lastSettlement.paymentAmount)} for receivable ${shortId(lastSettlement.receivableId)}`
  }, [lastSettlement])

  useEffect(() => {
    const parsedFaceValue = Number(faceValue)

    if (
      !Number.isFinite(parsedFaceValue) ||
      parsedFaceValue <= 0 ||
      !dueDate ||
      !receivableType ||
      !paymentCurrency
    ) {
      return undefined
    }

    const timeoutId = window.setTimeout(() => {
      setSimulationStatus('loading')
      setSimulationError('')

      void simulatePricing({
        type: receivableType,
        faceValue: parsedFaceValue,
        dueDate,
        paymentCurrency,
      })
        .then((response) => {
          setSimulation(response)
          setSimulationStatus('success')
        })
        .catch((error: unknown) => {
          setSimulationStatus('error')
          setSimulationError(messageFromError(error))
        })
    }, 400)

    return () => window.clearTimeout(timeoutId)
  }, [dueDate, faceValue, paymentCurrency, receivableType])

  async function refreshSettlements(nextPage: number) {
    try {
      const response = await listSettlements(nextPage, 20)
      setSettlements(response)
      setPage(response.number)
    } catch (error) {
      setNotice({ kind: 'error', message: messageFromError(error) })
    }
  }

  async function handleCreateReceivable(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)

    try {
      const receivable = await createReceivable({
        assignor,
        type: receivableType,
        faceValue: Number(faceValue),
        dueDate,
      })

      setLastReceivable(receivable)
      setSettlementReceivableId(receivable.id)
      setNotice({
        kind: 'success',
        message: `Receivable ${shortId(receivable.id)} created.`,
      })
    } catch (error) {
      setNotice({ kind: 'error', message: messageFromError(error) })
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateExchangeRate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)

    try {
      const exchangeRate = await createExchangeRate({
        sourceCurrency,
        targetCurrency,
        rate: Number(rate),
        effectiveAt,
      })

      setLastExchangeRate(exchangeRate)
      setNotice({
        kind: 'success',
        message: `Exchange rate ${exchangeRate.sourceCurrency}/${exchangeRate.targetCurrency} saved.`,
      })
    } catch (error) {
      setNotice({ kind: 'error', message: messageFromError(error) })
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateSettlement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)

    try {
      const settlement = await createSettlement(
        {
          receivableId: settlementReceivableId,
          paymentCurrency,
        },
        idempotencyKey,
      )

      setLastSettlement(settlement)
      setNotice({
        kind: 'success',
        message: `Settlement ${shortId(settlement.id)} returned by API.`,
      })
      await refreshSettlements(0)
    } catch (error) {
      setNotice({ kind: 'error', message: messageFromError(error) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">SRM Credit Engine</p>
          <h1>Operations console</h1>
        </div>
        <div className={`notice ${notice.kind}`} role="status">
          {notice.message}
        </div>
      </header>

      <section className="workflow-grid" aria-label="API workflow">
        <form className="panel" onSubmit={handleCreateReceivable}>
          <div className="panel-heading">
            <h2>Receivable</h2>
            <span>POST /api/v1/receivables</span>
          </div>

          <label>
            Assignor
            <input
              value={assignor}
              onChange={(event) => setAssignor(event.target.value)}
              required
            />
          </label>

          <label>
            Type
            <select
              value={receivableType}
              onChange={(event) =>
                setReceivableType(event.target.value as ReceivableType)
              }
              required
            >
              {RECEIVABLE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>

          <div className="field-row">
            <label>
              Face value
              <input
                inputMode="decimal"
                value={faceValue}
                onChange={(event) => setFaceValue(event.target.value)}
                required
              />
            </label>
            <label>
              Due date
              <input
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
                required
              />
            </label>
          </div>

          <button type="submit" disabled={loading}>
            Create receivable
          </button>

          <ResultLine
            label="Last receivable"
            value={
              lastReceivable
                ? `${shortId(lastReceivable.id)} - ${lastReceivable.status}`
                : 'None'
            }
          />
        </form>

        <form className="panel" onSubmit={handleCreateExchangeRate}>
          <div className="panel-heading">
            <h2>Exchange rate</h2>
            <span>POST /api/v1/exchange-rates</span>
          </div>

          <div className="field-row">
            <label>
              Source
              <select
                value={sourceCurrency}
                onChange={(event) =>
                  setSourceCurrency(event.target.value as Currency)
                }
                required
              >
                {CURRENCIES.map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Target
              <select
                value={targetCurrency}
                onChange={(event) =>
                  setTargetCurrency(event.target.value as Currency)
                }
                required
              >
                {CURRENCIES.map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label>
            Rate
            <input
              inputMode="decimal"
              value={rate}
              onChange={(event) => setRate(event.target.value)}
              required
            />
          </label>

          <label>
            Effective at
            <input
              value={effectiveAt}
              onChange={(event) => setEffectiveAt(event.target.value)}
              required
            />
          </label>

          <button type="submit" disabled={loading}>
            Save rate
          </button>

          <ResultLine
            label="Last rate"
            value={
              lastExchangeRate
                ? `${lastExchangeRate.sourceCurrency}/${lastExchangeRate.targetCurrency} ${lastExchangeRate.rate}`
                : 'None'
            }
          />
        </form>

        <form className="panel" onSubmit={handleCreateSettlement}>
          <div className="panel-heading">
            <h2>Settlement</h2>
            <span>POST /api/v1/settlements</span>
          </div>

          <label>
            Receivable id
            <input
              value={settlementReceivableId}
              onChange={(event) => setSettlementReceivableId(event.target.value)}
              required
            />
          </label>

          <div className="field-row">
            <label>
              Payment currency
              <select
                value={paymentCurrency}
                onChange={(event) =>
                  setPaymentCurrency(event.target.value as Currency)
                }
                required
              >
                {CURRENCIES.map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Idempotency-Key
              <input
                value={idempotencyKey}
                onChange={(event) => setIdempotencyKey(event.target.value)}
                required
              />
            </label>
          </div>

          <div className="actions">
            <button type="submit" disabled={loading}>
              Settle
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => setIdempotencyKey(crypto.randomUUID())}
              disabled={loading}
            >
              New key
            </button>
          </div>

          <ResultLine label="Last settlement" value={settlementSummary} />
        </form>
      </section>

      <section className="simulation-section" aria-label="Pricing simulation">
        <div className="section-bar">
          <div>
            <h2>Pricing simulation</h2>
            <span>POST /api/v1/pricing/simulations</span>
          </div>
          <strong className={`simulation-status ${simulationStatus}`}>
            {simulationStatus === 'loading' && 'Calculating...'}
            {simulationStatus === 'success' && 'Ready'}
            {simulationStatus === 'error' && 'Needs attention'}
            {simulationStatus === 'idle' && 'Waiting for valid input'}
          </strong>
        </div>

        {simulationStatus === 'error' && (
          <div className="simulation-error">{simulationError}</div>
        )}

        <div className="simulation-grid">
          <Metric label="Face value" value={formatMoney(simulation?.faceValue)} />
          <Metric
            label="Term"
            value={
              simulation ? `${simulation.termInMonths.toString()} months` : '-'
            }
          />
          <Metric label="Base rate" value={formatRate(simulation?.baseRate)} />
          <Metric label="Spread" value={formatRate(simulation?.spread)} />
          <Metric
            label="Present value BRL"
            value={formatMoney(simulation?.presentValueBrl)}
          />
          <Metric
            label="Discount"
            value={formatMoney(simulation?.discountAmount)}
          />
          <Metric label="Payment currency" value={simulation?.paymentCurrency ?? '-'} />
          <Metric
            label="Net payment"
            value={formatMoney(simulation?.paymentAmount)}
          />
          <Metric
            label="Exchange rate"
            value={
              simulation?.exchangeRate
                ? `${simulation.exchangeRate.toString()} effective ${formatDateTime(simulation.exchangeRateEffectiveAt)}`
                : '-'
            }
          />
        </div>
      </section>

      <section className="table-section" aria-label="Settlements">
        <div className="section-bar">
          <div>
            <h2>Settlements</h2>
            <span>
              GET /api/v1/settlements?page={page}&size={settlements.size}
            </span>
          </div>
          <div className="pager">
            <button
              type="button"
              className="secondary"
              disabled={loading}
              onClick={() => void refreshSettlements(page)}
            >
              Refresh
            </button>
            <button
              type="button"
              className="secondary"
              disabled={!canGoBack || loading}
              onClick={() => void refreshSettlements(page - 1)}
            >
              Previous
            </button>
            <span>
              Page {settlements.totalPages === 0 ? 0 : page + 1} of{' '}
              {settlements.totalPages}
            </span>
            <button
              type="button"
              className="secondary"
              disabled={!canGoNext || loading}
              onClick={() => void refreshSettlements(page + 1)}
            >
              Next
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Settlement</th>
                <th>Receivable</th>
                <th>Currency</th>
                <th>Payment</th>
                <th>Present BRL</th>
                <th>FX rate</th>
                <th>Settled at</th>
              </tr>
            </thead>
            <tbody>
              {settlements.content.map((settlement) => (
                <tr key={settlement.id}>
                  <td>{shortId(settlement.id)}</td>
                  <td>{shortId(settlement.receivableId)}</td>
                  <td>{settlement.paymentCurrency}</td>
                  <td>{formatMoney(settlement.paymentAmount)}</td>
                  <td>{formatMoney(settlement.presentValueBrl)}</td>
                  <td>{settlement.exchangeRate ?? '-'}</td>
                  <td>{formatDateTime(settlement.settledAt)}</td>
                </tr>
              ))}
              {settlements.empty && (
                <tr>
                  <td colSpan={7} className="empty-state">
                    No settlements returned by the API.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}

function ResultLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="result-line">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function messageFromError(error: unknown) {
  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}

function shortId(value: string) {
  return value.length > 8 ? value.slice(0, 8) : value
}

function formatMoney(value?: number) {
  if (value === undefined) {
    return '-'
  }

  return value.toLocaleString('en', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function formatRate(value?: number) {
  if (value === undefined) {
    return '-'
  }

  return value.toString()
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('en', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export default App
