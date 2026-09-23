package com.devteria.report.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.report.dto.response.ApiResponse;

@FeignClient(name = "group-service-report", url = "${app.services.group.url}")
public interface GroupClient {
    @GetMapping("/internal/groups/{groupId}/exists")
    ApiResponse<Boolean> groupExists(@PathVariable String groupId);
}
