// pages/ShopPage.tsx
import { useEffect, useState } from 'react'
import {
  ShoppingBag, Search, SlidersHorizontal, Gem,
  Star, Package, Sword, Zap, Shield, X, Loader2,
  CheckCircle2, AlertCircle, Info, BookOpen
} from 'lucide-react'
import { shopApi } from '@/api/client'
import { useAuth } from '@/hooks/useAuth'
import type { WebshopItem, ItemType } from '@/types/api'
import clsx from 'clsx'

const ITEM_TYPE_LABELS: Record<ItemType, string> = {
  skin:      'Trang Phục',
  cosmetic:  'Trang Trí',
  pack:      'Gói Vật Phẩm',
  mount:     'Thú Cưỡi',
  pet:       'Pet',
  pass:      'Sổ Sứ Mệnh',
}

const RARITY_LABELS = ['', 'Thường', 'Hiếm', 'Sử Thi', 'Huyền Thoại', 'Thiêng Liêng']
const RARITY_COLORS = ['', 'text-gray-400', 'text-green-400', 'text-blue-400', 'text-purple-400', 'text-amber-400']

function RarityStars({ rarity }: { rarity: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          size={10}
          className={i < rarity ? RARITY_COLORS[rarity] : 'text-white/10'}
          fill={i < rarity ? 'currentColor' : 'none'}
        />
      ))}
    </div>
  )
}

const ITEM_TYPE_ICONS: Record<ItemType, typeof ShoppingBag> = {
  skin:     Sword,
  cosmetic: Zap,
  pack:     Package,
  mount:    Shield,
  pet:      Star,
  pass:     BookOpen,
}

function ItemCard({
  item,
  onBuy,
}: {
  item: WebshopItem
  onBuy: (item: WebshopItem) => void
}) {
  const TypeIcon = ITEM_TYPE_ICONS[item.item_type] ?? ShoppingBag
  const soldOut = item.stock === 0
  const hasDiscount = item.original_price > item.diamond_price
  const discountPct = hasDiscount
    ? Math.round((1 - item.diamond_price / item.original_price) * 100)
    : 0

  // Per-user limit status
  const hasLimit     = item.per_user_limit > 0
  const userPurchased = item.user_purchased ?? 0
  const limitReached = hasLimit && userPurchased >= item.per_user_limit
  const limitText = hasLimit
    ? `${userPurchased}/${item.per_user_limit} lần${
        item.per_user_period === 'daily'   ? '/ngày' :
        item.per_user_period === 'weekly'  ? '/tuần' :
        item.per_user_period === 'monthly' ? '/tháng' : ''
      }`
    : null

  const canPurchase = !soldOut && !limitReached && (item.can_buy !== false)

  return (
    <div className={clsx(
      'relative card-hover flex flex-col group overflow-hidden',
      !canPurchase && 'opacity-60'
    )}>
      {/* Featured ribbon */}
      {item.is_featured && (
        <div className="ribbon flex items-center gap-1 text-[10px]">
          <Star size={9} />
          HOT
        </div>
      )}

      {/* Discount badge */}
      {hasDiscount && canPurchase && (
        <div className="absolute top-2 left-2 z-10 px-2 py-0.5 bg-red-500 text-white text-xs font-bold rounded-full">
          -{discountPct}%
        </div>
      )}

      {/* Overlay: sold out / limit reached */}
      {!canPurchase && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/60 rounded-xl">
          <span className="text-white/80 font-bold text-sm bg-black/50 px-3 py-1 rounded-lg">
            {soldOut ? 'Hết Hàng' : limitReached ? 'Đã Mua Hết Lượt' : 'Không Thể Mua'}
          </span>
        </div>
      )}

      {/* Image / icon */}
      <div className="h-36 bg-surface-900 rounded-t-xl overflow-hidden flex items-center justify-center border-b border-white/5">
        {item.icon_url ? (
          <img
            src={item.icon_url}
            alt={item.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <TypeIcon size={40} className="text-brand-500/30" />
        )}
      </div>

      {/* Content */}
      <div className="p-4 flex flex-col flex-1 gap-2">
        {/* Type */}
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-brand-400 uppercase tracking-wide">
            {ITEM_TYPE_LABELS[item.item_type]}
          </span>
          <RarityStars rarity={3} />
        </div>

        <h3 className="font-semibold text-surface-100 text-sm leading-tight">{item.name}</h3>

        {item.description && (
          <p className="text-xs text-surface-200/50 leading-relaxed line-clamp-2">
            {item.description}
          </p>
        )}

        {/* Stock info */}
        <div className="space-y-1">
          {item.is_limited && item.stock > 0 && item.stock <= 50 && (
            <div className="flex items-center gap-1 text-xs text-amber-400/80">
              <Info size={11} />
              <span>Còn {item.stock} sản phẩm</span>
            </div>
          )}
          {limitText && (
            <div className={clsx(
              'flex items-center gap-1 text-xs',
              limitReached ? 'text-red-400/80' : 'text-surface-200/50'
            )}>
              <Info size={11} />
              <span>Đã mua: {limitText}</span>
            </div>
          )}
        </div>

        {/* Price + buy */}
        <div className="mt-auto pt-2 border-t border-white/5 flex items-center justify-between gap-2">
          <div>
            <div className="diamond-count font-bold">
              <Gem size={14} />
              {item.diamond_price.toLocaleString('vi-VN')}
            </div>
            {hasDiscount && (
              <div className="text-xs text-surface-200/40 line-through">
                {item.original_price.toLocaleString('vi-VN')}
              </div>
            )}
          </div>
          <button
            onClick={() => canPurchase && onBuy(item)}
            disabled={!canPurchase}
            className={clsx(
              'btn text-xs px-3 py-1.5',
              canPurchase ? 'btn-primary' : 'btn-secondary cursor-not-allowed'
            )}
          >
            {soldOut ? 'Hết hàng' : limitReached ? 'Hết lượt' : 'Mua'}
          </button>
        </div>
      </div>
    </div>
  )
}

