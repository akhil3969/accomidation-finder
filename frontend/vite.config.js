import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies the API and the WebSocket to Spring Boot on :8080,
// so the browser only ever talks to one origin and CORS never bites in development.
const proxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/ws': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    ws: true,
  },
};

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  // `npm run build && npm run preview` serves the production bundle with the same
  // proxy - the only honest way to measure load speed on your own machine.
  preview: { port: 4173, proxy },
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
          // framer-motion is deliberately not pinned to one chunk: LazyMotion splits
          // it into a small core (first paint) and the engine (loaded right after).
          map: ['leaflet', 'react-leaflet'],
          realtime: ['@stomp/stompjs', 'sockjs-client'],
        },
      },
    },
  },
});
