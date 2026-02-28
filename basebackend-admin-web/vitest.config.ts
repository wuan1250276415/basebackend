import { defineConfig } from 'vitest/config';
import path from 'path';

// Vitest 测试配置
export default defineConfig({
  resolve: {
    alias: {
      // 与 vite.config.mts 保持一致的路径别名
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    // 使用 jsdom 模拟浏览器环境
    environment: 'jsdom',
    // 设置 jsdom URL，确保 localStorage 等 API 正常工作
    environmentOptions: {
      jsdom: {
        url: 'http://localhost',
      },
    },
    // 全局注入测试 API（describe, it, expect 等）
    globals: true,
    // 测试启动前执行的配置文件
    setupFiles: ['./src/__tests__/setup.ts'],
  },
});
