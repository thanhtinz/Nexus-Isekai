// hooks/useAuth.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Session } from '@/types/api'
import { authApi, setAuthStore } from '@/api/client'

interface AuthState {
  session: Session | null
  isFirstTopup: boolean
  loading: boolean
  error: string | null
  login: (username: string, password: string) => Promise<boolean>
  logout: () => void
  updateDiamond: (amount: number) => void
}

export const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      session: null,
      isFirstTopup: false,
      loading: false,
      error: null,

      login: async (username, password) => {
        set({ loading: true, error: null })
        try {
          const res = await authApi.login(username, password)
          if (res.success && res.session) {
            set({ session: res.session, isFirstTopup: res.isFirstTopup ?? false, loading: false })
            return true
          } else {
            set({ error: res.message ?? 'Đăng nhập thất bại', loading: false })
            return false
          }
        } catch (e: any) {
          set({ error: e?.response?.data?.message ?? 'Lỗi kết nối', loading: false })
          return false
        }
      },

      logout: () => {
        set({ session: null, isFirstTopup: false })
        localStorage.removeItem('nexus-session')
      },

      updateDiamond: (amount) => {
        const s = get().session
        if (s) set({ session: { ...s, diamond: amount } })
      },
    }),
    {
      name: 'nexus-session',
      partialize: (s) => ({ session: s.session }),
    }
  )
)

// Inject store reference into API client
setAuthStore(useAuth)
