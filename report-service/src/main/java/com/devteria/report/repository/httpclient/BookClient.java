package com.devteria.report.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.devteria.report.dto.response.ApiResponse;

@FeignClient(name = "book-service-report", url = "${app.services.book.url}")
public interface BookClient {
    @GetMapping("/internal/reviews/{reviewId}/exists")
    ApiResponse<Boolean> reviewExists(@PathVariable String reviewId);
}
