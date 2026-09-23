package com.devteria.group.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.GroupPostComment;

@Mapper(componentModel = "spring")
public interface GroupPostCommentMapper {
    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "authorId", source = "comment.authorId")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "authorUsername", source = "profile.username")
    @Mapping(target = "authorAvatar", source = "profile.avatar")
    GroupPostCommentResponse toGroupPostCommentResponse(GroupPostComment comment, UserProfileResponse profile);
}
