package com.devteria.report.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.report.dto.response.ApiResponse;

// idea-spec BA GAP-03: tái dùng thẳng /internal/users/{userId} đã có sẵn ở profile-service để
// validate targetType=USER — mọi user đã đăng ký đều có profile tạo đồng bộ lúc đăng ký (xem
// identity-service's UserService.createUser()), không cần thêm endpoint riêng ở identity-service.
@FeignClient(name = "profile-service-report", url = "${app.services.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<Object> getProfile(@PathVariable String userId);
}
