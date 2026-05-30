// pages/LoginPage.tsx
import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Eye, EyeOff, Lock, User, Loader2, AlertCircle } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'

export function LoginPage() {
  const { login, loading, error } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as any)?.from ?? '/topup'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const ok = await login(username, password)
    if (ok) navigate(from, { replace: true })
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      {/* Background glow */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2
                        w-[600px] h-[400px] bg-brand-500/10 blur-[120px] rounded-full" />
      </div>

      <div className="relative w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl
                          bg-brand-500 mb-4 shadow-lg shadow-brand-500/30">
            <span className="font-display font-bold text-white text-xl">NI</span>
          </div>
          <h1 className="font-display text-2xl font-bold text-surface-100">Nexus Isekai</h1>
          <p className="text-surface-200/60 text-sm mt-1">Đăng nhập để nạp game và mua vật phẩm</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="card p-6 space-y-4">
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
              <AlertCircle size={15} className="flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-medium text-surface-200/70 uppercase tracking-wide">
              Tên đăng nhập
            </label>
            <div className="relative">
              <User size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-surface-200/40" />
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                className="input pl-9"
                placeholder="username"
                autoComplete="username"
                required
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-medium text-surface-200/70 uppercase tracking-wide">
              Mật khẩu
            </label>
            <div className="relative">
              <Lock size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-surface-200/40" />
              <input
                type={showPw ? 'text' : 'password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="input pl-9 pr-10"
                placeholder="••••••••"
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                onClick={() => setShowPw(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-200/40 hover:text-surface-200/70"
              >
                {showPw ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || !username || !password}
            className="btn-primary w-full justify-center py-2.5"
          >
            {loading ? <Loader2 size={16} className="animate-spin" /> : null}
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>

        <p className="text-center text-xs text-surface-200/40 mt-4">
          Sử dụng tài khoản game để đăng nhập.
        </p>
      </div>
    </div>
  )
}