//  Buy Confirm Modal 
function BuyModal({
  item,
  onConfirm,
  onCancel,
  loading,
  result,
}: {
  item: WebshopItem
  onConfirm: () => void
  onCancel: () => void
  loading: boolean
  result: { success: boolean; message: string } | null
}) {
  const { session } = useAuth()
  const canAfford = (session?.diamond ?? 0) >= item.diamond_price

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70" onClick={onCancel} />
      <div className="relative card p-6 w-full max-w-sm space-y-4 animate-in zoom-in-95 duration-150">
        {/* Close */}
        <button onClick={onCancel} className="absolute top-3 right-3 p-1.5 rounded-lg hover:bg-white/5 text-surface-200/50">
          <X size={16} />
        </button>

        <h3 className="font-display text-lg font-semibold text-surface-100">Xác nhận mua hàng</h3>

        {/* Item preview */}
        <div className="flex items-center gap-3 p-3 bg-surface-900 rounded-lg border border-white/5">
          {item.icon_url ? (
            <img src={item.icon_url} alt={item.name} className="w-12 h-12 rounded-lg object-cover" />
          ) : (
            <div className="w-12 h-12 rounded-lg bg-brand-500/20 flex items-center justify-center">
              <ShoppingBag size={20} className="text-brand-400" />
            </div>
          )}
          <div>
            <div className="font-medium text-surface-100 text-sm">{item.name}</div>
            <div className="text-xs text-surface-200/50">{ITEM_TYPE_LABELS[item.item_type]}</div>
          </div>
        </div>

        {/* Price */}
        <div className="flex items-center justify-between p-3 bg-surface-900 rounded-lg">
          <span className="text-sm text-surface-200/70">Chi phí</span>
          <span className="diamond-count font-bold text-lg">
            <Gem size={16} />
            {item.diamond_price.toLocaleString('vi-VN')}
          </span>
        </div>

        {/* Balance */}
        <div className="flex items-center justify-between text-sm">
          <span className="text-surface-200/50">Số dư sau giao dịch</span>
          <span className={clsx('font-semibold', canAfford ? 'text-emerald-400' : 'text-red-400')}>
            {canAfford
              ? `${((session?.diamond ?? 0) - item.diamond_price).toLocaleString('vi-VN')} diamond`
              : 'Không đủ diamond'
            }
          </span>
        </div>

        {/* Result feedback */}
        {result && (
          <div className={clsx(
            'flex items-center gap-2 p-3 rounded-lg text-sm',
            result.success
              ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
              : 'bg-red-500/10 border border-red-500/20 text-red-400'
          )}>
            {result.success ? <CheckCircle2 size={15} /> : <AlertCircle size={15} />}
            <span>{result.message}</span>
          </div>
        )}

        {/* Actions */}
        {!result?.success && (
          <div className="flex gap-2 pt-1">
            <button onClick={onCancel} className="btn-secondary flex-1 justify-center">Huỷ</button>
            <button
              onClick={onConfirm}
              disabled={loading || !canAfford}
              className="btn-gold flex-1 justify-center"
            >
              {loading ? <Loader2 size={14} className="animate-spin" /> : <ShoppingBag size={14} />}
              {loading ? 'Đang xử lý...' : 'Xác nhận mua'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

//  Main Page 
export function ShopPage() {
  const { session, updateDiamond } = useAuth()
  const [items, setItems] = useState<WebshopItem[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [filterType, setFilterType] = useState<ItemType | ''>('')
  const [selectedItem, setSelectedItem] = useState<WebshopItem | null>(null)
  const [buying, setBuying] = useState(false)
  const [buyResult, setBuyResult] = useState<{ success: boolean; message: string } | null>(null)

  useEffect(() => {
    shopApi.items(session?.accountId).then(d => { setItems(d); setLoading(false) })
  }, [session])

  const filtered = items.filter(item => {
    const matchSearch = !search || item.name.toLowerCase().includes(search.toLowerCase())
    const matchType = !filterType || item.item_type === filterType
    return matchSearch && matchType
  })

  const handleBuy = async () => {
    if (!selectedItem || !session) return
    setBuying(true)
    try {
      const res = await shopApi.buy(selectedItem.id, session.accountId, session.charId, session.token)
      setBuyResult(res)
      if (res.success) {
        const newDiamond = (res as any).new_diamond ?? session.diamond - selectedItem.diamond_price
        updateDiamond(newDiamond)
        // Refresh items (stock + user_purchased count updated)
        shopApi.items(session?.accountId).then(setItems)
        setTimeout(() => {
          setSelectedItem(null)
          setBuyResult(null)
        }, 2000)
      }
    } finally {
      setBuying(false)
    }
  }

  const types: Array<{ value: ItemType | ''; label: string }> = [
    { value: '', label: 'Tất cả' },
    { value: 'skin', label: 'Trang phục' },
    { value: 'cosmetic', label: 'Trang trí' },
    { value: 'pack', label: 'Gói đồ' },
    { value: 'mount', label: 'Thú cưỡi' },
    { value: 'pet', label: 'Pet' },
  ]

  if (!session) return null

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl font-bold text-surface-100">Cửa Hàng</h1>
          <p className="text-surface-200/50 text-sm mt-1">
            Vật phẩm độc quyền, trang phục hiếm không có trong shop ingame
          </p>
        </div>
        <div className="flex items-center gap-1.5 px-3 py-2 card rounded-xl">
          <Gem size={16} className="text-gold-500" />
          <span className="font-semibold text-gold-500 tabular-nums">
            {session.diamond.toLocaleString('vi-VN')}
          </span>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-surface-200/40" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="input pl-9"
            placeholder="Tìm kiếm vật phẩm..."
          />
        </div>
        <div className="flex items-center gap-2">
          <SlidersHorizontal size={15} className="text-surface-200/40 flex-shrink-0" />
          <div className="flex items-center gap-1 p-1 bg-surface-850 rounded-lg border border-white/5">
            {types.map(t => (
              <button
                key={t.value}
                onClick={() => setFilterType(t.value as any)}
                className={clsx(
                  'px-3 py-1.5 rounded-md text-xs font-medium transition-colors',
                  filterType === t.value
                    ? 'bg-brand-500 text-white shadow'
                    : 'text-surface-200/60 hover:text-surface-200'
                )}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Grid */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="skeleton h-56 rounded-xl" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="card p-12 text-center">
          <ShoppingBag size={32} className="text-surface-200/20 mx-auto mb-3" />
          <p className="text-surface-200/40">Không tìm thấy vật phẩm</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {filtered.map(item => (
            <ItemCard key={item.id} item={item} onBuy={i => { setSelectedItem(i); setBuyResult(null) }} />
          ))}
        </div>
      )}

      {/* Buy modal */}
      {selectedItem && (
        <BuyModal
          item={selectedItem}
          onConfirm={handleBuy}
          onCancel={() => { setSelectedItem(null); setBuyResult(null) }}
          loading={buying}
          result={buyResult}
        />
      )}
    </div>
  )
}
