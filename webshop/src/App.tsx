// App.tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import { ServerProvider } from '@/context/ServerContext'
import ProtectedRoute from '@/components/ProtectedRoute'
import { Layout } from '@/components/Layout'
import LandingPage from '@/pages/LandingPage'
import { LoginPage } from '@/pages/LoginPage'
import { GiftCodePage } from '@/pages/GiftCodePage'
import { PassPage } from '@/pages/PassPage'
import RankingPage from '@/pages/RankingPage'
import GameStorePage from '@/pages/GameStorePage'
import DownloadPage from '@/pages/DownloadPage'
import NewsPage from '@/pages/NewsPage'
import SupportPage from '@/pages/SupportPage'
import AdminDashboard from '@/pages/AdminDashboard'
import StudioApp from '@/pages/StudioApp'

const withLayout = (el: React.ReactNode) => <Layout>{el}</Layout>

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ServerProvider>
          <Routes>
            {/* Standalone (no nav layout) */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/sys/internal/v2/dashboard" element={<AdminDashboard />} />
            <Route path="/studio" element={<StudioApp />} />

            {/* Public pages (with layout) */}
            <Route path="/download" element={withLayout(<DownloadPage />)} />
            <Route path="/news" element={withLayout(<NewsPage />)} />
            <Route path="/support" element={withLayout(<SupportPage />)} />

            {/* Gated: cần login + chọn server + chọn nhân vật */}
            <Route path="/store" element={withLayout(<ProtectedRoute><GameStorePage /></ProtectedRoute>)} />
            <Route path="/ranking" element={withLayout(<ProtectedRoute><RankingPage /></ProtectedRoute>)} />
            <Route path="/giftcode" element={withLayout(<ProtectedRoute><GiftCodePage /></ProtectedRoute>)} />
            <Route path="/pass" element={withLayout(<ProtectedRoute><PassPage /></ProtectedRoute>)} />
          </Routes>
        </ServerProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
