package com.devteria.group.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.group.dto.GroupMemberRole;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "group_members")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupMember {
    @MongoId
    String id;

    @Indexed
    String groupId;

    @Indexed
    String userId;

    GroupMemberRole role;

    Instant joinedAt;
}
