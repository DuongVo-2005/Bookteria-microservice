package com.devteria.group.consumer;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.devteria.group.dto.KafkaBookEvent;
import com.devteria.group.dto.response.ApiResponse;
import com.devteria.group.dto.response.BookLookupResponse;
import com.devteria.group.entity.BookRef;
import com.devteria.group.entity.GroupPost;
import com.devteria.group.repository.GroupPostRepository;
import com.devteria.group.repository.httpclient.InternalBookClient;
import com.devteria.group.service.IdempotencyService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// idea-spec BA GAP-02 / OPS-02: GroupPost đính kèm bookRef chỉ là snapshot nhúng, không phải FK
// sống. BOOK_DELETED (GAP-02) -> đánh dấu bookRef.isDeleted=true để FE hiển thị "Sách không còn
// tồn tại". BOOK_MERGED (OPS-02, Admin gộp sách trùng) -> CHUYỂN bookRef sang sách gốc (re-fetch
// snapshot mới), khác hẳn BOOK_DELETED vì sách vẫn tồn tại, chỉ đổi bookId. Consumer Kafka ĐẦU
// TIÊN của group-service (trước đó service này chỉ publish, chưa từng consume gì).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookEventConsumer {
    GroupPostRepository groupPostRepository;
    IdempotencyService idempotencyService;
    InternalBookClient internalBookClient;
    ObjectMapper objectMapper = new ObjectMapper(); // com.fasterxml.jackson (Jackson 2, khớp JsonDeserializer)

    @KafkaListener(topics = "book-events", groupId = "group-service")
    public void consume(KafkaBookEvent message) {
        try {
            switch (message.getEventType()) {
                case "BOOK_DELETED" -> handleBookDeleted(message);
                case "BOOK_MERGED" -> handleBookMerged(message);
                default -> {
                    // Không quan tâm (BOOK_CREATED/BOOK_UPDATED/BOOK_REVIEWED/BOOK_IMPORTED...).
                }
            }
        } catch (Exception e) {
            log.error(
                    "Failed to process book event. aggregateId={}, eventType={}",
                    message.getAggregateId(),
                    message.getEventType(),
                    e);
        }
    }

    private void handleBookDeleted(KafkaBookEvent message) throws Exception {
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

        List<GroupPost> affected = groupPostRepository.findAllByBookRef_BookId(bookId);
        if (!affected.isEmpty()) {
            affected.forEach(post -> post.getBookRef().setDeleted(true));
            groupPostRepository.saveAll(affected);
        }

        log.info("Marked bookRef as deleted for {} group post(s). bookId={}", affected.size(), bookId);
    }

    // idea-spec BA OPS-02
    private void handleBookMerged(KafkaBookEvent message) throws Exception {
        if (!idempotencyService.markProcessedIfNew(message.getEventId())) {
            log.debug("Duplicate book-merged event ignored. eventId={}", message.getEventId());
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String fromBookId = (String) payload.get("fromBookId");
        String toBookId = (String) payload.get("toBookId");
        if (fromBookId == null || fromBookId.isBlank() || toBookId == null || toBookId.isBlank()) {
            log.warn("book-events BOOK_MERGED thiếu fromBookId/toBookId, ignored. eventId={}", message.getEventId());
            return;
        }

        List<GroupPost> affected = groupPostRepository.findAllByBookRef_BookId(fromBookId);
        if (affected.isEmpty()) {
            return;
        }

        ApiResponse<BookLookupResponse> response = internalBookClient.getBookById(toBookId);
        if (response == null || response.getResult() == null) {
            log.warn(
                    "BOOK_MERGED nhưng không lấy được sách gốc toBookId={} từ book-service, giữ nguyên bookRef cũ.",
                    toBookId);
            return;
        }
        var book = response.getResult();
        BookRef newBookRef = BookRef.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .authorName(
                        book.getAuthors() != null && !book.getAuthors().isEmpty()
                                ? book.getAuthors().getFirst().getName()
                                : null)
                .coverImage(book.getMetadata() != null ? book.getMetadata().getCoverImage() : null)
                .build();

        affected.forEach(post -> post.setBookRef(newBookRef));
        groupPostRepository.saveAll(affected);

        log.info(
                "Re-pointed bookRef for {} group post(s). fromBookId={} -> toBookId={}",
                affected.size(),
                fromBookId,
                toBookId);
    }
}
