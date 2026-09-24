import React from "react";
import { ToastProvider } from "../../context/ToastContext";
import { FriendProvider } from "../../context/FriendContext";
import { NotificationProvider } from "../../context/NotificationContext";

// Bọc toàn bộ <Routes> (bên trong <Router>) vì SideMenu/Header hiển thị badge
// số lời mời kết bạn + số thông báo trên MỌI trang, không riêng vài trang lẻ.
// NotificationProvider đặt BÊN TRONG FriendProvider vì Notification giờ nghe
// trực tiếp trên cùng 1 kết nối Socket.IO mà FriendContext đang sở hữu
// (useFriend().socket) — không mở thêm kết nối riêng.
export default function FriendProviders({ children }) {
  return (
    <ToastProvider>
      <FriendProvider>
        <NotificationProvider>{children}</NotificationProvider>
      </FriendProvider>
    </ToastProvider>
  );
}
