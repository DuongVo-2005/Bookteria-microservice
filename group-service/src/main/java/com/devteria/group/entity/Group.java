package com.devteria.group.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.group.dto.GroupStatus;
import com.devteria.group.dto.GroupVisibility;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Group {
    @MongoId
    String id;

    String name;

    String description;

    String avatar;

    String coverImage;

    GroupVisibility visibility;

    String category;

    @Indexed
    String ownerId;

    List<String> rules;

    // idea-spec BA GAP-03 (ẩn qua report, giờ = SUSPENDED) + BA v2 §2.3 (Admin đổi trạng thái
    // trực tiếp): SUSPENDED ẩn nhóm khỏi list/search/popular công khai mà KHÔNG xoá dữ liệu (khác
    // deleteGroup, không thể hoàn tác) - owner/member hiện tại và platform ADMIN vẫn xem được,
    // cùng cơ chế requireVisibleContentAccess đang áp dụng cho PRIVATE. FROZEN chỉ chặn đăng bài
    // mới, vẫn hiện công khai bình thường.
    @Builder.Default
    GroupStatus status = GroupStatus.ACTIVE;

    // idea-spec BA OPS-01: khi bật, bài viết mới trong nhóm này phải qua duyệt (OWNER/ADMIN)
    // trước khi công khai — xem GroupPostService.createPost()/approvePost()/rejectPost().
    @Builder.Default
    boolean requireApproval = false;

    Instant createdAt;

    Instant updatedAt;
}
