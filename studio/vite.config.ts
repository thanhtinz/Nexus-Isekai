import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Tool doc lap. API tro toi server game qua bien moi truong VITE_API_BASE
// (vd http://localhost:9090). Khi dev co the dung proxy duoi day.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    host: '0.0.0.0',
    proxy: {
      '/api': { target: process.env.VITE_API_BASE || 'http://localhost:9090', changeOrigin: true },
    },
  },
})
