package com.devteria.reading.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devteria.reading.configuration.AuthenticationRequestInterceptor;
import com.devteria.reading.dto.request.PostCreateRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.PostSummaryResponse;

@FeignClient(
        name = "post-service",
        url = "${app.service.post.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface PostClient {
    @PostMapping("/")
    ApiResponse<PostSummaryResponse> createPost(@RequestBody PostCreateRequest request);
}
