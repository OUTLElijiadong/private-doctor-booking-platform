import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-element': ['element-plus', '@element-plus/icons-vue'],
          'vendor-echarts': ['echarts'],
          'vendor-vue': ['vue', 'vue-router', 'pinia', 'axios', 'dayjs']
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 可通过环境变量覆盖后端地址：VITE_API_TARGET=http://localhost:8090 npm run dev
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
