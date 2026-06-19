package com.co.eurekatic.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Serves the admin-ui SPA at /admin/**.
 *
 * The SPA is built by {@code npm run build} (see
 * {@code api-gateway/Dockerfile}, stage {@code node-spa}). Its
 * {@code dist/} is copied into {@code src/main/resources/static/admin/}
 * by the Docker build. In local dev without a built SPA, requests
 * fall through to 404, which is fine because developers run Vite on
 * :5173 in dev mode and use the gateway as a pure API proxy.
 *
 * <p>Routing rules:
 * <ul>
 *   <li>{@code /admin/assets/{path}} → static file under
 *       {@code static/admin/assets/{path}} (JS/CSS chunks, fonts,
 *       images). MIME type inferred from the filename.</li>
 *   <li>Everything else under {@code /admin/**} →
 *       {@code static/admin/index.html} (SPA history-mode fallback).
 *       This is what lets {@code /admin/users} deep-link work without
 *       server-side route matching.</li>
 * </ul>
 *
 * <p><b>Why RouterFunction, not a Spring Cloud Gateway route:</b>
 * SCG routes forward to downstream services ({@code uri: ...}). They
 * don't serve classpath resources. {@link RouterFunctions} are the
 * WebFlux-native way to expose local content and they win over SCG
 * for paths SCG doesn't claim in {@code application.yml}.
 *
 * <p><b>Security:</b> this router is mounted publicly. Auth is
 * enforced inside the SPA ({@code RequireAuth} wrapper +
 * 401-triggered refresh) and at the downstream API endpoints
 * (Bearer header on {@code /sso-admin/**}, refresh cookie on
 * {@code /auth/refresh}). The gateway's {@code SecurityWebFilterChain}
 * permits {@code /admin/**} explicitly.
 */
@Configuration
public class AdminUiRouter {

    private static final Resource INDEX_HTML =
            new ClassPathResource("static/admin/index.html");

    @Bean
    public RouterFunction<ServerResponse> adminUiRouterFunction() {
        return RouterFunctions.route()
                // Concrete asset paths first — the Vite build emits
                // hashed filenames under /assets/, and we copied them
                // to /admin/assets/. These match BEFORE the SPA
                // fallback so JS/CSS chunks are served with the right
                // Content-Type instead of text/html.
                .GET("/admin/assets/{path}", request -> {
                    String path = request.pathVariable("path");
                    Resource resource = new ClassPathResource("static/admin/assets/" + path);
                    if (resource.exists()) {
                        return ServerResponse.ok()
                                .contentType(mediaTypeFor(path))
                                .bodyValue(resource);
                    }
                    return ServerResponse.notFound().build();
                })
                // SPA history-mode fallback: any /admin/** path
                // (e.g. /admin, /admin/users, /admin/login,
                // /admin/activate) renders the SPA's index.html so
                // the client-side router takes over.
                .GET("/admin", request -> serveIndex())
                .GET("/admin/**", request -> serveIndex())
                .build();
    }

    private static Mono<ServerResponse> serveIndex() {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .bodyValue(INDEX_HTML);
    }

    private static MediaType mediaTypeFor(String path) {
        if (path.endsWith(".js") || path.endsWith(".mjs")) {
            return MediaType.valueOf("application/javascript");
        }
        if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        }
        if (path.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (path.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        if (path.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (path.endsWith(".ico")) {
            return MediaType.valueOf("image/x-icon");
        }
        if (path.endsWith(".woff2")) {
            return MediaType.valueOf("font/woff2");
        }
        if (path.endsWith(".woff")) {
            return MediaType.valueOf("font/woff");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
