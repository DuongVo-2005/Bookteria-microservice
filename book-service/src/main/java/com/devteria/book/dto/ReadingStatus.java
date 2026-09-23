package com.devteria.book.dto;

public enum ReadingStatus {
    WANT_TO_READ,
    READING,
    COMPLETED,

    // idea-spec BA GAP-02: sách bị Admin xoá khỏi catalog trong khi vẫn còn trong shelf của user
    // — không xoá luôn ReadingList (mất lịch sử "đã từng đọc gì"), chuyển sang trạng thái này để
    // FE tự hiển thị rõ "sách không còn tồn tại trên hệ thống" thay vì crash/404 khi mở lại.
    UNAVAILABLE
}
