import { getRateLimitMessage } from "./httpErrorMessages";

// identity-service ErrorCode liên quan quản trị user (be-report.md, OPS-03,
// bổ sung 2026-09-19) — đọc trực tiếp từ source thật.
export const IDENTITY_ERROR_MESSAGES = {
  1014: "Vui lòng nhập lý do khoá tài khoản.",
  1015: "Vui lòng chọn thời hạn khoá hợp lệ.",
};

export const getIdentityErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  if (IDENTITY_ERROR_MESSAGES[code]) return IDENTITY_ERROR_MESSAGES[code];
  if (getRateLimitMessage(error)) return getRateLimitMessage(error);

  const status = error?.response?.status;
  if (status === 401) return "Bạn cần đăng nhập lại để tiếp tục.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  if (status === 404) return "Không tìm thấy người dùng này.";
  return error?.response?.data?.message || "Đã có lỗi xảy ra. Vui lòng thử lại.";
};
