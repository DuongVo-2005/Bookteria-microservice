import React, { createContext, useContext, useState, useEffect } from "react";
import { useFriend } from "./FriendContext";
import { getCurrentUserId } from "../services/authenticationService";
import { getNotifications, getUnreadNotificationCount, markNotificationAsRead, markAllNotificationsAsRead } from "../services/notificationService";
import { getGroupDetail } from "../services/groupService";

// notification-service's body cho GROUP_POST_CREATED/GROUP_COMMENT_CREATED
// nhét thẳng groupId thô (Mongo ObjectId 24 ký tự hex) vào câu, kiểu "...
// trong nhóm 6aac...ee5f" — bug BE đã tự nhận (be-report.md Phase 1 Known
// Issues #3: payload 2 event này không có groupName, khác GROUP_MEMBER_JOINED
// có sẵn). Không sửa được ở BE (ngoài phạm vi FE Agent) — workaround: tự phát
// hiện ID thô trong text đã lưu, resolve tên nhóm thật qua group-service rồi
// thay thế lại trước khi hiển thị.
const GROUP_ID_IN_TEXT_RE = /nhóm ([0-9a-fA-F]{24})\b/g;

const resolveGroupIdsInNotifications = (items) => {
  const groupIds = new Set();
  items.forEach((item) => {
    for (const match of item.content?.matchAll(GROUP_ID_IN_TEXT_RE) || []) {
      groupIds.add(match[1]);
    }
  });
  if (groupIds.size === 0) return Promise.resolve(items);

  return Promise.all(
    [...groupIds].map((id) =>
      getGroupDetail(id)
        .then((res) => [id, res?.data?.result?.name])
        .catch(() => [id, null])
    )
  ).then((entries) => {
    const nameById = Object.fromEntries(entries.filter(([, name]) => Boolean(name)));
    if (Object.keys(nameById).length === 0) return items;
    return items.map((item) => ({
      ...item,
      content: item.content?.replace(GROUP_ID_IN_TEXT_RE, (full, id) => (nameById[id] ? `nhóm "${nameById[id]}"` : full)),
    }));
  });
};

// Notification Center — 2 nguồn dữ liệu THẬT, không mock:
//
// 1) "persisted": notification-service giờ đã lưu trữ thật (MongoDB) + có API
//    thật (đã xác nhận trực tiếp với BE) — GET /notification/notifications,
//    GET /notification/notifications/unread-count,
//    POST /notification/notifications/{id}/read,
//    POST /notification/notifications/read-all.
//    Nguồn này chỉ có title/body chung chung (do sinh từ template email), KHÔNG
//    có type/targetUrl riêng cho từng loại sự kiện — dùng để có LỊCH SỬ thật,
//    sống sót qua reload trang (khắc phục giới hạn cũ đã ghi trong báo cáo
//    trước: "mất khi tải lại trang").
//
// 2) "realtime": sự kiện Socket.IO thật (friend-event, message-request-event,
//    group-event) nhận được TRONG phiên hiện tại — vẫn giữ nguyên vì có
//    type/targetUrl rõ ràng cho UX điều hướng/click ngay lập tức, việc mà
//    nguồn "persisted" (title/body chung chung) không làm được.
//
// Không gộp/khớp 2 nguồn theo id (không có gì đảm bảo 1-1 giữa sự kiện
// Socket.IO và bản ghi Kafka "notification-delivery" đã lưu) — hiển thị nối
// tiếp nhau: mục "persisted" nạp 1 lần lúc mở app, mục "realtime" luôn được
// thêm mới lên đầu khi có sự kiện tới.
const NotificationContext = createContext(undefined);

let notifIdCounter = 0;
const nextId = () => `notif-${Date.now()}-${notifIdCounter++}`;

