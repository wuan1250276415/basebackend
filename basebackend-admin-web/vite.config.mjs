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
            '/api/user': {
                target: 'http://localhost:8081',
                changeOrigin: true,
            },
            '/api/system': {
                target: 'http://localhost:8082',
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
