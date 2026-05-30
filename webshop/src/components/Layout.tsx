// components/Layout.tsx
import { Link, useLocation } from 'react-router-dom'
import {
  Gem, ShoppingBag, Gift, BookOpen, User, LogOut, Menu, X
} from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import clsx from 'clsx'

const NAV_ITEMS = [
  { path: '/topup',   label: 'Nạp Game',    icon: Gem },
  { path: '/shop',    label: 'Cửa Hàng',    icon: ShoppingBag },
  { path: '/pass',    label: 'Sổ Sứ Mệnh',  icon: BookOpen },
  { path: '/giftcode',label: 'Gift Code',    icon: Gift },
]

function DiamondBalance({ amount }: { amount: number }) {
  return (
    <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gold-500/10 border border-gold-500/20">
      <Gem size={14} className="text-gold-500" />
      <span className="text-gold-500 text-sm font-semibold tabular-nums">
        {amount.toLocaleString('vi-VN')}
      </span>
    </div>
  )
}

export function Header() {
  const { session, logout } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <header className="fixed top-0 inset-x-0 z-50 glass border-b border-white/5">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex items-center h-16 gap-6">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 flex-shrink-0">
            <div className="w-8 h-8 rounded-lg bg-brand-500 flex items-center justify-center">
              <span className="font-display font-bold text-white text-sm">NI</span>
            </div>
            <span className="font-display font-bold text-lg text-surface-100 hidden sm:block">
              Nexus Isekai
            </span>
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-1 flex-1">
            {NAV_ITEMS.map(({ path, label, icon: Icon }) => (
              <Link
                key={path}
                to={path}
                className={clsx(
                  'flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                  location.pathname.startsWith(path)
                    ? 'bg-brand-500/20 text-brand-300'
                    : 'text-surface-200 hover:text-surface-100 hover:bg-white/5'
                )}
              >
                <Icon size={15} />
                {label}
              </Link>
            ))}
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-3 ml-auto">
            {session ? (
              <>
                <DiamondBalance amount={session.diamond} />
                <div className="hidden sm:flex items-center gap-2 text-sm text-surface-200">
                  <User size={15} />
                  <span className="font-medium">{session.charName}</span>
                  <span className="text-surface-200/50">Lv.{session.level}</span>
                </div>
                <button
                  onClick={logout}
                  className="p-2 rounded-lg hover:bg-white/5 text-surface-200/60 hover:text-surface-200 transition-colors"
                  title="Đăng xuất"
                >
                  <LogOut size={16} />
                </button>
              </>
            ) : (
              <Link to="/login" className="btn-primary text-sm px-3 py-2">
                Đăng nhập
              </Link>
            )}

            {/* Mobile menu toggle */}
            <button
              className="md:hidden p-2 rounded-lg hover:bg-white/5 text-surface-200"
              onClick={() => setMobileOpen(v => !v)}
            >
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>

        {/* Mobile nav dropdown */}
        {mobileOpen && (
          <div className="md:hidden pb-3 border-t border-white/5 pt-2">
            {NAV_ITEMS.map(({ path, label, icon: Icon }) => (
              <Link
                key={path}
                to={path}
                onClick={() => setMobileOpen(false)}
                className={clsx(
                  'flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium',
                  location.pathname.startsWith(path)
                    ? 'bg-brand-500/20 text-brand-300'
                    : 'text-surface-200'
                )}
              >
                <Icon size={16} />
                {label}
              </Link>
            ))}
          </div>
        )}
      </div>
    </header>
  )
}

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-surface-950">
      <Header />
      <main className="pt-16 min-h-screen">
        {children}
      </main>
      <footer className="border-t border-white/5 py-6 text-center text-xs text-surface-200/40">
        &copy; {new Date().getFullYear()} Nexus Isekai. All rights reserved.
      </footer>
    </div>
  )
}
