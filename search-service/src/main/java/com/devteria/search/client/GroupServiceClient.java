package com.devteria.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devteria.search.configuration.AuthenticationRequestInterceptor;
import com.devteria.search.dto.GroupListItemResponse;
import com.devteria.search.dto.response.ApiResponse;
import com.devteria.search.dto.response.PageResponse;

@FeignClient(
        name = "group-service",
        url = "${app.services.group.url}",
        configuration = AuthenticationRequestInterceptor.class)
public interface GroupServiceClient {
    @GetMapping("/groups")
    ApiResponse<PageResponse<GroupListItemResponse>> listGroups(
            @RequestParam("page") int page, @RequestParam("size") int size);
}
