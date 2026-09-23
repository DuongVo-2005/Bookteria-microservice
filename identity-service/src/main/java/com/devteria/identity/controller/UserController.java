package com.devteria.identity.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.identity.dto.request.ApiResponse;
import com.devteria.identity.dto.request.LockUserRequest;
import com.devteria.identity.dto.request.UserCreationRequest;
import com.devteria.identity.dto.request.UserPermissionUpdateRequest;
import com.devteria.identity.dto.request.UserUpdateRequest;
import com.devteria.identity.dto.response.UserResponse;
import com.devteria.identity.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @PostMapping("/registration")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    // idea-spec Phase 4 - "11.1 Session Revocation" / BA OPS-03 (thời hạn khoá)
    @PostMapping("/{userId}/lock")
    ApiResponse<UserResponse> lockUser(@PathVariable String userId, @RequestBody @Valid LockUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.lockUser(userId, request))
                .build();
    }

    @PostMapping("/{userId}/unlock")
    ApiResponse<UserResponse> unlockUser(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.unlockUser(userId))
                .build();
    }

    // idea-spec BA v2 P1-01: tuỳ chỉnh danh sách Permission trực tiếp cho 1 tài khoản.
    @PatchMapping("/{userId}/permissions")
    ApiResponse<UserResponse> updatePermissions(
            @PathVariable String userId, @RequestBody UserPermissionUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updatePermissions(userId, request))
                .build();
    }

    // idea-spec BA v2 §2.2: Admin Password Reset - không trả mật khẩu thô, chỉ gửi qua email.
    @PostMapping("/{userId}/reset-password")
    ApiResponse<Void> resetPassword(@PathVariable String userId) {
        userService.resetPassword(userId);
        return ApiResponse.<Void>builder()
                .message("Temporary password has been emailed to the user")
                .build();
    }

    // idea-spec BA v2 §2.2: Soft Delete & Anonymize.
    @PostMapping("/{userId}/deactivate")
    ApiResponse<UserResponse> deactivateAccount(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.deactivateAccount(userId))
                .build();
    }
}
