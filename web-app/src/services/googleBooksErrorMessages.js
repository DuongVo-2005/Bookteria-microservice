import { getRateLimitMessage } from "./httpErrorMessages";

// Mã lỗi Google Books lấy đúng theo be-report-and-fe-requirements.md
// (2026-09-18) — book-service đã verify HTTP thật, không suy đoán.
export const GOOGLE_BOOKS_ERROR_MESSAGES = {
  1035: "Không tìm thấy sách này trên Google Books.",
  1036: "Google Books tạm thời không phản hồi (có thể do hết hạn mức truy vấn trong ngày). Vui lòng thử lại sau.",
  1037: "Tiến độ đọc không hợp lệ (phải trong khoảng 0-100%).",
};

export const getGoogleBooksErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  if (GOOGLE_BOOKS_ERROR_MESSAGES[code]) return GOOGLE_BOOKS_ERROR_MESSAGES[code];
  if (getRateLimitMessage(error)) return getRateLimitMessage(error);

  const status = error?.response?.status;
  if (status === 503) return GOOGLE_BOOKS_ERROR_MESSAGES[1036];
  if (status === 404) return "Không tìm thấy sách.";
  if (status === 401) return "Bạn cần đăng nhập lại để tiếp tục.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  return error?.response?.data?.message || "Không thể tải thông tin đọc sách.";
};
