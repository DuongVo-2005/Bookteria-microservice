package com.devteria.friend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.friend.dto.response.FriendRequestResponse;
import com.devteria.friend.dto.response.FriendResponse;
import com.devteria.friend.dto.response.UserProfileResponse;
import com.devteria.friend.entity.FriendRequest;

@Mapper(componentModel = "spring")
public interface FriendMapper {
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "senderUsername", source = "profile.username")
    @Mapping(target = "senderAvatar", source = "profile.avatar")
    FriendRequestResponse toFriendRequestResponse(FriendRequest entity, UserProfileResponse profile);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "username", source = "profile.username")
    @Mapping(target = "avatar", source = "profile.avatar")
    FriendResponse toFriendResponse(String userId, String status, UserProfileResponse profile);
}
