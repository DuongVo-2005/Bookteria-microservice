package com.devteria.group.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.repository.GroupRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

// idea-spec BA GAP-03: report-service "validate nhẹ" targetId trước khi lưu Report PENDING.
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalGroupController {
    GroupRepository groupRepository;

    @GetMapping("/internal/groups/{groupId}/exists")
    ApiResponse<Boolean> groupExists(@PathVariable String groupId) {
        return ApiResponse.<Boolean>builder()
                .result(groupRepository.existsById(groupId))
                .build();
    }
}
