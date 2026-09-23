package com.devteria.group.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.GroupPostResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.GroupPost;

@Mapper(componentModel = "spring")
public interface GroupPostMapper {
    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "groupId", source = "post.groupId")
    @Mapping(target = "authorId", source = "post.authorId")
    @Mapping(target = "content", source = "post.content")
    @Mapping(target = "bookRef", source = "post.bookRef")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "authorUsername", source = "profile.username")
    @Mapping(target = "authorAvatar", source = "profile.avatar")
    @Mapping(target = "likesCount", source = "likesCount")
    @Mapping(target = "commentsCount", source = "commentsCount")
    @Mapping(target = "liked", source = "liked")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "status", source = "post.status")
    @Mapping(target = "rejectionReason", source = "post.rejectionReason")
    GroupPostResponse toGroupPostResponse(
            GroupPost post,
            UserProfileResponse profile,
            long likesCount,
            long commentsCount,
            boolean liked,
            List<GroupPostCommentResponse> comments);
}
