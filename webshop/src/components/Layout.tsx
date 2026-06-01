// components/Layout.tsx — light fantasy theme (đồng bộ landing)
import { Link, useLocation } from 'react-router-dom'
import ServerSelector from './ServerSelector'
import { Gem, ShoppingBag, Gift, BookOpen, User, LogOut, Menu, X } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import clsx from 'clsx'

const NAV_ITEMS = [
  { path: '/topup', label: 'Nạp Game', icon: Gem },
  { path: '/shop', label: 'Cửa Hàng', icon: ShoppingBag },
  { path: '/pass', label: 'Sổ Sứ Mệnh', icon: BookOpen },
  { path: '/giftcode', label: 'Gift Code', icon: Gift },
]

function DiamondBalance({ amount }: { amount: number }) {
  return (
    <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full" style={{ background: '#e0a02018', border: '1px solid #e0a02033' }}>
      <Gem size={14} style={{ color: '#e0a020' }} />
      <span className="text-sm font-semibold tabular-nums" style={{ color: '#b07d10' }}>{amount.toLocaleString('vi-VN')}</span>
    </div>
  )
}

export function Header() {
  const { session, logout } = useAuth()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <header className="fixed top-0 inset-x-0 z-50 border-b" style={{ background: 'rgba(251,247,238,.85)', backdropFilter: 'blur(10px)', borderColor: '#ece3d0' }}>
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex items-center h-16 gap-6">
          <Link to="/" className="flex items-center gap-2.5 flex-shrink-0 no-underline">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: 'linear-gradient(135deg,#6c3ef3,#4820b8)' }}>
              <span className="font-bold text-white text-sm" style={{ fontFamily: "'Cinzel',serif" }}>VL</span>
            </div>
            <span className="font-bold text-lg hidden sm:block" style={{ fontFamily: "'Cinzel',serif", color: '#2a2350' }}>Vọng Linh Giới</span>
          </Link>

          <nav className="hidden md:flex items-center gap-1 flex-1">
            {NAV_ITEMS.map(({ path, label, icon: Icon }) => {
              const active = location.pathname.startsWith(path)
              return (
                <Link key={path} to={path} className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors no-underline"
                  style={{ background: active ? '#6c3ef315' : 'transparent', color: active ? '#6c3ef3' : '#5b5380' }}>
                  <Icon size={15} />{label}
                </Link>
              )
            })}
            <ServerSelector />
          </nav>

          <div className="flex items-center gap-3 ml-auto">
            {session ? (
              <>
                <DiamondBalance amount={session.diamond} />
                <div className="hidden sm:flex items-center gap-2 text-sm" style={{ color: '#5b5380' }}>
                  <User size={15} />
                  <span className="font-medium">{session.charName}</span>
                  <span style={{ color: '#5b538080' }}>Lv.{session.level}</span>
                </div>
                {session.isAdmin === 1 && (<>
                  <a href="/sys/internal/v2/dashboard" className="hidden sm:inline text-xs px-3 py-1.5 rounded-lg font-semibold no-underline" style={{ background: '#6c3ef320', color: '#6c3ef3' }}>Admin</a>
                  <a href={(import.meta as any).env?.VITE_STUDIO_URL || '/studio/'} className="hidden sm:inline text-xs px-3 py-1.5 rounded-lg font-semibold no-underline" style={{ background: '#d4a82020', color: '#b8860b' }}>Studio</a>
                </>)}
                <button onClick={logout} className="p-2 rounded-lg transition-colors" style={{ color: '#5b538099' }} title="Đăng xuất"><LogOut size={16} /></button>
              </>
            ) : (
              <Link to="/login" className="text-sm px-4 py-2 rounded-lg font-bold text-white no-underline" style={{ background: 'linear-gradient(135deg,#6c3ef3,#4820b8)' }}>Đăng nhập</Link>
            )}
            <button className="md:hidden p-2 rounded-lg" style={{ color: '#5b5380' }} onClick={() => setMobileOpen(v => !v)}>
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>

        {mobileOpen && (
          <div className="md:hidden pb-3 border-t pt-2" style={{ borderColor: '#ece3d0' }}>
            {NAV_ITEMS.map(({ path, label, icon: Icon }) => {
              const active = location.pathname.startsWith(path)
              return (
                <Link key={path} to={path} onClick={() => setMobileOpen(false)}
                  className="flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium no-underline"
                  style={{ background: active ? '#6c3ef315' : 'transparent', color: active ? '#6c3ef3' : '#5b5380' }}>
                  <Icon size={16} />{label}
                </Link>
              )
            })}
            <div className="px-3 pt-2"><ServerSelector /></div>
          </div>
        )}
      </div>
    </header>
  )
}

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen" style={{ background: '#fbf7ee', color: '#2a2350', fontFamily: "'Be Vietnam Pro',sans-serif" }}>
      <Header />
      <main className="pt-16 min-h-screen">{children}</main>
      <footer className="border-t py-6 text-center text-xs" style={{ borderColor: '#ece3d0', color: '#5b538099' }}>
        &copy; {new Date().getFullYear()} Nexus Isekai — Vọng Linh Giới. Đăng ký tài khoản trong game.
      </footer>
    </div>
  )
}
