// pages/PassPage.tsx
import { useEffect, useState } from 'react'
import {
  BookOpen, Lock, CheckCircle2, ChevronRight,
  Gem, Coins, Package, Crown, Star, Loader2
} from 'lucide-react'
import type { PassSeason, PassReward, PlayerPass } from '@/types/api'
import { useAuth } from '@/hooks/useAuth'
import clsx from 'clsx'

//  Reward Cell 
function RewardCell({
  reward,
  claimed,
  unlocked,
}: {
  reward: PassReward
  claimed: boolean
  unlocked: boolean
}) {
  const isPremium = reward.tier === 1

  return (
    <div className={clsx(
      'relative flex flex-col items-center gap-1.5 p-2.5 rounded-lg border transition-all',
      claimed
        ? 'bg-emerald-500/10 border-emerald-500/30'
        : unlocked
          ? isPremium
            ? 'bg-gold-500/10 border-gold-500/30 hover:border-gold-500/50 cursor-pointer'
            : 'bg-brand-500/10 border-brand-500/30 hover:border-brand-500/50 cursor-pointer'
          : 'bg-[#fffdf7] border-[#ece3d0] opacity-40'
    )}>
      {/* Icon */}
      <div className={clsx(
        'w-10 h-10 rounded-lg flex items-center justify-center',
        isPremium ? 'bg-gold-500/20' : 'bg-brand-500/20'
      )}>
        {reward.item_id > 0 ? (
          <Package size={18} className={isPremium ? 'text-gold-500' : 'text-brand-400'} />
        ) : reward.diamond > 0 ? (
          <Gem size={18} className="text-gold-500" />
        ) : (
          <Coins size={18} className="text-yellow-500" />
        )}
      </div>

      {/* Amount */}
      <span className="text-xs font-semibold text-[#2a2350] text-center leading-tight">
        {reward.item_id > 0 ? `x${reward.item_qty}` : reward.diamond > 0 ? `${reward.diamond}` : `${reward.gold}`}
      </span>

      {/* Claimed check */}
      {claimed && (
        <div className="absolute -top-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full flex items-center justify-center">
          <CheckCircle2 size={10} className="text-[#2a2350]" />
        </div>
      )}

      {/* Lock */}
      {!unlocked && (
        <div className="absolute inset-0 flex items-center justify-center">
          <Lock size={16} className="text-[#5b5380]/30" />
        </div>
      )}

      {/* Premium crown */}
      {isPremium && (
        <div className="absolute -top-1.5 left-1/2 -translate-x-1/2">
          <Crown size={10} className="text-gold-500" fill="currentColor" />
        </div>
      )}
    </div>
  )
}

//  Level Row 
function LevelRow({
  level,
  freeReward,
  premiumReward,
  passLevel,
  hasPremium,
  claimedKeys,
}: {
  level: number
  freeReward?: PassReward
  premiumReward?: PassReward
  passLevel: number
  hasPremium: boolean
  claimedKeys: Set<string>
}) {
  const unlocked = passLevel >= level
  const isCurrent = passLevel === level

  return (
    <div className={clsx(
      'flex items-center gap-3 p-2 rounded-xl',
      isCurrent && 'bg-brand-500/5 ring-1 ring-brand-500/20'
    )}>
      {/* Level number */}
      <div className={clsx(
        'w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0',
        unlocked ? 'bg-brand-500 text-[#2a2350]' : 'bg-[#fffdf7] text-[#5b5380]/30 border border-[#ece3d0]'
      )}>
        {level}
      </div>

      {/* Free reward */}
      <div className="flex-1">
        {freeReward ? (
          <RewardCell
            reward={freeReward}
            claimed={claimedKeys.has(`${level}_0`)}
            unlocked={unlocked}
          />
        ) : (
          <div className="h-16 rounded-lg border border-dashed border-[#ece3d0]" />
        )}
      </div>

      {/* Separator */}
      <ChevronRight size={14} className="text-[#5b5380]/20 flex-shrink-0" />

      {/* Premium reward */}
      <div className="flex-1 relative">
        {premiumReward ? (
          <RewardCell
            reward={premiumReward}
            claimed={claimedKeys.has(`${level}_1`)}
            unlocked={unlocked && hasPremium}
          />
        ) : (
          <div className="h-16 rounded-lg border border-dashed border-[#ece3d0]" />
        )}
      </div>
    </div>
  )
}

