package com.devteria.search.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.search.dto.KafkaEventMessage;
import com.devteria.search.dto.PostIndexPayload;
import com.devteria.search.service.PostIndexService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec Phase 6 - "22.1 Search Integration"
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PostEventConsumer {
    PostIndexService postIndexService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "post-events",
            groupId = "search-service",
            containerFactory = "postGroupKafkaListenerContainerFactory")
    public void consume(KafkaEventMessage message) {
        try {
            switch (message.getEventType()) {
                case "POST_CREATED" -> {
                    PostIndexPayload payload = objectMapper.readValue(message.getPayload(), PostIndexPayload.class);
                    postIndexService.upsert(payload);
                }
                case "POST_DELETED" -> {
                    java.util.Map<?, ?> parsed = objectMapper.readValue(message.getPayload(), java.util.Map.class);
                    postIndexService.delete((String) parsed.get("postId"));
                }
                default -> log.debug("Ignored event type: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process post event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
