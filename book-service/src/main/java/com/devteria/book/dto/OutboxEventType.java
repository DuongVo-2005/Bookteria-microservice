package com.devteria.book.dto;

public enum OutboxEventType {
    BOOK_CREATED,
    BOOK_UPDATED,
    BOOK_REVIEWED,
    READING_PROGRESS_UPDATED,
    BOOK_DELETED,
    BOOK_IMPORTED,
    // idea-spec BA OPS-02: {fromBookId, toBookId} — group-service re-point GroupPost.bookRef
    // đang trỏ tới fromBookId sang toBookId (khác BOOK_DELETED chỉ đánh dấu deleted=true).
    BOOK_MERGED
}
