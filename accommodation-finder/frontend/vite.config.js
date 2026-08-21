import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies the API and the WebSocket to Spring Boot on :8080,
// so the browser only ever talks to one origin and CORS never bites in development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  define: {
    // sockjs-client expects a browser `global`.
    global: 'globalThis',
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        // The map and the realtime transport are each large and each only
        // needed by part of the app, so they get their own long-lived chunks
        // instead of riding along in the entry bundle.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          motion: ['framer-motion'],
          map: ['leaflet', 'react-leaflet'],
          realtime: ['@stomp/stompjs', 'sockjs-client'],
        },
      },
    },
  },
});
