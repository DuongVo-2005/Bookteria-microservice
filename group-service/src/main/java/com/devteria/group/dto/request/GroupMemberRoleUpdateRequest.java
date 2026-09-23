package com.devteria.group.dto.request;

import jakarta.validation.constraints.NotNull;

import com.devteria.group.dto.GroupMemberRole;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupMemberRoleUpdateRequest {
    @NotNull
    GroupMemberRole role;
}
