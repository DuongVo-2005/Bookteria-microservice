package com.devteria.reading.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.reading.dto.KafkaBookEvent;
import com.devteria.reading.repository.BookmarkRepository;
import com.devteria.reading.repository.ChapterRepository;
import com.devteria.reading.repository.HighlightRepository;
import com.devteria.reading.repository.ReadingProgressRepository;
import com.devteria.reading.service.IdempotencyService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-02: sách bị xoá ở book-service để lại Chapter/Highlight/Bookmark/
// ReadingProgress mồ côi ở reading-service (vẫn tồn tại nhưng không ai truy cập được nữa vì
// bookId không còn tồn tại). Consumer Kafka ĐẦU TIÊN của reading-service (trước đó service này
// chỉ publish, chưa từng consume gì) — copy đúng pattern idempotency + DLT đã proven ở
// book-service's ReadingProgressSyncConsumer.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookEventConsumer {
    ChapterRepository chapterRepository;
    HighlightRepository highlightRepository;
    BookmarkRepository bookmarkRepository;
    ReadingProgressRepository readingProgressRepository;
    IdempotencyService idempotencyService;
    ObjectMapper objectMapper = new ObjectMapper(); // com.fasterxml.jackson (Jackson 2, khớp JsonDeserializer)

    @KafkaListener(topics = "book-events", groupId = "reading-service")
    public void consume(KafkaBookEvent message) {
        try {
            if (!"BOOK_DELETED".equals(message.getEventType())) {
                return;
            }
            if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
                log.debug("Duplicate book-deleted event ignored. eventId={}", message.getEventId());
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String bookId = (String) payload.get("bookId");
            if (bookId == null || bookId.isBlank()) {
                log.warn("book-events BOOK_DELETED without bookId, ignored. eventId={}", message.getEventId());
                return;
            }

            chapterRepository.deleteAllByBookId(bookId);
            highlightRepository.deleteAllByBookId(bookId);
            bookmarkRepository.deleteAllByBookId(bookId);
            readingProgressRepository.deleteAllByBookId(bookId);

            log.info("Cleaned up reading-service content for deleted book. bookId={}", bookId);
        } catch (Exception e) {
            log.error(
                    "Failed to process book event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }
}
