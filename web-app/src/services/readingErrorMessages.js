import { getRateLimitMessage } from "./httpErrorMessages";

// Mã lỗi lấy đúng từ reading-service/src/main/java/.../exception/ErrorCode.java
// (đã đọc trực tiếp source thật, không suy đoán). Lưu ý: mọi lỗi validate
// @Valid (thiếu field bắt buộc...) đều rơi về 1001 vì các DTO ở service này
// không gắn message= riêng cho từng field.
export const READING_ERROR_MESSAGES = {
  1001: "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.",
  1006: "Bạn cần đăng nhập lại để tiếp tục.",
  1007: "Bạn không có quyền thực hiện thao tác này.",
  1008: "Không tìm thấy chương sách này.",
  1009: "Không tìm thấy dấu trang này.",
  1010: "Không tìm thấy đoạn tô sáng này.",
  1011: "Màu tô sáng không hợp lệ.",
  // Phase 5 (be-report.md, 2026-09-18): POST /highlights/{id}/share.
  1014: "Không thể chia sẻ trích dẫn này lúc này. Vui lòng thử lại sau.",
};

export const getReadingErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  return (
    READING_ERROR_MESSAGES[code] ||
    getRateLimitMessage(error) ||
    error?.response?.data?.message ||
    "Đã có lỗi xảy ra. Vui lòng thử lại."
  );
};
