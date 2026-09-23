package com.devteria.search.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.search.dto.GroupIndexPayload;
import com.devteria.search.dto.KafkaEventMessage;
import com.devteria.search.service.GroupIndexService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec Phase 6 - "22.1 Search Integration" ("Group nổi bật")
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GroupEventConsumer {
    GroupIndexService groupIndexService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "group-events",
            groupId = "search-service",
            containerFactory = "postGroupKafkaListenerContainerFactory")
    public void consume(KafkaEventMessage message) {
        try {
            switch (message.getEventType()) {
                case "GROUP_CREATED" -> {
                    GroupIndexPayload payload = objectMapper.readValue(message.getPayload(), GroupIndexPayload.class);
                    groupIndexService.upsert(payload);
                }
                case "GROUP_DELETED" -> {
                    java.util.Map<?, ?> parsed = objectMapper.readValue(message.getPayload(), java.util.Map.class);
                    groupIndexService.delete((String) parsed.get("groupId"));
                }
                default -> log.debug("Ignored event type: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process group event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
