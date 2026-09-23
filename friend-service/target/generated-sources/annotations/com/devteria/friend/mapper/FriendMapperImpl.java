package com.devteria.friend.mapper;

import com.devteria.friend.dto.response.FriendRequestResponse;
import com.devteria.friend.dto.response.FriendResponse;
import com.devteria.friend.dto.response.UserProfileResponse;
import com.devteria.friend.entity.FriendRequest;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class FriendMapperImpl implements FriendMapper {

    @Override
    public FriendRequestResponse toFriendRequestResponse(FriendRequest entity, UserProfileResponse profile) {
        if ( entity == null && profile == null ) {
            return null;
        }

        FriendRequestResponse.FriendRequestResponseBuilder friendRequestResponse = FriendRequestResponse.builder();

        if ( entity != null ) {
            friendRequestResponse.id( entity.getId() );
            friendRequestResponse.senderId( entity.getSenderId() );
            friendRequestResponse.receiverId( entity.getReceiverId() );
            friendRequestResponse.status( entity.getStatus() );
            friendRequestResponse.createdAt( entity.getCreatedAt() );
        }
        if ( profile != null ) {
            friendRequestResponse.senderUsername( profile.getUsername() );
            friendRequestResponse.senderAvatar( profile.getAvatar() );
        }

        return friendRequestResponse.build();
    }

    @Override
    public FriendResponse toFriendResponse(String userId, String status, UserProfileResponse profile) {
        if ( userId == null && status == null && profile == null ) {
            return null;
        }

        FriendResponse.FriendResponseBuilder friendResponse = FriendResponse.builder();

        if ( profile != null ) {
            friendResponse.username( profile.getUsername() );
            friendResponse.avatar( profile.getAvatar() );
        }
        friendResponse.userId( userId );
        friendResponse.status( status );

        return friendResponse.build();
    }
}
