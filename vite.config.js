import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.js'],
    css: true,
    testTimeout: 30000,
    hookTimeout: 30000,
    server: {
      deps: {
        inline: ['element-plus', 'async-validator', '@element-plus/icons-vue', '@vueuse/core', '@vueuse/shared']
      }
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      exclude: ['node_modules/', 'src/main.js', '**/*.config.js', 'dist/']
    }
  }
})