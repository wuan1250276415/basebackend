import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      // 用户服务 API
      '/basebackend-user-api': {
        target: 'http://192.168.66.126:8280',
        changeOrigin: true,
      },
      // 系统服务 API
      '/basebackend-system-api': {
        target: 'http://192.168.66.126:8280',
        changeOrigin: true,
      },
      // notification
      '/basebackend-notification-service': {
        target: 'http://192.168.66.126:8280',
        changeOrigin: true,
      },
      // workflow 服务 - 转换为 camunda 路径
      '/basebackend-scheduler': {
        target: 'http://192.168.66.126:8280',
        changeOrigin: true,
      },
      // 其他 API 请求走默认网关路由
      '/api': {
        target: 'http://192.168.66.126:8280',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'antd-vendor': ['antd', '@ant-design/icons'],
        },
      },
    },
  },
})
