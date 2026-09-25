package com.devteria.gateway.configuration;

import com.devteria.gateway.dto.response.ApiResponse;
import com.devteria.gateway.service.IdentityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {

    IdentityService identityService;

    ObjectMapper objectMapper;

    /**
     * Các endpoint không yêu cầu JWT.
     */
    @NonFinal
    String[] publicEndpoints = {
            "/identity/auth/.*",
            "/identity/users/registration",
            "/profile/users/search",
            "/notification/email/send",
            "/file/media/download/.*",

            // Google OAuth2
            "/identity/oauth2/.*",
            "/identity/login/oauth2/.*",

            // Health check
            "/identity/actuator/health"
    };

    @Value("${app.api-prefix}")
    @NonFinal
    private String apiPrefix;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        log.info("Enter authentication filter");

        // Public endpoint -> không cần JWT
        if (isPublicEndpoint(exchange.getRequest())) {
            log.info(
                    "Public endpoint: {}",
                    exchange.getRequest().getURI().getPath()
            );

            return chain.filter(exchange);
        }

        // Lấy Authorization header
        List<String> authHeader = exchange
                .getRequest()
                .getHeaders()
                .get(HttpHeaders.AUTHORIZATION);

        // Không có Authorization header
        if (CollectionUtils.isEmpty(authHeader)) {
            log.warn("Missing Authorization header");

            return unauthenticated(exchange.getResponse());
        }

        // Lấy Bearer token
        String authorization = authHeader.getFirst();

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            log.warn("Invalid Authorization header");

            return unauthenticated(exchange.getResponse());
        }

        String token = authorization.substring(7);

        // JWT rỗng
        if (token.isBlank()) {
            log.warn("Empty JWT token");

            return unauthenticated(exchange.getResponse());
        }

        // Kiểm tra JWT thông qua Identity Service
        return identityService
                .introspect(token)
                .flatMap(introspectResponse -> {

                    if (introspectResponse.getResult().isValid()) {

                        log.info("Authentication successful");

                        return chain.filter(exchange);
                    }

                    log.warn("Invalid JWT token");

                    return unauthenticated(exchange.getResponse());
                })
                .onErrorResume(throwable -> {

                    log.error(
                            "JWT introspection failed",
                            throwable
                    );

                    return unauthenticated(exchange.getResponse());
                });
    }

    /**
     * Kiểm tra request hiện tại có phải public endpoint hay không.
     */
    private boolean isPublicEndpoint(ServerHttpRequest request) {

        String path = request.getURI().getPath();

        return Arrays.stream(publicEndpoints)
                .anyMatch(endpoint ->
                        path.matches(apiPrefix + endpoint)
                );
    }

    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Trả về HTTP 401 khi user chưa xác thực.
     */
    Mono<Void> unauthenticated(ServerHttpResponse response) {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(1401)
                .message("Unauthenticated")
                .build();

        String body;

        try {
            body = objectMapper.writeValueAsString(apiResponse);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        response.getHeaders().add(
                HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE
        );

        return response.writeWith(
                Mono.just(
                        response
                                .bufferFactory()
                                .wrap(body.getBytes())
                )
        );
    }
}
