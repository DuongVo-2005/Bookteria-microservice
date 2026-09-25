package com.devteria.gateway.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

// idea-spec Phase 7 - "26. Security Hardening / API protection": rate limit theo IP tại gateway
// (không theo user - nhiều endpoint public/pre-auth như login không có JWT để định danh user).
// replenishRate=20, burstCapacity=40: cho phép trung bình 20 request/giây/IP, nổ burst tới 40
// trước khi bắt đầu bị chặn (429) - số khởi điểm hợp lý cho 1 hệ thống MVP, cần chỉnh lại dựa
// trên traffic thật khi có (chưa có Redis/traffic thật để tinh chỉnh trong phiên code này).
@Configuration
public class RateLimiterConfig {

    // Sau Nginx reverse proxy, remote address luôn là IP của Nginx -> ưu tiên X-Real-IP do Nginx
    // ghi đè (client không tự đặt được vì gateway không public ra ngoài).
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return Mono.just(realIp);
            return Mono.just(exchange.getRequest().getRemoteAddress() != null
                        && exchange.getRequest().getRemoteAddress().getAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown");
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
