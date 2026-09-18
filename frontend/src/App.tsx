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
    message: 'API backend disponível.',
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
      return 'Nenhuma liquidação criada nesta sessão.'
    }

    return `${lastSettlement.paymentCurrency} ${formatMoney(lastSettlement.paymentAmount)} para o recebível ${shortId(lastSettlement.receivableId)}`
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
        message: `Recebível ${shortId(receivable.id)} criado com sucesso.`,
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
        message: `Taxa de câmbio ${exchangeRate.sourceCurrency}/${exchangeRate.targetCurrency} salva com sucesso.`,
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
        message: `Liquidação ${shortId(settlement.id)} retornada pela API.`,
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
          <h1>Painel de Operações</h1>
        </div>
        <div className={`notice ${notice.kind}`} role="status">
          {notice.message}
        </div>
      </header>

      <section className="workflow-grid" aria-label="Fluxo da API">
        <form className="panel" onSubmit={handleCreateReceivable}>
          <div className="panel-heading">
            <h2>Recebível</h2>
            <span>POST /api/v1/receivables</span>
          </div>

          <label>
            Cedente
            <input
              value={assignor}
              onChange={(event) => setAssignor(event.target.value)}
              required
            />
          </label>

          <label>
            Tipo
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
              Valor de Face
              <input
                inputMode="decimal"
                value={faceValue}
                onChange={(event) => setFaceValue(event.target.value)}
                required
              />
            </label>
            <label>
              Data de Vencimento
              <input
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
                required
              />
            </label>
          </div>

          <button type="submit" disabled={loading}>
            Criar Recebível
          </button>

          <ResultLine
            label="Último Recebível"
            value={
              lastReceivable
                ? `${shortId(lastReceivable.id)} - ${lastReceivable.status}`
                : 'Nenhum'
            }
          />
        </form>

        <form className="panel" onSubmit={handleCreateExchangeRate}>
          <div className="panel-heading">
            <h2>Taxa de Câmbio</h2>
            <span>POST /api/v1/exchange-rates</span>
          </div>

          <div className="field-row">
            <label>
              Moeda de Origem
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
              Moeda de Destino
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
            Taxa
            <input
              inputMode="decimal"
              value={rate}
              onChange={(event) => setRate(event.target.value)}
              required
            />
          </label>

          <label>
            Vigente a partir de
            <input
              value={effectiveAt}
              onChange={(event) => setEffectiveAt(event.target.value)}
              required
            />
          </label>

          <button type="submit" disabled={loading}>
            Salvar Taxa
          </button>

          <ResultLine
            label="Última Taxa"
            value={
              lastExchangeRate
                ? `${lastExchangeRate.sourceCurrency}/${lastExchangeRate.targetCurrency} ${lastExchangeRate.rate}`
                : 'Nenhuma'
            }
          />
        </form>

        <form className="panel" onSubmit={handleCreateSettlement}>
          <div className="panel-heading">
            <h2>Liquidação</h2>
            <span>POST /api/v1/settlements</span>
          </div>

          <label>
            ID do Recebível
            <input
              value={settlementReceivableId}
              onChange={(event) => setSettlementReceivableId(event.target.value)}
              required
            />
          </label>

          <div className="field-row">
            <label>
              Moeda de Pagamento
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
              Chave de Idempotência
              <input
                value={idempotencyKey}
                onChange={(event) => setIdempotencyKey(event.target.value)}
                required
              />
            </label>
          </div>

          <div className="actions">
            <button type="submit" disabled={loading}>
              Liquidar
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => setIdempotencyKey(crypto.randomUUID())}
              disabled={loading}
            >
              Nova Chave
            </button>
          </div>

          <ResultLine label="Última Liquidação" value={settlementSummary} />
        </form>
      </section>

      <section className="simulation-section" aria-label="Simulação de Precificação">
        <div className="section-bar">
          <div>
            <h2>Simulação de Precificação</h2>
            <span>POST /api/v1/pricing/simulations</span>
          </div>
          <strong className={`simulation-status ${simulationStatus}`}>
            {simulationStatus === 'loading' && 'Calculando...'}
            {simulationStatus === 'success' && 'Pronto'}
            {simulationStatus === 'error' && 'Requer atenção'}
            {simulationStatus === 'idle' && 'Aguardando dados válidos'}
          </strong>
        </div>

        {simulationStatus === 'error' && (
          <div className="simulation-error">{simulationError}</div>
        )}

        <div className="simulation-grid">
          <Metric label="Valor de Face" value={formatMoney(simulation?.faceValue)} />
          <Metric
            label="Prazo"
            value={
              simulation ? `${simulation.termInMonths.toString()} meses` : '-'
            }
          />
          <Metric label="Taxa Base" value={formatRate(simulation?.baseRate)} />
          <Metric label="Spread" value={formatRate(simulation?.spread)} />
          <Metric
            label="Valor Presente em BRL"
            value={formatMoney(simulation?.presentValueBrl)}
          />
          <Metric
            label="Deságio"
            value={formatMoney(simulation?.discountAmount)}
          />
          <Metric label="Moeda de Pagamento" value={simulation?.paymentCurrency ?? '-'} />
          <Metric
            label="Valor Líquido"
            value={formatMoney(simulation?.paymentAmount)}
          />
          <Metric
            label="Taxa de Câmbio"
            value={
              simulation?.exchangeRate
                ? `${simulation.exchangeRate.toString()} vigente em ${formatDateTime(simulation.exchangeRateEffectiveAt)}`
                : '-'
            }
          />
        </div>
      </section>

      <section className="table-section" aria-label="Liquidações">
        <div className="section-bar">
          <div>
            <h2>Liquidações</h2>
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
              Atualizar
            </button>
            <button
              type="button"
              className="secondary"
              disabled={!canGoBack || loading}
              onClick={() => void refreshSettlements(page - 1)}
            >
              Anterior
            </button>
            <span>
              Página {settlements.totalPages === 0 ? 0 : page + 1} de{' '}
              {settlements.totalPages}
            </span>
            <button
              type="button"
              className="secondary"
              disabled={!canGoNext || loading}
              onClick={() => void refreshSettlements(page + 1)}
            >
              Próxima
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>LIQUIDAÇÃO</th>
                <th>RECEBÍVEL</th>
                <th>MOEDA</th>
                <th>PAGAMENTO</th>
                <th>VALOR PRESENTE BRL</th>
                <th>TAXA DE CÂMBIO</th>
                <th>LIQUIDADO EM</th>
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
                    Nenhuma liquidação retornada pela API.
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

  return 'Erro inesperado'
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
