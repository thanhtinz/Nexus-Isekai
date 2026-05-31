// pages/TopupPage.tsx
import { useEffect, useState, useRef } from 'react'
import {
  Gem, RefreshCw, ChevronRight, Clock, CheckCircle2, XCircle,
  Copy, Check, AlertTriangle, QrCode, History, Star
} from 'lucide-react'
import { topupApi } from '@/api/client'
import { useAuth } from '@/hooks/useAuth'
import type { TopupPackage, TopupOrder, TopupHistoryItem } from '@/types/api'
import clsx from 'clsx'

//  Package Card 
function PackageCard({
  pkg, selected, onClick,
}: {
  pkg: TopupPackage
  selected: boolean
  onClick: () => void
}) {
  const totalDiamond = pkg.diamond + pkg.bonus_diamond

  return (
    <button
      onClick={onClick}
      className={clsx(
        'relative card-hover p-5 text-left transition-all duration-200 cursor-pointer w-full',
        selected && 'ring-2 ring-brand-500 bg-brand-500/10 border-brand-500/40',
        pkg.is_featured && 'border-gold-500/30'
      )}
    >
      {pkg.is_featured && (
        <div className="ribbon flex items-center gap-1">
          <Star size={10} />
          HOT
        </div>
      )}

      {/* Diamond icon */}
      <div className={clsx(
        'w-12 h-12 rounded-xl flex items-center justify-center mb-3',
        totalDiamond >= 5000 ? 'bg-gold-500/20' :
        totalDiamond >= 1000 ? 'bg-purple-500/20' :
        totalDiamond >= 500  ? 'bg-blue-500/20' : 'bg-brand-500/20'
      )}>
        <Gem size={22} className={clsx(
          totalDiamond >= 5000 ? 'text-gold-500' :
          totalDiamond >= 1000 ? 'text-purple-400' :
          totalDiamond >= 500  ? 'text-blue-400' : 'text-brand-400'
        )} />
      </div>

      {/* Amount */}
      <div className="text-2xl font-display font-bold text-surface-100">
        {totalDiamond.toLocaleString('vi-VN')}
      </div>
      <div className="text-xs text-gold-500/80 font-medium mt-0.5">
        {pkg.bonus_diamond > 0 ? `${pkg.diamond.toLocaleString()} + ${pkg.bonus_diamond.toLocaleString()} thưởng` : 'Diamond'}
      </div>

      {/* Price */}
      <div className="mt-3 pt-3 border-t border-white/5">
        <span className="text-surface-100 font-semibold">
          {pkg.price_vnd.toLocaleString('vi-VN')}đ
        </span>
      </div>

      {/* Selection indicator */}
      {selected && (
        <div className="absolute top-3 right-3 w-5 h-5 rounded-full bg-brand-500 flex items-center justify-center">
          <Check size={12} className="text-white" />
        </div>
      )}
    </button>
  )
}

//  Copy Button 
function CopyButton({ value }: { value: string }) {
  const [copied, setCopied] = useState(false)
  const copy = () => {
    navigator.clipboard.writeText(value)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button
      onClick={copy}
      className="p-1.5 rounded hover:bg-white/5 text-surface-200/50 hover:text-surface-200 transition-colors"
      title="Sao chép"
    >
      {copied ? <Check size={13} className="text-emerald-400" /> : <Copy size={13} />}
    </button>
  )
}

//  Transfer Info Row 
function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
      <span className="text-xs text-surface-200/50">{label}</span>
      <div className="flex items-center gap-1.5">
        <span className="text-sm font-medium text-surface-100">{value}</span>
        <CopyButton value={value} />
      </div>
    </div>
  )
}

//  Status Badge 
function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { cls: string; label: string; icon: typeof CheckCircle2 }> = {
    paid:    { cls: 'badge-paid',    label: 'Thành công', icon: CheckCircle2 },
    pending: { cls: 'badge-pending', label: 'Chờ thanh toán', icon: Clock },
    failed:  { cls: 'badge-failed',  label: 'Thất bại', icon: XCircle },
  }
  const { cls, label, icon: Icon } = map[status] ?? map.pending
  return (
    <span className={cls}>
      <Icon size={11} className="mr-1" />
      {label}
    </span>
  )
}

