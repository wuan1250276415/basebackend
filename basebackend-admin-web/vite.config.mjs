import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
// Vite 构建配置
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            // 路径别名，@ 指向 src 目录
            '@': path.resolve(__dirname, './src'),
        },
    },
    css: {
        // Less 预处理器配置
        preprocessorOptions: {
            less: {
                javascriptEnabled: true,
                modifyVars: {
                    // Ant Design 主题色
                    '@primary-color': '#1677ff',
                },
            },
        },
    },
    server: {
        port: 3000,
        open: true,
        proxy: {
            // 统一代理 /api 请求到后端网关
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            // 代理微服务前缀的请求到后端网关
            '^/basebackend-.*': {
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
});
