import React from "react";
import { Outlet } from "react-router-dom";
import { ToastProvider } from "../../context/ToastContext";
import { ReadingProvider } from "../../context/ReadingContext";

// Layout route dùng riêng cho khu vực Book (Books/BookDetail/ReadingList/Search/Reader)
// để cung cấp ToastProvider (Snackbar thông báo dùng chung) mà không đụng tới
// AppRoutes/Scene của các trang khác. ReadingProvider (tiến độ đọc/bookmark/
// highlight — tính năng Reader) thêm ở đây vì ReadingList cũng cần đọc lịch
// sử đọc, không tách provider riêng cho /reader/:bookId.
export default function BookProviders() {
  return (
    <ToastProvider>
      <ReadingProvider>
        <Outlet />
      </ReadingProvider>
    </ToastProvider>
  );
}
