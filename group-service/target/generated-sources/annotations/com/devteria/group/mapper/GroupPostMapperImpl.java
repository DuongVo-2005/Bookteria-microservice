package com.devteria.group.mapper;

import com.devteria.group.dto.response.BookRefResponse;
import com.devteria.group.dto.response.GroupPostCommentResponse;
import com.devteria.group.dto.response.GroupPostResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.BookRef;
import com.devteria.group.entity.GroupPost;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class GroupPostMapperImpl implements GroupPostMapper {

    @Override
    public GroupPostResponse toGroupPostResponse(GroupPost post, UserProfileResponse profile, long likesCount, long commentsCount, boolean liked, List<GroupPostCommentResponse> comments) {
        if ( post == null && profile == null && comments == null ) {
            return null;
        }

        GroupPostResponse.GroupPostResponseBuilder groupPostResponse = GroupPostResponse.builder();

        if ( post != null ) {
            groupPostResponse.id( post.getId() );
            groupPostResponse.groupId( post.getGroupId() );
            groupPostResponse.authorId( post.getAuthorId() );
            groupPostResponse.content( post.getContent() );
            groupPostResponse.bookRef( bookRefToBookRefResponse( post.getBookRef() ) );
            groupPostResponse.createdAt( post.getCreatedAt() );
            groupPostResponse.status( post.getStatus() );
            groupPostResponse.rejectionReason( post.getRejectionReason() );
        }
        if ( profile != null ) {
            groupPostResponse.authorUsername( profile.getUsername() );
            groupPostResponse.authorAvatar( profile.getAvatar() );
        }
        groupPostResponse.likesCount( likesCount );
        groupPostResponse.commentsCount( commentsCount );
        groupPostResponse.liked( liked );
        List<GroupPostCommentResponse> list = comments;
        if ( list != null ) {
            groupPostResponse.comments( new ArrayList<GroupPostCommentResponse>( list ) );
        }

        return groupPostResponse.build();
    }

    protected BookRefResponse bookRefToBookRefResponse(BookRef bookRef) {
        if ( bookRef == null ) {
            return null;
        }

        BookRefResponse.BookRefResponseBuilder bookRefResponse = BookRefResponse.builder();

        bookRefResponse.bookId( bookRef.getBookId() );
        bookRefResponse.bookTitle( bookRef.getBookTitle() );
        bookRefResponse.authorName( bookRef.getAuthorName() );
        bookRefResponse.coverImage( bookRef.getCoverImage() );
        bookRefResponse.deleted( bookRef.isDeleted() );

        return bookRefResponse.build();
    }
}