export const NotificationProvider = ({ children }) => {
  const { socket } = useFriend();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const pushNotification = (notif) => {
    setNotifications((prev) => [{ id: nextId(), source: "realtime", isRead: false, createdAt: new Date().toISOString(), ...notif }, ...prev]);
    setUnreadCount((prev) => prev + 1);
  };

  const markAsRead = (notificationId) => {
    setNotifications((prev) => {
      const target = prev.find((n) => n.id === notificationId);
      if (!target || target.isRead) return prev;

      if (target.source === "persisted") {
        markNotificationAsRead(notificationId).catch(() => {});
      }

      setUnreadCount((count) => Math.max(0, count - 1));
      return prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n));
    });
  };

  const markAllAsRead = () => {
    markAllNotificationsAsRead().catch(() => {});
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    setUnreadCount(0);
  };

  // Nạp lịch sử thật + số chưa đọc thật ngay khi có phiên đăng nhập (socket
  // chỉ được tạo sau khi isAuthenticated() === true, xem FriendContext) — chỉ
  // chạy 1 lần cho mỗi phiên (khi socket chuyển từ null sang có giá trị).
  useEffect(() => {
    if (!socket) return;

    getNotifications(0, 20)
      .then((response) => {
        const page = response?.data?.result;
        const items = (page?.data || []).map((n) => ({
          id: n.id,
          source: "persisted",
          type: null,
          title: n.title,
          content: n.body,
          targetUrl: null,
          isRead: n.isRead,
          createdAt: n.createdAt,
        }));
        setNotifications((prev) => [...prev, ...items]);
        return resolveGroupIdsInNotifications(items);
      })
      .then((resolvedItems) => {
        setNotifications((prev) =>
          prev.map((n) => resolvedItems.find((r) => r.id === n.id) || n)
        );
      })
      .catch(() => {});

    getUnreadNotificationCount()
      .then((response) => setUnreadCount(response?.data?.result || 0))
      .catch(() => {});
  }, [socket]);

  useEffect(() => {
    if (!socket) return;
    const myId = getCurrentUserId();

    // --- friend-event: chỉ log lại thành thông báo, KHÔNG lặp lại logic
    // refetch (FriendContext đã tự xử lý phần đó qua handler riêng của nó —
    // Socket.IO cho phép nhiều listener cùng tên event trên cùng 1 socket).
    const handleFriendEvent = (event) => {
      switch (event?.eventType) {
        case "FRIEND_REQUEST_SENT":
          pushNotification({ type: "FRIEND_REQUEST_SENT", title: "Lời mời kết bạn mới", content: "Bạn có một lời mời kết bạn mới.", targetUrl: "/friends" });
          break;
        case "FRIEND_REQUEST_ACCEPTED":
          pushNotification({ type: "FRIEND_REQUEST_ACCEPTED", title: "Đã chấp nhận kết bạn", content: "Lời mời kết bạn của bạn đã được chấp nhận.", targetUrl: "/friends" });
          break;
        case "FRIEND_REQUEST_REJECTED":
          pushNotification({ type: "FRIEND_REQUEST_REJECTED", title: "Lời mời kết bạn bị từ chối", content: "Một lời mời kết bạn bạn gửi đã bị từ chối.", targetUrl: "/friends" });
          break;
        case "FRIEND_REMOVED":
          pushNotification({ type: "FRIEND_REMOVED", title: "Đã huỷ kết bạn", content: "Một người bạn vừa huỷ kết bạn với bạn.", targetUrl: "/friends" });
          break;
        case "FRIEND_BLOCKED":
          // Chỉ log cho người CHỦ ĐỘNG chặn — giữ đúng tinh thần "cân nhắc
          // riêng tư" đã ghi trong friend-system-fe-contract.md mục 8: không
          // tạo thêm tín hiệu cho người bị chặn ngoài việc dữ liệu tự ẩn đi.
          if (event.blockerId === myId) {
            pushNotification({ type: "FRIEND_BLOCKED", title: "Đã chặn người dùng", content: "Bạn đã chặn 1 người dùng.", targetUrl: "/friends" });
          }
          break;
        default:
          break;
      }
    };

    // --- message-request-event: payload chỉ có {eventType, conversationId}
    // (đã xác nhận với BE) — không có sender/receiver, nhưng BE tự target
    // đúng người nhận theo từng loại event nên không cần tự phân biệt vai
    // trò ở đây (nhận được event nào thì chắc chắn đúng vai trò event đó).
    const handleMessageRequestEvent = (event) => {
      switch (event?.eventType) {
        case "MESSAGE_REQUEST_RECEIVED":
          pushNotification({ type: "MESSAGE_REQUEST_RECEIVED", title: "Yêu cầu nhắn tin mới", content: "Bạn có 1 yêu cầu nhắn tin đang chờ ở mục Tin nhắn chờ.", targetUrl: "/chat" });
          break;
        case "MESSAGE_REQUEST_ACCEPTED":
          pushNotification({ type: "MESSAGE_REQUEST_ACCEPTED", title: "Yêu cầu nhắn tin được chấp nhận", content: "Bạn có thể tiếp tục trò chuyện ngay bây giờ.", targetUrl: "/chat" });
          break;
        case "MESSAGE_REQUEST_REJECTED":
          pushNotification({ type: "MESSAGE_REQUEST_REJECTED", title: "Yêu cầu nhắn tin bị từ chối", content: "Yêu cầu nhắn tin của bạn đã bị từ chối.", targetUrl: "/chat" });
          break;
        default:
          break;
      }
    };

    // --- group-event: payload {eventType, groupId, groupName?, ...} (đã
    // xác nhận có thật ở BE qua GroupEventConsumer). Tính năng Groups ở FE
    // hiện vẫn là dữ liệu mock cục bộ (chưa nối group-service thật) — nên
    // targetUrl chỉ dẫn về /groups, KHÔNG đảm bảo đúng group thật hiển thị
    // đúng nội dung sự kiện cho tới khi Groups được nối API thật.
    const handleGroupEvent = (event) => {
      if (!event?.eventType) return;
      const groupLabel = event.groupName ? `nhóm "${event.groupName}"` : "một nhóm bạn tham gia";
      const labels = {
        GROUP_POST: { title: "Bài viết mới trong nhóm", content: `Có bài viết mới trong ${groupLabel}.` },
        GROUP_INVITE: { title: "Lời mời vào nhóm", content: `Bạn được mời tham gia ${groupLabel}.` },
      };
      const info = labels[event.eventType] || { title: "Cập nhật nhóm", content: `Có cập nhật mới trong ${groupLabel}.` };
      pushNotification({
        type: event.eventType,
        title: info.title,
        content: info.content,
        targetUrl: event.groupId ? `/groups/${event.groupId}` : "/groups",
      });
    };

    socket.on("friend-event", handleFriendEvent);
    socket.on("message-request-event", handleMessageRequestEvent);
    socket.on("group-event", handleGroupEvent);

    return () => {
      socket.off("friend-event", handleFriendEvent);
      socket.off("message-request-event", handleMessageRequestEvent);
      socket.off("group-event", handleGroupEvent);
    };
  }, [socket]);

  return (
    <NotificationContext.Provider value={{ notifications, unreadCount, markAsRead, markAllAsRead }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error("useNotification must be used within a NotificationProvider");
  }
  return context;
};
