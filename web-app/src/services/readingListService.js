import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const addToShelf = async (bookId, status) => {
  return await httpClient.post(
    API.READING_LIST,
    {
      bookId: bookId,
      status: status,
    },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};

export const getMyReadingList = async (page, size, status) => {
  return await httpClient.get(API.READING_LIST, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    params: {
      page: page,
      size: size,
      status: status,
    },
  });
};

export const updateReadingProgress = async (id, data) => {
  return await httpClient.patch(`${API.READING_LIST_DETAIL}/${id}`, data, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
      "Content-Type": "application/json",
    },
  });
};

export const removeFromShelf = async (id) => {
  return await httpClient.delete(`${API.READING_LIST_DETAIL}/${id}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

// Phase 5 (be-report.md, 2026-09-18): completedByMonth luôn đúng 12 phần tử
// (tháng hiện tại + 11 tháng trước, kể cả count=0) — FE không cần tự lấp.
export const getMyReadingStats = async () => {
  return await httpClient.get(`${API.READING_LIST}/stats`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

// BA v2 Phase 3 §4.2 (be-report.md, bổ sung 2026-09-22) — Reading Wrap-up:
// đã code từ trước, chỉ vừa được BE viết báo cáo. Response shape KHÔNG được
// mô tả chi tiết field-by-field trong report (chỉ mô tả bằng lời: "số sách
// hoàn thành + top thể loại/tác giả") — UI đọc field phòng thủ (nhiều tên
// khả dĩ), không tự đoán 1 shape cụ thể duy nhất.
export const getMyReadingWrapUp = async () => {
  return await httpClient.get(`${API.READING_LIST}/wrapup`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};
