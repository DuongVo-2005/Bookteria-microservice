package com.devteria.identity.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// idea-spec BA OPS-03: "3_DAYS"/"7_DAYS"/"30_DAYS" không phải tên hằng số Java hợp lệ (không được
// bắt đầu bằng chữ số) - đặt tên đọc được, FE gửi/nhận đúng chuỗi này qua JSON (Jackson serialize
// enum theo tên hằng số).
public enum LockDuration {
    THREE_DAYS(3),
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    PERMANENT(null);

    private final Integer days;

    LockDuration(Integer days) {
        this.days = days;
    }

    public Instant computeLockedUntil(Instant from) {
        return days == null ? null : from.plus(days, ChronoUnit.DAYS);
    }
}
