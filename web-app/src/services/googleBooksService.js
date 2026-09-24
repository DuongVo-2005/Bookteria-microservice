import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

// Google Books integration (book-service) — BE xác nhận thật 2026-09-18,
// đã verify HTTP thật ("BE xong 100%"). Toàn bộ endpoint dưới nằm chung
// controller /books của book-service (qua gateway /api/v1/book/books/...),
// KHÔNG phải "/books/..." như bản nháp ban đầu — đã sửa lại cho đúng.
const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

// GET /book/books/google/search?q=&page=&size= — gọi trực tiếp Google, KHÔNG
// lưu DB, chỉ để Admin xem trước khi import. `size` BE tự clamp tối đa 40.
export const searchGoogleBooks = async (q, page = 0, size = 10) => {
  return await httpClient.get(API.GOOGLE_BOOKS_SEARCH, { headers: authHeader(), params: { q, page, size } });
};

// POST /book/books/google/import/{googleBookId} — ADMIN only. Import lại id
// đã import rồi -> trả về sách cũ, không lỗi, không tạo trùng.
export const importGoogleBook = async (googleBookId) => {
  return await httpClient.post(`${API.GOOGLE_BOOKS_IMPORT}/${googleBookId}`, {}, { headers: authHeader() });
};

// POST /book/books/google/import — ADMIN only, batch theo từ khoá.
export const batchImportGoogleBooks = async ({ query, maxResults = 20, startIndex = 0 }) => {
  return await httpClient.post(API.GOOGLE_BOOKS_IMPORT, { query, maxResults, startIndex }, { headers: jsonHeader() });
};

// GET /book/books/{bookId}/reading-access
export const getReadingAccess = async (bookId) => {
  return await httpClient.get(`${API.BOOKS}/${bookId}/reading-access`, { headers: authHeader() });
};

// GET /book/books/{bookId}/reading-progress — upsert theo user hiện tại +
// bookId, cùng entity với "Kệ sách" (/me/reading-list), không cần biết
// readingList.id trước. Chưa từng đọc -> status "WANT_TO_READ", không phải 404.
export const getReadingProgress = async (bookId) => {
  return await httpClient.get(`${API.BOOKS}/${bookId}/reading-progress`, { headers: authHeader() });
};

// PUT /book/books/{bookId}/reading-progress — currentPage/progressPercent
// đều optional, gửi field nào cập nhật field đó.
export const updateReadingProgress = async (bookId, data) => {
  return await httpClient.put(`${API.BOOKS}/${bookId}/reading-progress`, data, { headers: jsonHeader() });
};

// 5 case theo đúng mục 5 của yêu cầu UI ban đầu — dùng chung giữa BookDetail
// (quyết định nút/nhãn) và BookReaderPage (tự kiểm tra lại khi vào thẳng URL).
export const GOOGLE_BOOKS_ACCESS_CASE = {
  FULL_EMBED: "FULL_EMBED", // viewability=ALL_PAGES, embeddable=true -> "Đọc sách"
  PARTIAL_EMBED: "PARTIAL_EMBED", // viewability=PARTIAL, embeddable=true -> "Đọc bản xem trước"
  EXTERNAL_ONLY: "EXTERNAL_ONLY", // embeddable=false, có webReaderLink -> "Đọc trên Google Books"
  NO_ACCESS: "NO_ACCESS", // canRead=false (luôn đúng với sách không có googleBooksAccess)
  UNKNOWN: "UNKNOWN", // không khớp case nào đã biết -> không giả định có quyền đọc
};

export const resolveGoogleBooksAccessCase = (access) => {
  if (!access || access.canRead === false) return GOOGLE_BOOKS_ACCESS_CASE.NO_ACCESS;
  if (access.embeddable === true && access.viewability === "ALL_PAGES") return GOOGLE_BOOKS_ACCESS_CASE.FULL_EMBED;
  if (access.embeddable === true && access.viewability === "PARTIAL") return GOOGLE_BOOKS_ACCESS_CASE.PARTIAL_EMBED;
  if (access.embeddable === false && access.webReaderLink) return GOOGLE_BOOKS_ACCESS_CASE.EXTERNAL_ONLY;
  if (access.viewability === "NO_PAGES") return GOOGLE_BOOKS_ACCESS_CASE.NO_ACCESS;
  return GOOGLE_BOOKS_ACCESS_CASE.UNKNOWN;
};
