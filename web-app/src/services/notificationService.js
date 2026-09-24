import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

// Route thật qua gateway (đã xác nhận trực tiếp với BE, không suy đoán):
// GET  /api/v1/notification/notifications?page=&size=  -> ApiResponse<PageResponse<NotificationResponse>>
// GET  /api/v1/notification/notifications/unread-count -> ApiResponse<Long>
// POST /api/v1/notification/notifications/{id}/read    -> ApiResponse<Void>
// POST /api/v1/notification/notifications/read-all     -> ApiResponse<Void>
// PageResponse dùng field "data" (không phải "content"), "currentPage" (không phải "pageNumber").

export const getNotifications = async (page = 0, size = 20) => {
  return await httpClient.get(`${API.NOTIFICATIONS}?page=${page}&size=${size}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getUnreadNotificationCount = async () => {
  return await httpClient.get(API.NOTIFICATIONS_UNREAD_COUNT, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const markNotificationAsRead = async (notificationId) => {
  return await httpClient.post(
    `${API.NOTIFICATIONS}/${notificationId}/read`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};

export const markAllNotificationsAsRead = async () => {
  return await httpClient.post(
    API.NOTIFICATIONS_READ_ALL,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};
