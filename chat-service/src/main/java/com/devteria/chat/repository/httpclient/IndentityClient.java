package com.devteria.chat.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devteria.chat.dto.request.IntrospectRequest;
import com.devteria.chat.dto.response.ApiResponse;
import com.devteria.chat.dto.response.IntrospectResponse;

@FeignClient(name = "identity-service", url = "${app.services.identity.url}")
public interface IndentityClient {
    @PostMapping("/auth/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request);
}
