package com.devteria.search.consumer;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.search.dto.BookIndexPayload;
import com.devteria.search.dto.KafkaBookEvent;
import com.devteria.search.service.BookIndexService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookEventConsumer {
    BookIndexService bookIndexService;
    ObjectMapper objectMapper =
            new ObjectMapper(); // com.fasterxml.jackson.databind — KHÔNG dùng tools.jackson (Jackson 3)

    @KafkaListener(topics = "book-events", groupId = "search-service")
    public void consume(KafkaBookEvent message) {
        try {
            switch (message.getEventType()) {
                case "BOOK_CREATED", "BOOK_UPDATED" -> {
                    BookIndexPayload payload = objectMapper.readValue(message.getPayload(), BookIndexPayload.class);
                    bookIndexService.upsert(payload);
                }
                case "BOOK_DELETED" -> {
                    Map<String, Object> parsed = objectMapper.readValue(message.getPayload(), Map.class);
                    bookIndexService.delete((String) parsed.get("bookId"));
                }
                case "BOOK_REVIEWED" -> {
                    Map<String, Object> parsed = objectMapper.readValue(message.getPayload(), Map.class);
                    bookIndexService.updateRating(
                            (String) parsed.get("bookId"),
                            new BigDecimal(parsed.get("ratingAverage").toString()),
                            Long.valueOf(parsed.get("ratingCount").toString()));
                }
                default -> log.debug("Ignored event type: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process book event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
