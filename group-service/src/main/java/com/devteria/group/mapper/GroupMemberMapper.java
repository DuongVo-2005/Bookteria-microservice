package com.devteria.group.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.group.dto.response.GroupMemberResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.GroupMember;

@Mapper(componentModel = "spring")
public interface GroupMemberMapper {
    // `GroupMember.userId` and `UserProfileResponse.userId` share the same
    // field name — MapStruct's multi-param mapping has silently picked the
    // wrong source for a collision like this before in this repo (FriendMapper
    // bug), so every ambiguous target is pinned explicitly to its source param.
    @Mapping(target = "id", source = "member.id")
    @Mapping(target = "groupId", source = "member.groupId")
    @Mapping(target = "userId", source = "member.userId")
    @Mapping(target = "role", source = "member.role")
    @Mapping(target = "joinedAt", source = "member.joinedAt")
    @Mapping(target = "username", source = "profile.username")
    @Mapping(target = "avatar", source = "profile.avatar")
    GroupMemberResponse toGroupMemberResponse(GroupMember member, UserProfileResponse profile);
}
