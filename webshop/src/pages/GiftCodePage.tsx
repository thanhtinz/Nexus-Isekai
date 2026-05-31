// pages/GiftCodePage.tsx
import { useState } from 'react'
import { Gift, CheckCircle2, AlertCircle, Tag, Loader2 } from 'lucide-react'
import { shopApi } from '@/api/client'
import { useAuth } from '@/hooks/useAuth'
import clsx from 'clsx'

export function GiftCodePage() {
  const { session } = useAuth()
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<{ success: boolean; message: string; rewards?: string[] } | null>(null)

  const handleRedeem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!session || !code.trim()) return
    setLoading(true)
    setResult(null)
    try {
      const res = await shopApi.redeem(
        code.trim().toUpperCase(),
        session.accountId,
        session.charId,
        session.level,
        session.token
      )
      setResult(res)
      if (res.success) setCode('')
    } finally {
      setLoading(false)
    }
  }

  if (!session) return null

  return (
    <div className="max-w-xl mx-auto px-4 py-16">
      {/* Decorative glow */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[500px] h-[300px]
                        bg-brand-500/8 blur-[100px] rounded-full" />
      </div>

      <div className="relative space-y-8">
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-brand-500/20 border border-brand-500/30 mb-2">
            <Gift size={28} className="text-brand-400" />
          </div>
          <h1 className="font-display text-2xl font-bold text-[#2a2350]">Gift Code</h1>
          <p className="text-[#5b538080] text-sm">
            Nhập mã code để nhận vật phẩm, diamond, danh hiệu và nhiều phần thưởng khác
          </p>
        </div>

        {/* Input form */}
        <form onSubmit={handleRedeem} className="card p-6 space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-[#5b5380]/70 uppercase tracking-wide">
              Mã Gift Code
            </label>
            <div className="relative">
              <Tag size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#5b5380]/40" />
              <input
                value={code}
                onChange={e => setCode(e.target.value.toUpperCase())}
                className="input pl-9 font-mono tracking-widest text-center uppercase text-base"
                placeholder="NEXUS-XXXX-XXXX"
                maxLength={32}
                autoComplete="off"
                spellCheck={false}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || !code.trim()}
            className="btn-primary w-full justify-center py-3"
          >
            {loading ? <Loader2 size={16} className="animate-spin" /> : <Gift size={16} />}
            {loading ? 'Đang đổi code...' : 'Nhận Thưởng'}
          </button>
        </form>

        {/* Result */}
        {result && (
          <div className={clsx(
            'card p-5 space-y-3 border',
            result.success
              ? 'border-emerald-500/30 bg-emerald-500/5'
              : 'border-red-500/30 bg-red-500/5'
          )}>
            <div className="flex items-center gap-2">
              {result.success
                ? <CheckCircle2 size={18} className="text-emerald-400" />
                : <AlertCircle size={18} className="text-red-400" />
              }
              <span className={clsx(
                'font-semibold text-sm',
                result.success ? 'text-emerald-400' : 'text-red-400'
              )}>
                {result.success ? 'Đổi code thành công!' : 'Không thể đổi code'}
              </span>
            </div>

            <p className="text-sm text-[#5b5380]/70">{result.message}</p>

            {result.success && result.rewards && result.rewards.length > 0 && (
              <div className="space-y-1.5 pt-2 border-t border-[#ece3d0]">
                <p className="text-xs font-medium text-[#5b538080] uppercase tracking-wide">
                  Phần thưởng nhận được
                </p>
                <div className="flex flex-wrap gap-2">
                  {result.rewards.map((r, i) => (
                    <span key={i} className="px-2.5 py-1 bg-brand-500/20 text-brand-300 text-xs rounded-full border border-brand-500/20">
                      {r}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Info */}
        <div className="card p-4 space-y-2">
          <h3 className="text-sm font-semibold text-[#2a2350]">Lưu ý</h3>
          <ul className="space-y-1.5 text-xs text-[#5b538080]">
            <li className="flex items-start gap-1.5">
              <span className="text-brand-400 mt-0.5 flex-shrink-0">•</span>
              Mỗi code chỉ có thể sử dụng một lần cho mỗi nhân vật.
            </li>
            <li className="flex items-start gap-1.5">
              <span className="text-brand-400 mt-0.5 flex-shrink-0">•</span>
              Code có thể có giới hạn thời gian và số lần sử dụng.
            </li>
            <li className="flex items-start gap-1.5">
              <span className="text-brand-400 mt-0.5 flex-shrink-0">•</span>
              Vật phẩm nhận được sẽ vào túi đồ nhân vật đang chọn.
            </li>
          </ul>
        </div>
      </div>
    </div>
  )
}
