package com.co.eurekatic.gateway

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse

/**
 * Serves the admin-ui SPA at /admin/**.
 *
 * The SPA is built by `npm run build` (see api-gateway/Dockerfile,
 * stage 1: node-spa). Its `dist/` is copied into
 * `src/main/resources/static/admin/` by the Docker build. In local
 * dev without a built SPA, requests fall through to 404, which is
 * fine because developers run Vite on :5173 in dev mode.
 *
 * Routing rules:
 *  - `/admin/assets/{path}` -> static file under
 *    `static/admin/assets/{path}` (JS/CSS chunks, fonts, images).
 *    Mime types inferred from the filename.
 *  - everything else under `/admin/**` -> `index.html` (SPA history
 *    mode fallback). This is what lets `/admin/users` deep-link
 *    work without server-side route matching.
 *
 * Why RouterFunction, not a Spring Cloud Gateway route: SCG routes
 * forward to downstream services (`uri: ...`). They don't serve
 * classpath resources. RouterFunctions are the WebFlux-native way to
 * expose local content and they win over SCG for paths SCG doesn't
 * claim in `application.yml`.
 *
 * Security: this router is mounted publicly. Auth is enforced inside
 * the SPA (RequireAuth wrapper + 401-triggered refresh) and at the
 * downstream API endpoints (Bearer header on /sso-admin/**,
 * refresh cookie on /auth/refresh). The gateway's
 * SecurityWebFilterChain permits `/admin/**` explicitly.
 */
@Configuration
class AdminUiRouter {

    @Bean
    fun adminUiRouterFunction(): RouterFunction<ServerResponse> =
        RouterFunctions.route()
            // Concrete asset paths first — the Vite build emits
            // hashed filenames under /assets/, and we copied them
            // to /admin/assets/. These match BEFORE the SPA
            // fallback so JS/CSS chunks are served with the right
            // Content-Type instead of text/html.
            .GET("/admin/assets/{path}") { request ->
                val path = request.pathVariable("path")
                val resource: Resource = ClassPathResource("static/admin/assets/$path")
                if (resource.exists()) {
                    ServerResponse.ok()
                        .contentType(mediaTypeFor(path))
                        .bodyValue(resource)
                } else {
                    ServerResponse.notFound().build()
                }
            }
            // SPA history-mode fallback: any /admin/** path
            // (e.g. /admin, /admin/users, /admin/login,
            // /admin/activate) renders the SPA's index.html so the
            // client-side router takes over.
            .GET("/admin") { serveIndex() }
            .GET("/admin/**") { serveIndex() }
            .build()

    private fun serveIndex(): ServerResponse =
        ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValue(INDEX_HTML)

    companion object {
        private val INDEX_HTML: Resource = ClassPathResource("static/admin/index.html")

        private fun mediaTypeFor(path: String): MediaType = when {
            path.endsWith(".js") -> MediaType.valueOf("application/javascript")
            path.endsWith(".mjs") -> MediaType.valueOf("application/javascript")
            path.endsWith(".css") -> MediaType.valueOf("text/css")
            path.endsWith(".json") -> MediaType.APPLICATION_JSON
            path.endsWith(".svg") -> MediaType.valueOf("image/svg+xml")
            path.endsWith(".png") -> MediaType.IMAGE_PNG
            path.endsWith(".ico") -> MediaType.valueOf("image/x-icon")
            path.endsWith(".woff2") -> MediaType.valueOf("font/woff2")
            path.endsWith(".woff") -> MediaType.valueOf("font/woff")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
    }
}
