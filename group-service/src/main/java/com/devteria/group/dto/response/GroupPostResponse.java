package com.devteria.group.dto.response;

import java.time.Instant;
import java.util.List;

import com.devteria.group.dto.GroupPostStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupPostResponse {
    String id;
    String groupId;
    String authorId;
    String authorUsername;
    String authorAvatar;
    String content;
    BookRefResponse bookRef;
    Instant createdAt;
    long likesCount;
    long commentsCount;
    GroupPostStatus status;
    String rejectionReason;

    // Named "liked" (not "isLiked") — same Lombok/MapStruct naming gotcha as
    // GroupResponse.joined, see comment there.
    @JsonProperty("isLiked")
    boolean liked;

    List<GroupPostCommentResponse> comments;
}