//  Progress Bar 
function ProgressBar({ current, max }: { current: number; max: number }) {
  const pct = Math.min(100, (current / max) * 100)
  return (
    <div className="w-full bg-[#fffdf7] rounded-full h-2 overflow-hidden border border-[#ece3d0]">
      <div
        className="h-full bg-gradient-to-r from-brand-500 to-brand-400 rounded-full transition-all duration-500"
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}

//  Main 
export function PassPage() {
  const { session } = useAuth()
  const [season, setSeason] = useState<PassSeason | null>(null)
  const [rewards, setRewards] = useState<PassReward[]>([])
  const [playerPass, setPlayerPass] = useState<PlayerPass | null>(null)
  const [loading, setLoading] = useState(true)
  const [buyingPremium, setBuyingPremium] = useState(false)

  // Fetch via admin API (season info is public)
  useEffect(() => {
    fetch('/api/pass/active')
      .then(r => r.json())
      .then(d => {
        setSeason(d.season)
        setRewards(d.rewards ?? [])
        setPlayerPass(d.player_pass)
        setLoading(false)
      })
      .catch(() => setLoading(false))
  }, [session])

  const claimedKeys = new Set(playerPass?.claimed_rewards ?? [])

  // Group rewards by level
  const rewardsByLevel = new Map<number, { free?: PassReward; premium?: PassReward }>()
  if (season) {
    for (let l = 1; l <= season.max_level; l++) rewardsByLevel.set(l, {})
  }
  rewards.forEach(r => {
    const existing = rewardsByLevel.get(r.level) ?? {}
    if (r.tier === 0) existing.free = r
    else existing.premium = r
    rewardsByLevel.set(r.level, existing)
  })

  if (!session) return null

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-6">
        <BookOpen size={22} className="text-brand-400" />
        <h1 className="font-display text-2xl font-bold text-[#2a2350]">Sổ Sứ Mệnh</h1>
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="skeleton h-20 rounded-xl" />
          ))}
        </div>
      ) : !season ? (
        <div className="card p-12 text-center">
          <BookOpen size={36} className="text-[#5b5380]/20 mx-auto mb-3" />
          <p className="text-[#5b5380]/40">Chưa có season nào đang hoạt động</p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Season card */}
          <div className="card p-5 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="font-display text-lg font-semibold text-[#2a2350]">{season.name}</h2>
                <p className="text-sm text-[#5b538080] mt-0.5">{season.description}</p>
                <p className="text-xs text-[#5b5380]/40 mt-1">
                  {season.start_date} — {season.end_date}
                </p>
              </div>
              {!playerPass?.has_premium && (
                <button
                  onClick={() => setBuyingPremium(true)}
                  disabled={buyingPremium}
                  className="btn-gold flex-shrink-0 text-sm px-4 py-2"
                >
                  {buyingPremium ? <Loader2 size={14} className="animate-spin" /> : <Crown size={14} />}
                  Premium
                  <span className="text-xs opacity-80 ml-1">
                    {season.premium_diamond.toLocaleString()}
                  </span>
                  <Gem size={11} className="opacity-80" />
                </button>
              )}
              {playerPass?.has_premium && (
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gold-500/20 rounded-full border border-gold-500/30">
                  <Crown size={14} className="text-gold-500" />
                  <span className="text-xs font-semibold text-gold-400">Premium</span>
                </div>
              )}
            </div>

            {/* Progress */}
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[#5b538099]">
                  Level <span className="text-[#2a2350] font-semibold">{playerPass?.pass_level ?? 1}</span>
                  /{season.max_level}
                </span>
                <span className="text-[#5b5380]/40 text-xs tabular-nums">
                  {playerPass?.pass_exp ?? 0} / 100 EXP
                </span>
              </div>
              <ProgressBar current={playerPass?.pass_exp ?? 0} max={100} />
            </div>
          </div>

          {/* Column headers */}
          <div className="flex items-center gap-3 px-2">
            <div className="w-10 text-center text-xs font-medium text-[#5b5380]/40 uppercase">Lv</div>
            <div className="flex-1 text-center text-xs font-medium text-[#5b538099] uppercase">
              Miễn Phí
            </div>
            <div className="w-5" />
            <div className="flex-1 text-center text-xs font-medium text-gold-500/70 uppercase flex items-center justify-center gap-1">
              <Crown size={11} />
              Premium
            </div>
          </div>

          {/* Reward rows */}
          <div className="space-y-1.5 card p-3">
            {Array.from(rewardsByLevel.entries())
              .sort(([a], [b]) => a - b)
              .map(([level, { free, premium }]) => (
                <LevelRow
                  key={level}
                  level={level}
                  freeReward={free}
                  premiumReward={premium}
                  passLevel={playerPass?.pass_level ?? 0}
                  hasPremium={playerPass?.has_premium ?? false}
                  claimedKeys={claimedKeys}
                />
              ))}
          </div>

          {/* Legend */}
          <div className="flex items-center gap-4 text-xs text-[#5b5380]/40 px-2">
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded-full bg-brand-500" />
              <span>Đã mở khoá</span>
            </div>
            <div className="flex items-center gap-1.5">
              <CheckCircle2 size={12} className="text-emerald-400" />
              <span>Đã nhận</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Crown size={12} className="text-gold-500" />
              <span>Cần Premium</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
