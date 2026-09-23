package com.devteria.chat.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.devteria.chat.dto.request.ConversationRequest;
import com.devteria.chat.dto.response.ApiResponse;
import com.devteria.chat.dto.response.ConversationResponse;
import com.devteria.chat.dto.response.PageResponse;
import com.devteria.chat.service.ConversationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("conversations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;

    @PostMapping("/create")
    com.devteria.chat.dto.response.ApiResponse<ConversationResponse> createConversation(
            @RequestBody @Valid ConversationRequest request) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.create(request))
                .build();
    }

    @GetMapping("/my-conversations")
    ApiResponse<PageResponse<ConversationResponse>> myConversations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ConversationResponse>>builder()
                .result(conversationService.myConversations(page, size))
                .build();
    }

    @PostMapping("/{conversationId}/read")
    ApiResponse<Void> markAsRead(@PathVariable String conversationId) {
        conversationService.markAsRead(conversationId);
        return ApiResponse.<Void>builder().message("Marked as read").build();
    }

    @PostMapping("/{conversationId}/hide")
    ApiResponse<Void> hide(@PathVariable String conversationId) {
        conversationService.hide(conversationId);
        return ApiResponse.<Void>builder().message("Conversation hidden").build();
    }

    @GetMapping("/requests")
    ApiResponse<List<ConversationResponse>> getPendingRequests() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(conversationService.getPendingRequests())
                .build();
    }

    @GetMapping("/requests/count")
    ApiResponse<Long> getPendingRequestCount() {
        return ApiResponse.<Long>builder()
                .result(conversationService.getPendingRequestCount())
                .build();
    }

    @PostMapping("/{conversationId}/accept")
    ApiResponse<ConversationResponse> acceptRequest(@PathVariable String conversationId) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.acceptRequest(conversationId))
                .build();
    }

    @PostMapping("/{conversationId}/reject")
    ApiResponse<ConversationResponse> rejectRequest(@PathVariable String conversationId) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.rejectRequest(conversationId))
                .build();
    }
}
