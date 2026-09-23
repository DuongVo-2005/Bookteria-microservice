package com.devteria.group.mapper;

import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.GroupPostComment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class GroupPostCommentMapperImpl implements GroupPostCommentMapper {

    @Override
    public GroupPostCommentResponse toGroupPostCommentResponse(GroupPostComment comment, UserProfileResponse profile) {
        if ( comment == null && profile == null ) {
            return null;
        }

        GroupPostCommentResponse.GroupPostCommentResponseBuilder groupPostCommentResponse = GroupPostCommentResponse.builder();

        if ( comment != null ) {
            groupPostCommentResponse.id( comment.getId() );
            groupPostCommentResponse.authorId( comment.getAuthorId() );
            groupPostCommentResponse.content( comment.getContent() );
            groupPostCommentResponse.createdAt( comment.getCreatedAt() );
        }
        if ( profile != null ) {
            groupPostCommentResponse.authorUsername( profile.getUsername() );
            groupPostCommentResponse.authorAvatar( profile.getAvatar() );
        }

        return groupPostCommentResponse.build();
    }
}
