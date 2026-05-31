// App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from '@/components/Layout'
import { LoginPage } from '@/pages/LoginPage'
import { TopupPage } from '@/pages/TopupPage'
import { ShopPage } from '@/pages/ShopPage'
import { GiftCodePage } from '@/pages/GiftCodePage'
import { PassPage } from '@/pages/PassPage'
import { useAuth } from '@/hooks/useAuth'
import LandingPage from '@/pages/LandingPage'
import AdminDashboard from '@/pages/AdminDashboard'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { session } = useAuth()
  if (!session) return <Navigate to="/login" replace />
  return <>{children}</>
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        
            <Route path="/store" element={<GameStorePage />} />
            <Route path="/download" element={<DownloadPage />} />
            <Route path="/news" element={<NewsPage />} />
            <Route path="/support" element={<SupportPage />} />
            <Route path="/sys/internal/v2/dashboard" element={<AdminDashboard />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <Layout>
              <Routes>
</PrivateRoute>} />
</PrivateRoute>} />
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
