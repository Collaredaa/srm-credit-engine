import type {
  ApiErrorResponse,
  CreateExchangeRateRequest,
  CreateReceivableRequest,
  CreateSettlementRequest,
  ExchangeRateResponse,
  PricingSimulationRequest,
  PricingSimulationResponse,
  ReceivableResponse,
  SettlementResult,
  SpringPage,
} from './types'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const API_PREFIX = `${API_BASE_URL}/api/v1`

export class ApiError extends Error {
  readonly status: number
  readonly details?: ApiErrorResponse

  constructor(message: string, status: number, details?: ApiErrorResponse) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_PREFIX}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    let details: ApiErrorResponse | undefined
    try {
      details = (await response.json()) as ApiErrorResponse
    } catch {
      details = undefined
    }

    throw new ApiError(
      details?.message ?? `Requisição falhou com status ${response.status}`,
      response.status,
      details,
    )
  }

  return (await response.json()) as T
}

export function createReceivable(
  payload: CreateReceivableRequest,
): Promise<ReceivableResponse> {
  return request<ReceivableResponse>('/receivables', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createExchangeRate(
  payload: CreateExchangeRateRequest,
): Promise<ExchangeRateResponse> {
  return request<ExchangeRateResponse>('/exchange-rates', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createSettlement(
  payload: CreateSettlementRequest,
  idempotencyKey: string,
): Promise<SettlementResult> {
  return request<SettlementResult>('/settlements', {
    method: 'POST',
    headers: {
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(payload),
  })
}

export function simulatePricing(
  payload: PricingSimulationRequest,
): Promise<PricingSimulationResponse> {
  return request<PricingSimulationResponse>('/pricing/simulations', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function listSettlements(
  page = 0,
  size = 20,
): Promise<SpringPage<SettlementResult>> {
  return request<SpringPage<SettlementResult>>(
    `/settlements?page=${page}&size=${size}`,
  )
}
