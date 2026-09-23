package com.devteria.gateway.configuration;


import com.devteria.gateway.dto.response.ApiResponse;
import com.devteria.gateway.service.IdentityService;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import reactor.netty.http.server.HttpServerResponse;
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

    // idea-spec Phase 7 - "26. Security Hardening / API protection": bỏ "/book/.*" khỏi danh
    // sách public - book-service's SecurityConfig (PUBLIC_ENDPOINTS rỗng) đòi JWT ở MỌI endpoint
    // của chính nó rồi, nên wildcard này ở gateway không "mở" thêm gì cho user hợp lệ, mà chỉ
    // BỎ QUA bước introspect() (kiểm tra token có bị revoke chưa - InvalidatedToken/Redis) cho
    // toàn bộ book-service, kể cả các endpoint ADMIN mới thêm ở Phase 4-6. Hệ quả thật: user vừa
    // bị Admin khoá tài khoản (§11 Session Revocation) vẫn gọi được book-service bình thường cho
    // tới khi JWT tự hết hạn, vì revocation chỉ được check ở bước introspect() bị bypass bởi rule
    // này - làm vô hiệu hoá đúng effect mà lockUser() tuyên bố "chặn ngay token hiện có". Xoá
    // wildcard này khôi phục lại đúng hành vi introspect cho book-service, không phá vỡ luồng nào
    // (client hợp lệ vẫn luôn phải gửi JWT để qua được security của chính book-service).
    @NonFinal
    String[] publicEndpoints= {"/identity/auth/.*"
            ,"/identity/users/registration",
            "/profile/users/search",
            "/notification/email/send",
            "/file/media/download/.*",

            // Google OAuth2
            "/identity/oauth2/.*",
            "/identity/login/oauth2/.*",};

    @Value("${app.api-prefix}")
    @NonFinal
    private String apiPrefix;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Enter authentication filter");

        if(isPublicEndpoints(exchange.getRequest())){
            return chain.filter(exchange);
        }

        //Get token from authorization header
        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if(CollectionUtils.isEmpty(authHeader)){
            return  unauthenticated(exchange.getResponse());
        }
        String token = authHeader.getFirst().replace("Bearer ","");

        //Verify token
        //Delegate identity service
         return identityService.introspect(token).flatMap(introspectResponseApiResponse -> {
             if(introspectResponseApiResponse.getResult().isValid()){
                 return chain.filter(exchange);
             }
             else{
                 return unauthenticated(exchange.getResponse());
             }
         }).onErrorResume(throwable -> unauthenticated(exchange.getResponse()));

    }
    private boolean isPublicEndpoints(ServerHttpRequest request){
        return Arrays.stream(publicEndpoints).anyMatch(s->request.getURI().getPath().matches(apiPrefix +s));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    Mono<Void> unauthenticated(ServerHttpResponse response) {
        ApiResponse<?> apiResponse =ApiResponse.builder()
                .code(1401)
                .message("Unauthenticated")
                .build();
        String body = null;
        try {
            body=objectMapper.writeValueAsString(apiResponse);
        }catch (JacksonException e){
            throw  new RuntimeException(e);
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
}
