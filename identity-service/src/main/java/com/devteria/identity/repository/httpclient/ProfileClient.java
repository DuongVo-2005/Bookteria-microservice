package com.devteria.identity.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devteria.identity.configuration.AuthenticationRequestInterceptor;
import com.devteria.identity.dto.request.ProfileCreationRequest;
import com.devteria.identity.dto.response.UserProfileResponse;

@FeignClient(
        name = "profile-service",
        url = "${app.services.profile}",
        configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    @PostMapping(value = "/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
    UserProfileResponse createProfile(@RequestBody ProfileCreationRequest request);

    // idea-spec BA v2 §2.2: Soft Delete & Anonymize. POST, không phải PATCH - bug thật bắt được
    // lúc live-verify: Feign's default Client (java.net.HttpURLConnection) KHÔNG hỗ trợ PATCH,
    // ném ProtocolException("Invalid HTTP method: PATCH") ở MỌI môi trường (không phải lỗi cấu
    // hình cục bộ) - deactivateAccount() luôn fail 100% trước khi sửa. Endpoint internal-only,
    // đổi verb không ảnh hưởng contract công khai nào.
    @PostMapping("/internal/users/{userId}/anonymize")
    void anonymizeProfile(@PathVariable("userId") String userId);
}
