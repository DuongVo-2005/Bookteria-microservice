package com.devteria.group.mapper;

import com.devteria.group.dto.response.GroupMemberResponse;
import com.devteria.group.dto.response.UserProfileResponse;
import com.devteria.group.entity.GroupMember;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class GroupMemberMapperImpl implements GroupMemberMapper {

    @Override
    public GroupMemberResponse toGroupMemberResponse(GroupMember member, UserProfileResponse profile) {
        if ( member == null && profile == null ) {
            return null;
        }

        GroupMemberResponse.GroupMemberResponseBuilder groupMemberResponse = GroupMemberResponse.builder();

        if ( member != null ) {
            groupMemberResponse.id( member.getId() );
            groupMemberResponse.groupId( member.getGroupId() );
            groupMemberResponse.userId( member.getUserId() );
            groupMemberResponse.role( member.getRole() );
            groupMemberResponse.joinedAt( member.getJoinedAt() );
        }
        if ( profile != null ) {
            groupMemberResponse.username( profile.getUsername() );
            groupMemberResponse.avatar( profile.getAvatar() );
        }

        return groupMemberResponse.build();
    }
}
