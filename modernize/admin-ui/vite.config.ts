import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

/**
 * Vite config for the admin SPA. In dev, the Vite server runs on
 * :5173 and proxies any /api/* request to the api-gateway on :8080.
 * This is what makes the cookie-based auth flow work without
 * involving CORS: the browser sees a single origin (the Vite dev
 * server), and the proxy makes the request to the gateway on the
 * server side, carrying cookies through HttpClient's normal
 * forwarding.
 *
 * In production, VITE_API_BASE is /api and the gateway serves the
 * SPA at /admin/** AND proxies /api/** to the backend. Single origin
 * end-to-end, no CORS at all.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Anything under /api (auth, sso-admin, anything else) is
      // forwarded to the gateway. The Vite dev server does NOT
      // change the request — Origin/Host/cookies are preserved.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: false,
        secure: false,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
    target: "es2022",
  },
  test: {
    environment: "happy-dom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    // e2e/ is Playwright's territory; vitest only owns src/.
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    exclude: ["node_modules", "dist", "e2e"],
  },
});
