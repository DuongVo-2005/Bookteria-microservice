package com.devteria.group.dto.response;

import java.time.Instant;
import java.util.List;

import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.GroupStatus;
import com.devteria.group.dto.GroupVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupResponse {
    String id;
    String name;
    String description;
    String avatar;
    String coverImage;
    GroupVisibility visibility;
    String category;
    Instant createdAt;
    String ownerId;
    long memberCount;

    // Named "joined" (not "isJoined") on purpose: Lombok's setter for a field
    // named "isJoined" would be setJoined(...) anyway (it strips the "is"
    // prefix), which confuses MapStruct's property-name matching. Keep the
    // Java-side name plain and force the wire format via @JsonProperty instead.
    @JsonProperty("isJoined")
    boolean joined;

    GroupMemberRole currentUserRole;
    List<String> rules;
    boolean requireApproval;
    GroupStatus status;
}
