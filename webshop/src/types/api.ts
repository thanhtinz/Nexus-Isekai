// ============================================================
// types/api.ts — Shared types synced với Java server DTOs
// Mọi thay đổi ở đây phải phản ánh tương ứng ở server Java
// ============================================================

//  Auth 
export interface Session {
  accountId: number
  charId: number
  charName: string
  level: number
  diamond: number
  token: string
}

export interface LoginResponse {
  success: boolean
  message?: string
  isFirstTopup?: boolean
  session?: Session
}

//  Topup / Payment 
export interface TopupPackage {
  id: number
  name: string
  diamond: number
  bonus_diamond: number
  price_vnd: number
  is_featured: boolean
  icon_url: string | null
  sort_order: number
}

export interface TopupOrder {
  success: boolean
  message?: string
  order_id: string
  amount: number
  transfer_content: string
  bank_account: string
  bank_name: string
  account_name: string
  qr_url: string
}

export interface TopupOrderStatus {
  success: boolean
  status: 'pending' | 'paid' | 'failed' | 'refunded'
  diamond?: number
}

export interface TopupHistoryItem {
  id: string
  package_name: string
  diamond: number
  bonus_diamond: number
  amount_vnd: number
  status: 'pending' | 'paid' | 'failed'
  created_at: string
  paid_at: string | null
}

//  Webshop 
export type ItemType = 'skin' | 'cosmetic' | 'pack' | 'mount' | 'pet' | 'pass'

export interface WebshopItem {
  id: number
  name: string
  description: string
  item_type: ItemType
  class_id: number          // 0 = all classes
  diamond_price: number
  original_price: number
  is_limited: boolean
  stock: number             // -1 = unlimited, 0 = sold out
  total_sold: number
  is_featured: boolean
  icon_url: string | null
  preview_url: string | null
  sort_order: number
  per_user_limit: number    // -1 = no limit, >0 = max purchases per user
  per_user_period: 'all' | 'daily' | 'weekly' | 'monthly'
  pass_season_id: number    // >0 = this item unlocks premium pass for that season
  // Populated by server if authenticated
  user_purchased?: number
  can_buy?: boolean
}

export interface BuyResult {
  success: boolean
  message: string
}

//  Gift Code 
export interface RedeemResult {
  success: boolean
  message: string
  rewards: string[]
}

//  Mission Pass 
export interface PassSeason {
  id: number
  name: string
  description: string
  start_date: string
  end_date: string
  free_diamond: number
  premium_diamond: number
  max_level: number
  is_active: boolean
}

export interface PassReward {
  id: number
  season_id: number
  level: number
  tier: 0 | 1             // 0=free, 1=premium
  item_id: number
  item_qty: number
  diamond: number
  gold: number
  description: string
}

export interface PlayerPass {
  char_id: number
  season_id: number
  pass_level: number
  pass_exp: number
  has_premium: boolean
  claimed_rewards: string[]
}

//  Title 
export interface Title {
  id: number
  name: string
  description: string
  title_type: string
  color_hex: string
  icon_id: number
}

//  Pet / Mount 
export type Element = 'fire' | 'ice' | 'lightning' | 'none'
export type Rarity = 1 | 2 | 3 | 4 | 5

export interface PetTemplate {
  id: number
  name: string
  element: Element
  rarity: Rarity
  base_hp: number
  base_atk: number
  base_def: number
  icon_id: number
  obtain_source: string
}

export interface MountTemplate {
  id: number
  name: string
  speed_bonus: number
  rarity: Rarity
  icon_id: number
  obtain_source: string
}

//  Pagination 
export interface PaginatedResponse<T> {
  success: boolean
  data: T[]
  total: number
  page: number
  per_page: number
}

//  Common 
export interface ApiError {
  success: false
  message: string
  code?: string
}

export type ApiResult<T> = T | ApiError