//  Main Page 
export function TopupPage() {
  const { session, updateDiamond, isFirstTopup } = useAuth()
  const [packages, setPackages] = useState<TopupPackage[]>([])
  const [history, setHistory] = useState<TopupHistoryItem[]>([])
  const [loadingPkgs, setLoadingPkgs] = useState(true)
  const [selected, setSelected] = useState<TopupPackage | null>(null)
  const [order, setOrder] = useState<TopupOrder | null>(null)
  const [creating, setCreating] = useState(false)
  const [payStatus, setPayStatus] = useState<'idle' | 'polling' | 'paid' | 'failed'>('idle')
  const [tab, setTab] = useState<'pay' | 'history'>('pay')
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    topupApi.packages().then(p => { setPackages(p); setLoadingPkgs(false) })
    loadHistory()
  }, [])

  const loadHistory = () => {
    topupApi.history().then(setHistory)
  }

  const handlePay = async () => {
    if (!selected || !session) return
    setCreating(true)
    try {
      const res = await topupApi.createOrder(selected.id, session.accountId, session.token)
      if (res.success !== false) {
        setOrder(res)
        setPayStatus('polling')
        startPolling(res.order_id)
      }
    } finally {
      setCreating(false)
    }
  }

  const startPolling = (orderId: string) => {
    let attempts = 0
    pollRef.current = setInterval(async () => {
      attempts++
      if (attempts > 72) { // 6 min timeout
        clearInterval(pollRef.current!)
        setPayStatus('failed')
        return
      }
      const s = await topupApi.orderStatus(orderId)
      if (s.status === 'paid') {
        clearInterval(pollRef.current!)
        setPayStatus('paid')
        if (s.diamond) updateDiamond(session!.diamond + s.diamond)
        topupApi.balance().then(updateDiamond)
        loadHistory()
      } else if (s.status === 'failed') {
        clearInterval(pollRef.current!)
        setPayStatus('failed')
      }
    }, 5000)
  }

  useEffect(() => () => { if (pollRef.current) clearInterval(pollRef.current) }, [])

  if (!session) return null

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* First topup banner */}
      {isFirstTopup && (
        <div className="card mb-6 p-4 border-gold-500/30 bg-gold-500/5">
          <div className="flex items-center gap-3">
            <Star size={18} className="text-gold-500 flex-shrink-0" />
            <div>
              <p className="font-semibold text-gold-400 text-sm">Nạp lần đầu nhận thưởng đặc biệt!</p>
              <p className="text-xs text-surface-200/60 mt-0.5">
                Nhận vật phẩm khởi đầu + nhân vật khi nạp lần đầu tiên.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-1 mb-6 p-1 bg-surface-850 rounded-xl w-fit border border-white/5">
        {([['pay', QrCode, 'Nạp Game'], ['history', History, 'Lịch Sử']] as const).map(([key, Icon, label]) => (
          <button
            key={key}
            onClick={() => { setTab(key as any); if (key === 'history') loadHistory() }}
            className={clsx(
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              tab === key ? 'bg-brand-500 text-white shadow' : 'text-surface-200/60 hover:text-surface-200'
            )}
          >
            <Icon size={14} />
            {label}
          </button>
        ))}
      </div>

      {tab === 'pay' && (
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
          {/* Left: packages */}
          <div className="lg:col-span-3 space-y-4">
            <h2 className="section-header">Chọn gói nạp</h2>
            {loadingPkgs ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="skeleton h-40 rounded-xl" />
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {packages.map(pkg => (
                  <PackageCard
                    key={pkg.id}
                    pkg={pkg}
                    selected={selected?.id === pkg.id}
                    onClick={() => { setSelected(pkg); setOrder(null); setPayStatus('idle') }}
                  />
                ))}
              </div>
            )}

            {selected && !order && (
              <button
                onClick={handlePay}
                disabled={creating}
                className="btn-gold w-full justify-center py-3 text-base mt-2"
              >
                {creating ? <RefreshCw size={16} className="animate-spin" /> : <ChevronRight size={18} />}
                {creating ? 'Đang tạo đơn...' : `Nạp ${selected.diamond + selected.bonus_diamond} Diamond`}
              </button>
            )}
          </div>

          {/* Right: payment info */}
          <div className="lg:col-span-2">
            {order ? (
              <div className="space-y-4 sticky top-20">
                <h2 className="section-header">Thông tin thanh toán</h2>
                <div className="card p-5 space-y-4">
                  {/* QR */}
                  <div className="flex justify-center">
                    <div className="p-3 bg-white rounded-xl">
                      <img
                        src={order.qr_url}
                        alt="QR thanh toán"
                        className="w-44 h-44 block"
                        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                      />
                    </div>
                  </div>

                  {/* Transfer details */}
                  <div className="space-y-0.5">
                    <InfoRow label="Ngân hàng" value={order.bank_name} />
                    <InfoRow label="Số tài khoản" value={order.bank_account} />
                    <InfoRow label="Tên tài khoản" value={order.account_name} />
                    <InfoRow label="Số tiền" value={`${order.amount.toLocaleString('vi-VN')}đ`} />
                    <InfoRow label="Nội dung CK" value={order.transfer_content} />
                  </div>

                  {/* Status */}
                  {payStatus === 'polling' && (
                    <div className="flex items-center gap-2 p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-lg text-yellow-400 text-sm">
                      <RefreshCw size={14} className="animate-spin" />
                      <span>Đang chờ xác nhận thanh toán...</span>
                    </div>
                  )}
                  {payStatus === 'paid' && (
                    <div className="flex items-center gap-2 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-400 text-sm">
                      <CheckCircle2 size={14} />
                      <span>Nạp thành công! Diamond đã được cộng.</span>
                    </div>
                  )}
                  {payStatus === 'failed' && (
                    <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
                      <AlertTriangle size={14} />
                      <span>Hết thời gian hoặc thanh toán thất bại.</span>
                    </div>
                  )}

                  <p className="text-xs text-surface-200/40 text-center">
                    Diamond sẽ được cộng tự động sau khi xác nhận (1-5 phút).
                  </p>
                </div>
              </div>
            ) : (
              <div className="card p-8 text-center border-dashed border-2 border-white/10">
                <Gem size={32} className="text-brand-500/40 mx-auto mb-3" />
                <p className="text-surface-200/50 text-sm">Chọn gói nạp để xem thông tin thanh toán</p>
              </div>
            )}
          </div>
        </div>
      )}

      {tab === 'history' && (
        <div>
          <h2 className="section-header">Lịch sử nạp</h2>
          {history.length === 0 ? (
            <div className="card p-12 text-center">
              <History size={32} className="text-surface-200/20 mx-auto mb-3" />
              <p className="text-surface-200/40 text-sm">Chưa có giao dịch nào</p>
            </div>
          ) : (
            <div className="card overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="text-left px-4 py-3 text-xs font-medium text-surface-200/50 uppercase tracking-wide">
                      Thời gian
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-surface-200/50 uppercase tracking-wide">
                      Gói nạp
                    </th>
                    <th className="text-right px-4 py-3 text-xs font-medium text-surface-200/50 uppercase tracking-wide">
                      Diamond
                    </th>
                    <th className="text-right px-4 py-3 text-xs font-medium text-surface-200/50 uppercase tracking-wide">
                      Số tiền
                    </th>
                    <th className="text-right px-4 py-3 text-xs font-medium text-surface-200/50 uppercase tracking-wide">
                      Trạng thái
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {history.map(h => (
                    <tr key={h.id} className="border-b border-white/5 hover:bg-white/2 transition-colors">
                      <td className="px-4 py-3 text-surface-200/60 tabular-nums text-xs">
                        {new Date(h.created_at).toLocaleString('vi-VN')}
                      </td>
                      <td className="px-4 py-3 text-surface-100">{h.package_name}</td>
                      <td className="px-4 py-3 text-right">
                        <span className="diamond-count text-sm">
                          <Gem size={13} />
                          {(h.diamond + h.bonus_diamond).toLocaleString('vi-VN')}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-surface-200/70 tabular-nums">
                        {(h.amount_vnd / 1000).toFixed(0)}k
                      </td>
                      <td className="px-4 py-3 text-right">
                        <StatusBadge status={h.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
