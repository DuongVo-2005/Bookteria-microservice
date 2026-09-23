package com.devteria.friend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.friend.dto.request.FriendRequestCreateRequest;
import com.devteria.friend.dto.response.ApiResponse;
import com.devteria.friend.dto.response.BlockResponse;
import com.devteria.friend.dto.response.FriendRequestResponse;
import com.devteria.friend.dto.response.FriendResponse;
import com.devteria.friend.service.FriendService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendController {

    FriendService friendService;

    // Gửi kb
    @PostMapping("/requests")
    ApiResponse<FriendRequestResponse> sendRequest(@RequestBody @Valid FriendRequestCreateRequest request) {
        return ApiResponse.<FriendRequestResponse>builder()
                .result(friendService.sendRequest(request))
                .build();
    }

    // Chấp nhận kb
    @PutMapping("/requests/{requestId}/accept")
    ApiResponse<FriendRequestResponse> acceptRequest(@PathVariable String requestId) {

        return ApiResponse.<FriendRequestResponse>builder()
                .result(friendService.acceptRequest(requestId))
                .build();
    }

    // Từ chối kb
    @PutMapping("/requests/{requestId}/reject")
    ApiResponse<FriendRequestResponse> rejectRequest(@PathVariable String requestId) {

        return ApiResponse.<FriendRequestResponse>builder()
                .result(friendService.rejectRequest(requestId))
                .build();
    }

    // Hủy kb
    @DeleteMapping("/requests/{requestId}")
    ApiResponse<Void> cancelRequest(@PathVariable String requestId) {

        friendService.cancelRequest(requestId);

        return ApiResponse.<Void>builder().message("Friend request cancelled").build();
    }

    // Xóa bạn
    @DeleteMapping("/{friendUserId}")
    ApiResponse<Void> removeFriend(@PathVariable String friendUserId) {

        friendService.removeFriend(friendUserId);

        return ApiResponse.<Void>builder()
                .message("Friend removed successfully")
                .build();
    }

    // Lấy ds bạn bè
    @GetMapping
    ApiResponse<List<FriendResponse>> getFriends() {

        return ApiResponse.<List<FriendResponse>>builder()
                .result(friendService.getFriend())
                .build();
    }

    // Internal, không auth (permitAll trong SecurityConfig) - post-service gọi để lọc
    // "feed theo bạn bè" (idea-spec Phase 3 mục 5), chỉ cần list userId, không enrich profile.
    @GetMapping("/internal/{userId}")
    ApiResponse<List<String>> getFriendUserIdsInternal(@PathVariable String userId) {
        return ApiResponse.<List<String>>builder()
                .result(friendService.getFriendUserIds(userId))
                .build();
    }

    // Lấy ds lời mời đã nhận
    @GetMapping("/requests/received")
    ApiResponse<List<FriendRequestResponse>> getReceivedRequests() {

        return ApiResponse.<List<FriendRequestResponse>>builder()
                .result(friendService.getReceiveRequest())
                .build();
    }

    // Lấy ds lời mời đã gửi

    @GetMapping("/requests/sent")
    ApiResponse<List<FriendRequestResponse>> getSentRequests() {

        return ApiResponse.<List<FriendRequestResponse>>builder()
                .result(friendService.getSentRequests())
                .build();
    }

    // Kiểm tra trạng thái quan hệ với một user.
    @GetMapping("/status/{otherUserId}")
    ApiResponse<FriendResponse> getFriendshipStatus(@PathVariable String otherUserId) {

        return ApiResponse.<FriendResponse>builder()
                .result(friendService.getFriendshipStatus(otherUserId))
                .build();
    }

    @PostMapping("/block/{userId}")
    ApiResponse<Void> blockUser(@PathVariable String userId) {
        friendService.blockUser(userId);
        return ApiResponse.<Void>builder().message("User blocked").build();
    }

    @DeleteMapping("/block/{userId}")
    ApiResponse<Void> unblockUser(@PathVariable String userId) {
        friendService.unblockUser(userId);
        return ApiResponse.<Void>builder().message("User unblocked").build();
    }

    @GetMapping("/blocks")
    ApiResponse<List<BlockResponse>> getBlockedUsers() {
        return ApiResponse.<List<BlockResponse>>builder()
                .result(friendService.getBlockedUsers())
                .build();
    }
}
