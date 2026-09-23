package com.devteria.friend.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.friend.dto.response.BlockResponse;
import com.devteria.friend.dto.response.UserProfileResponse;

@Mapper(componentModel = "spring")
public interface BlockMapper {
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "username", source = "profile.username")
    @Mapping(target = "avatar", source = "profile.avatar")
    BlockResponse toBlockResponse(String userId, Instant createdAt, UserProfileResponse profile);
}
