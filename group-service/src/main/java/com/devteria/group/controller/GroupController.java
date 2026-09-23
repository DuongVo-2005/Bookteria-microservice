package com.devteria.group.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.devteria.group.dto.GroupStatus;
import com.devteria.group.dto.request.GroupCreateRequest;
import com.devteria.group.dto.request.GroupMemberRoleUpdateRequest;
import com.devteria.group.dto.request.GroupSettingsUpdateRequest;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.GroupMemberResponse;
import com.devteria.group.dto.response.GroupResponse;
import com.devteria.group.dto.response.PageResponse;
import com.devteria.group.service.GroupService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {
    GroupService groupService;

    @PostMapping
    ApiResponse<GroupResponse> createGroup(@RequestBody @Valid GroupCreateRequest request) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.createGroup(request))
                .build();
    }

    @GetMapping
    ApiResponse<PageResponse<GroupResponse>> listGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GroupResponse> result = groupService.listGroups(search, category, page, size);
        return ApiResponse.<PageResponse<GroupResponse>>builder()
                .result(toPageResponse(result))
                .build();
    }

    // idea-spec Phase 6 - "20. Popular Groups"
    @GetMapping("/popular")
    ApiResponse<PageResponse<GroupResponse>> getPopularGroups(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GroupResponse> result = groupService.getPopularGroups(category, page, size);
        return ApiResponse.<PageResponse<GroupResponse>>builder()
                .result(toPageResponse(result))
                .build();
    }

    @GetMapping("/my")
    ApiResponse<List<GroupResponse>> getMyGroups() {
        return ApiResponse.<List<GroupResponse>>builder()
                .result(groupService.getMyGroups())
                .build();
    }

    @GetMapping("/{groupId}")
    ApiResponse<GroupResponse> getGroupDetail(@PathVariable String groupId) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.getGroupDetail(groupId))
                .build();
    }

    // idea-spec Phase 4 - "13. Group Moderation": group OWNER hoặc platform ADMIN.
    @DeleteMapping("/{groupId}")
    ApiResponse<Void> deleteGroup(@PathVariable String groupId) {
        groupService.deleteGroup(groupId);
        return ApiResponse.<Void>builder().message("Group deleted").build();
    }

    @PostMapping("/{groupId}/join")
    ApiResponse<GroupResponse> joinGroup(@PathVariable String groupId) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.joinGroup(groupId))
                .build();
    }

    @DeleteMapping("/{groupId}/leave")
    ApiResponse<Void> leaveGroup(@PathVariable String groupId) {
        groupService.leaveGroup(groupId);
        return ApiResponse.<Void>builder().message("Left group").build();
    }

    @GetMapping("/{groupId}/members")
    ApiResponse<List<GroupMemberResponse>> getMembers(@PathVariable String groupId) {
        return ApiResponse.<List<GroupMemberResponse>>builder()
                .result(groupService.getMembers(groupId))
                .build();
    }

    @PutMapping("/{groupId}/members/{userId}/role")
    ApiResponse<Void> changeMemberRole(
            @PathVariable String groupId,
            @PathVariable String userId,
            @RequestBody @Valid GroupMemberRoleUpdateRequest request) {
        groupService.changeMemberRole(groupId, userId, request);
        return ApiResponse.<Void>builder().message("Role updated").build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    ApiResponse<Void> removeMember(@PathVariable String groupId, @PathVariable String userId) {
        groupService.removeMember(groupId, userId);
        return ApiResponse.<Void>builder().message("Member removed").build();
    }

    @PostMapping("/{groupId}/members/{userId}/transfer-ownership")
    ApiResponse<Void> transferOwnership(@PathVariable String groupId, @PathVariable String userId) {
        groupService.transferOwnership(groupId, userId);
        return ApiResponse.<Void>builder().message("Ownership transferred").build();
    }

    // idea-spec BA OPS-01
    @PatchMapping("/{groupId}/settings")
    ApiResponse<Void> updateSettings(
            @PathVariable String groupId, @RequestBody @Valid GroupSettingsUpdateRequest request) {
        groupService.updateGroupSettings(groupId, request);
        return ApiResponse.<Void>builder().message("Group settings updated").build();
    }

    // idea-spec BA v2 §2.3: "Thay đổi trạng thái Nhóm: ACTIVE, FROZEN, SUSPENDED".
    @PatchMapping("/{groupId}/status")
    ApiResponse<GroupResponse> updateGroupStatus(@PathVariable String groupId, @RequestParam GroupStatus status) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.updateGroupStatus(groupId, status))
                .build();
    }

    // idea-spec BA v2 §2.3: "Giao diện Quản lý Toàn bộ Nhóm... tìm kiếm, lọc và xem chi tiết tất
    // cả các Nhóm (cả Public và Private)" — khác GET /groups (discovery công khai, loại SUSPENDED).
    @GetMapping("/admin")
    ApiResponse<PageResponse<GroupResponse>> listGroupsForAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<GroupResponse> result = groupService.listGroupsForAdmin(search, category, status, page, size);
        return ApiResponse.<PageResponse<GroupResponse>>builder()
                .result(toPageResponse(result))
                .build();
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }
}
