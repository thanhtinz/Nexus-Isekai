// api/client.ts — Axios-based API client
import axios from 'axios'
import type {
  LoginResponse, Session,
  TopupPackage, TopupOrder, TopupOrderStatus, TopupHistoryItem,
  WebshopItem, BuyResult, RedeemResult,
} from '@/types/api'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Attach auth token to every request
http.interceptors.request.use(config => {
  const session = useAuthStore.getState().session
  if (session?.token) {
    config.params = { ...config.params, token: session.token, account_id: session.accountId }
  }
  return config
})

// Lazy import to avoid circular dep
let useAuthStore: any
export function setAuthStore(store: any) { useAuthStore = store }

//  Auth 
export const authApi = {
  login: (username: string, password: string) =>
    http.post<LoginResponse>('/weblogin', { username, password }).then(r => r.data),

  logout: () => {
    /* clear session local only */
  },
}

//  Topup 
export const topupApi = {
  packages: () =>
    http.get<{ success: true; packages: TopupPackage[] }>('/packages')
      .then(r => r.data.packages),

  createOrder: (packageId: number, accountId: number, token: string) =>
    http.post<TopupOrder>('/order', { package_id: packageId, account_id: accountId, token })
      .then(r => r.data),

  orderStatus: (orderId: string) =>
    http.get<TopupOrderStatus>('/orderstatus', { params: { order_id: orderId } })
      .then(r => r.data),

  history: () =>
    http.get<{ success: true; orders: TopupHistoryItem[] }>('/history')
      .then(r => r.data.orders),

  balance: () =>
    http.get<{ success: true; diamond: number }>('/balance')
      .then(r => r.data.diamond),
}

//  Webshop 
export const shopApi = {
  items: (accountId?: number) =>
    http.get<{ success: true; items: WebshopItem[] }>('/shopitems', {
      params: accountId ? { account_id: accountId } : {}
    }).then(r => r.data.items),

  buy: (itemId: number, accountId: number, charId: number, token: string) =>
    http.post<BuyResult>('/buy', { item_id: itemId, account_id: accountId, char_id: charId, token })
      .then(r => r.data),

  redeem: (code: string, accountId: number, charId: number, level: number, token: string) =>
    http.post<RedeemResult>('/redeem', { code, account_id: accountId, char_id: charId, level, token })
      .then(r => r.data),
}


// Alias for context/pages that import { api }
export { http as api }
