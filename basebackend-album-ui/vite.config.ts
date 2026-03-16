import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5174,
    proxy: {
      '/api/album': {
        target: 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api/user': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      }
    }
  }
})
