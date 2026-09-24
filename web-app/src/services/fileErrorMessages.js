import { getRateLimitMessage } from "./httpErrorMessages";

// file-service ErrorCode (be-report.md Phase 7 + GAP-05, 2026-09-18/19) —
// đọc trực tiếp từ đặc tả, không suy đoán.
export const FILE_ERROR_MESSAGES = {
  1008: "Không tìm thấy file này.",
  1009: "File không được để trống.",
  1010: "Chỉ hỗ trợ ảnh JPG, PNG, GIF hoặc WEBP.",
  1011: "Kích thước file vượt quá 10MB.",
};

export const getFileErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  if (FILE_ERROR_MESSAGES[code]) return FILE_ERROR_MESSAGES[code];
  if (getRateLimitMessage(error)) return getRateLimitMessage(error);

  const status = error?.response?.status;
  if (status === 401) return "Bạn cần đăng nhập lại để tiếp tục.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  if (status === 404) return "Không tìm thấy file này.";
  return error?.response?.data?.message || "Không thể tải ảnh lên. Vui lòng thử lại.";
};
