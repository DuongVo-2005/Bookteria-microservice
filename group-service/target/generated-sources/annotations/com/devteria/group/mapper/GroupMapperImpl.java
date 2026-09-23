package com.devteria.group.mapper;

import com.devteria.group.dto.GroupMemberRole;
import com.devteria.group.dto.response.GroupResponse;
import com.devteria.group.entity.Group;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class GroupMapperImpl implements GroupMapper {

    @Override
    public GroupResponse toGroupResponse(Group group, long memberCount, boolean joined, GroupMemberRole currentUserRole) {
        if ( group == null && currentUserRole == null ) {
            return null;
        }

        GroupResponse.GroupResponseBuilder groupResponse = GroupResponse.builder();

        if ( group != null ) {
            groupResponse.id( group.getId() );
            groupResponse.name( group.getName() );
            groupResponse.description( group.getDescription() );
            groupResponse.avatar( group.getAvatar() );
            groupResponse.coverImage( group.getCoverImage() );
            groupResponse.visibility( group.getVisibility() );
            groupResponse.category( group.getCategory() );
            groupResponse.createdAt( group.getCreatedAt() );
            groupResponse.ownerId( group.getOwnerId() );
            List<String> list = group.getRules();
            if ( list != null ) {
                groupResponse.rules( new ArrayList<String>( list ) );
            }
            groupResponse.requireApproval( group.isRequireApproval() );
            groupResponse.status( group.getStatus() );
        }
        groupResponse.memberCount( memberCount );
        groupResponse.joined( joined );
        groupResponse.currentUserRole( currentUserRole );

        return groupResponse.build();
    }
}
