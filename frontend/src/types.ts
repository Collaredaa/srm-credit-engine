export type Currency = 'BRL' | 'USD'

export type ReceivableType = 'DUPLICATA_MERCANTIL' | 'CHEQUE_PRE_DATADO'

export type ReceivableStatus = 'OPEN' | 'SETTLED'

export interface CreateReceivableRequest {
  assignor: string
  type: ReceivableType
  faceValue: number
  dueDate: string
}

export interface ReceivableResponse {
  id: string
  assignor: string
  type: ReceivableType
  faceValue: number
  dueDate: string
  status: ReceivableStatus
  createdAt: string
}

export interface CreateExchangeRateRequest {
  sourceCurrency: Currency
  targetCurrency: Currency
  rate: number
  effectiveAt: string
}

export interface ExchangeRateResponse {
  id: string
  sourceCurrency: Currency
  targetCurrency: Currency
  rate: number
  effectiveAt: string
  createdAt: string
}

export interface PricingSimulationRequest {
  type: ReceivableType
  faceValue: number
  dueDate: string
  paymentCurrency: Currency
}

export interface PricingSimulationResponse {
  faceValue: number
  presentValueBrl: number
  discountAmount: number
  paymentCurrency: Currency
  paymentAmount: number
  baseRate: number
  spread: number
  termInMonths: number
  exchangeRate: number | null
  exchangeRateEffectiveAt: string | null
}

export interface CreateSettlementRequest {
  receivableId: string
  paymentCurrency: Currency
}

export interface SettlementResult {
  id: string
  receivableId: string
  idempotencyKey: string
  faceValue: number
  presentValueBrl: number
  paymentAmount: number
  paymentCurrency: Currency
  baseRate: number
  spread: number
  exchangeRate: number | null
  exchangeRateEffectiveAt: string | null
  settledAt: string
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
