import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

export const getChapterList = async (bookId, page = 0, size = 100) => {
  return await httpClient.get(`${API.READING_BASE}/books/${bookId}/chapters`, { headers: authHeader(), params: { page, size } });
};

export const getChapterContent = async (bookId, chapterNumber) => {
  return await httpClient.get(`${API.READING_BASE}/books/${bookId}/chapters/${chapterNumber}`, { headers: authHeader() });
};

// Luôn trả 200 — sách chưa từng đọc trả về mặc định status NOT_STARTED,
// KHÔNG phải 404 (đã xác nhận trực tiếp với source thật).
export const getReadingProgress = async (bookId) => {
  return await httpClient.get(`${API.READING_BASE}/progress/${bookId}`, { headers: authHeader() });
};

export const getReadingHistory = async () => {
  return await httpClient.get(`${API.READING_BASE}/progress`, { headers: authHeader() });
};

export const updateReadingProgress = async (bookId, data) => {
  return await httpClient.put(`${API.READING_BASE}/progress/${bookId}`, data, { headers: jsonHeader() });
};

export const getBookmarks = async (bookId) => {
  return await httpClient.get(`${API.READING_BASE}/books/${bookId}/bookmarks`, { headers: authHeader() });
};

export const createBookmark = async (data) => {
  return await httpClient.post(`${API.READING_BASE}/bookmarks`, data, { headers: jsonHeader() });
};

export const deleteBookmark = async (bookmarkId) => {
  return await httpClient.delete(`${API.READING_BASE}/bookmarks/${bookmarkId}`, { headers: authHeader() });
};

export const getHighlights = async (bookId) => {
  return await httpClient.get(`${API.READING_BASE}/books/${bookId}/highlights`, { headers: authHeader() });
};

// color phải thuộc yellow|green|pink|blue|purple (đã xác nhận trực tiếp) — BE
// từ chối (400) nếu không thuộc tập này, không tự coerce về giá trị khác.
export const createHighlight = async (data) => {
  return await httpClient.post(`${API.READING_BASE}/highlights`, data, { headers: jsonHeader() });
};

export const updateHighlightNote = async (highlightId, note) => {
  return await httpClient.patch(`${API.READING_BASE}/highlights/${highlightId}`, { note }, { headers: jsonHeader() });
};

export const deleteHighlight = async (highlightId) => {
  return await httpClient.delete(`${API.READING_BASE}/highlights/${highlightId}`, { headers: authHeader() });
};

// Phase 5 (be-report.md, 2026-09-18): 1 click tạo post mới ở post-service từ
// highlight đã lưu (nội dung tự ghép "<selectedText>"\n\n<note>, không có
// bước preview/sửa ở BE) — khác ShareQuoteDialog (client tự dựng thiệp trích
// dẫn rồi gọi createPost thường), đây là API mới riêng cho highlight đã lưu.
// FEAT-04 (bổ sung 2026-09-19): mediaUrls optional — không truyền vẫn y hệt
// hành vi cũ (share dạng text-only), không phải breaking change.
export const shareHighlight = async (highlightId, mediaUrls) => {
  const body = {};
  if (mediaUrls && mediaUrls.length > 0) body.mediaUrls = mediaUrls;
  return await httpClient.post(`${API.READING_BASE}/highlights/${highlightId}/share`, body, { headers: jsonHeader() });
};

// FEAT-04 — dữ liệu thật (không tin FE tự nhập) để vẽ thiệp trích dẫn: nội
// dung highlight + tên sách/tác giả THẬT + ảnh bìa sách.
export const getQuoteCardData = async (highlightId) => {
  return await httpClient.get(`${API.READING_BASE}/highlights/${highlightId}/quote-card`, { headers: authHeader() });
};

// BA v2 Phase 3 §3.1 (be-report.md, bổ sung 2026-09-22) — Offline Sync Batch:
// áp dụng 1 lô ghi (progress + highlight + bookmark) tích luỹ lúc offline
// trong 1 lần gọi. Progress Monotonicity ở BE tự bỏ qua write cũ hơn (không
// phải lỗi) — đọc lại progressSkipped để biết, không coi là thất bại.
export const syncBatch = async (payload) => {
  return await httpClient.post(API.SYNC_BATCH, payload, { headers: jsonHeader() });
};

// POST /books/{bookId}/import — Admin only, multipart "file" (.epub/.pdf), BE
// tự tách chương. Trả { chaptersCreated, warnings[] } — PDF không có outline
// sẽ luôn ra 1 chương duy nhất kèm warning, đây là giới hạn đã biết của BE,
// không phải lỗi FE cần xử lý gì thêm ngoài hiển thị lại đúng warnings đó.
export const importBookContent = async (bookId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  return await httpClient.post(`${API.READING_BASE}/books/${bookId}/import`, formData, {
    headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "multipart/form-data" },
  });
};
