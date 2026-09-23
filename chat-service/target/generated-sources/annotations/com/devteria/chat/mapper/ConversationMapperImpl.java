package com.devteria.chat.mapper;

import com.devteria.chat.dto.response.ConversationResponse;
import com.devteria.chat.entity.Conversation;
import com.devteria.chat.entity.ParticipantInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class ConversationMapperImpl implements ConversationMapper {

    @Override
    public ConversationResponse toConversationResponse(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }

        ConversationResponse.ConversationResponseBuilder conversationResponse = ConversationResponse.builder();

        conversationResponse.id( conversation.getId() );
        conversationResponse.type( conversation.getType() );
        conversationResponse.participantsHash( conversation.getParticipantsHash() );
        List<ParticipantInfo> list = conversation.getParticipants();
        if ( list != null ) {
            conversationResponse.participants( new ArrayList<ParticipantInfo>( list ) );
        }
        conversationResponse.createdDate( conversation.getCreatedDate() );
        conversationResponse.modifiedDate( conversation.getModifiedDate() );
        conversationResponse.status( conversation.getStatus() );
        conversationResponse.requestedBy( conversation.getRequestedBy() );
        conversationResponse.lastMessage( conversation.getLastMessage() );
        conversationResponse.lastMessageAt( conversation.getLastMessageAt() );

        return conversationResponse.build();
    }

    @Override
    public List<ConversationResponse> toConversationResponseList(List<Conversation> conversations) {
        if ( conversations == null ) {
            return null;
        }

        List<ConversationResponse> list = new ArrayList<ConversationResponse>( conversations.size() );
        for ( Conversation conversation : conversations ) {
            list.add( toConversationResponse( conversation ) );
        }

        return list;
    }
}
