package com.devteria.reading.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.reading.dto.request.SyncBatchRequest;
import com.devteria.reading.dto.response.ApiResponse;
import com.devteria.reading.dto.response.SyncBatchResponse;
import com.devteria.reading.service.SyncBatchService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SyncController {
    SyncBatchService syncBatchService;

    // Gateway route /reading/** đã StripPrefix=2 (xem api-gateway application.yaml), nên path
    // thật ở ngoài là POST /reading/sync-batch - khớp với mọi controller khác trong service này
    // (không tự thêm "/reading" ở path nội bộ).
    @PostMapping("/sync-batch")
    ApiResponse<SyncBatchResponse> syncBatch(@RequestBody @Valid SyncBatchRequest request) {
        return ApiResponse.<SyncBatchResponse>builder()
                .result(syncBatchService.applyBatch(request))
                .build();
    }
}
