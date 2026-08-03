import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isDevelopment = mode === 'development'

  return {
    base: '/',
    plugins: [vue()],
    resolve: {
      extensions: ['.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json', '.vue'],
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    define: {
      'process.env': JSON.stringify({
        NODE_ENV: isDevelopment ? 'development' : 'production',
        VUE_APP_BASE_API: env.VUE_APP_BASE_API || ''
      })
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.PORT || 9528),
      open: true,
      proxy: {
        '/dev-api': {
          target: 'http://127.0.0.1:18080',
          changeOrigin: true,
          ws: true,
          rewrite: path => path.replace(/^\/dev-api/, '')
        },
        '/static/snap': {
          target: 'http://127.0.0.1:18080',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: '../src/main/resources/static',
      assetsDir: 'static',
      emptyOutDir: true,
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            'ant-design': ['ant-design-vue', '@ant-design/icons-vue'],
            charts: ['echarts'],
            maps: ['ol']
          }
        }
      }
    }
  }
})
