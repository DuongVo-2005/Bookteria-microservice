package com.devteria.book.dto;

// idea-spec BA v2 §4.2: Reading Wrap-up - khung thời gian rolling-window tính tới hiện tại, cùng
// convention "N gần nhất" đã dùng ở ReadingListService.getMyStats() (12 tháng gần nhất), không
// phải tuần/tháng/năm lịch cố định (đơn giản hơn, không cần xử lý múi giờ/ngày bắt đầu tuần).
public enum WrapUpPeriod {
    WEEK(7),
    MONTH(30),
    YEAR(365);

    private final int days;

    WrapUpPeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
