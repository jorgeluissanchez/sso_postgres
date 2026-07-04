/// <reference types="vitest" />
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
    preview: {
        // `npm run preview` serves the production build (dist/) on
        // :4173. Without this proxy the SPA's /api/* calls would hit
        // :4173/api/* and 404 (Vite preview has no built-in proxy
        // outside of `server`). Adding the same forward as dev keeps
        // the prod-bundle testable locally without rebuilding the
        // gateway image each time you tweak the SPA.
        port: 4173,
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: false,
                secure: false,
            },
        },
    },
    build: {
        outDir: "dist",
        // Sourcemaps are dev artifacts. Keeping them OFF in the
        // production build saves ~1.3 MB in the api-gateway jar
        // (the SPA's `dist/` is copied into the jar's resources
        // before `mvn package`; sourcemaps would otherwise ship
        // inside the fat jar with no consumer). For debugging
        // production minified bundles, run a local dev server
        // (`npm run dev`) and use the browser devtools there.
        sourcemap: false,
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
