package com.devteria.chat.mapper;

import com.devteria.chat.dto.request.ChatMessageRequest;
import com.devteria.chat.dto.response.ChatMessageResponse;
import com.devteria.chat.entity.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class ChatMessageMapperImpl implements ChatMessageMapper {

    @Override
    public ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        if ( chatMessage == null ) {
            return null;
        }

        ChatMessageResponse.ChatMessageResponseBuilder chatMessageResponse = ChatMessageResponse.builder();

        chatMessageResponse.id( chatMessage.getId() );
        chatMessageResponse.conversationId( chatMessage.getConversationId() );
        chatMessageResponse.message( chatMessage.getMessage() );
        chatMessageResponse.sender( chatMessage.getSender() );
        chatMessageResponse.createdDate( chatMessage.getCreatedDate() );

        return chatMessageResponse.build();
    }

    @Override
    public ChatMessage toChatMessage(ChatMessageRequest request) {
        if ( request == null ) {
            return null;
        }

        ChatMessage.ChatMessageBuilder chatMessage = ChatMessage.builder();

        chatMessage.conversationId( request.getConversationId() );
        chatMessage.message( request.getMessage() );

        return chatMessage.build();
    }

    @Override
    public List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages) {
        if ( chatMessages == null ) {
            return null;
        }

        List<ChatMessageResponse> list = new ArrayList<ChatMessageResponse>( chatMessages.size() );
        for ( ChatMessage chatMessage : chatMessages ) {
            list.add( toChatMessageResponse( chatMessage ) );
        }

        return list;
    }
}
