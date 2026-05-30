// App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { LoginPage } from '@/pages/LoginPage'
import { TopupPage } from '@/pages/TopupPage'
import { ShopPage } from '@/pages/ShopPage'
import { GiftCodePage } from '@/pages/GiftCodePage'
import { PassPage } from '@/pages/PassPage'
import { useAuth } from '@/hooks/useAuth'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { session } = useAuth()
  if (!session) return <Navigate to="/login" replace />
  return <>{children}</>
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <Layout>
              <Routes>
                <Route path="/" element={<Navigate to="/topup" replace />} />
                <Route path="/topup" element={<PrivateRoute><TopupPage /></PrivateRoute>} />
                <Route path="/shop" element={<PrivateRoute><ShopPage /></PrivateRoute>} />
                <Route path="/giftcode" element={<PrivateRoute><GiftCodePage /></PrivateRoute>} />
                <Route path="/pass" element={<PrivateRoute><PassPage /></PrivateRoute>} />
              </Routes>
            </Layout>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}
