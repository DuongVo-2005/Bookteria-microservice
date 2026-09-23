package com.devteria.friend.mapper;

import com.devteria.friend.dto.response.BlockResponse;
import com.devteria.friend.dto.response.UserProfileResponse;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class BlockMapperImpl implements BlockMapper {

    @Override
    public BlockResponse toBlockResponse(String userId, Instant createdAt, UserProfileResponse profile) {
        if ( userId == null && createdAt == null && profile == null ) {
            return null;
        }

        BlockResponse.BlockResponseBuilder blockResponse = BlockResponse.builder();

        if ( profile != null ) {
            blockResponse.username( profile.getUsername() );
            blockResponse.avatar( profile.getAvatar() );
        }
        blockResponse.userId( userId );
        blockResponse.createdAt( createdAt );

        return blockResponse.build();
    }
}
