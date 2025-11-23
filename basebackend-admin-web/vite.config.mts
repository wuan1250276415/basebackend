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
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 系统服务 API
      '/basebackend-system-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // auth.ts 等已包含完整路径的接口 (admin-api/api/admin/auth/**)
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // workflow 服务 - 转换为 scheduler-service
      '/api/workflow': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/workflow/, '/scheduler-service'),
      },
      // user.ts 等简化路径的接口 (/admin/**) - 转换为 admin-api/api/admin/**
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/admin/, '/admin-api/api/admin'),
      },
      // 其他 API 请求走默认网关路由
      '/api': {
        target: 'http://localhost:8080',
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
