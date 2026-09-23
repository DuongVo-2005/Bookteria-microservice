package com.devteria.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devteria.search.configuration.AuthenticationRequestInterceptor;
import com.devteria.search.dto.PostFeedItemResponse;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;

@FeignClient(
        name = "post-service",
        url = "${app.services.post.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface PostServiceClient {
    @GetMapping("/feed")
    ApiResponse<PageResponse<PostFeedItemResponse>> getGlobalFeed(
            @RequestParam("page") int page, @RequestParam("size") int size);
}
