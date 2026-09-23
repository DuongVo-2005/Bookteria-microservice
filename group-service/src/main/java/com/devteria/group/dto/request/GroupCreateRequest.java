package com.devteria.group.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import com.devteria.group.dto.GroupVisibility;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupCreateRequest {
    @NotBlank
    String name;

    String description;

    String category;

    GroupVisibility visibility;

    String avatar;

    String coverImage;

    List<String> rules;

    // idea-spec BA OPS-01. Boolean (wrapper) chứ không phải primitive: Spring Boot 4/Jackson 3 giờ
    // fail cứng (MismatchedInputException) khi field JSON thiếu/null map vào primitive boolean -
    // client thường không gửi field optional này, nên phải cho phép null (xem GroupService.createGroup).
    Boolean requireApproval;
}
