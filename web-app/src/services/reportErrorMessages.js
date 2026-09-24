import { getRateLimitMessage } from "./httpErrorMessages";

// report-service ErrorCode (be-report.md, Phase 4, 2026-09-18) — đọc trực
// tiếp từ đặc tả (docs/phase4-moderation-report-tasks.md §15), không suy đoán.
export const REPORT_ERROR_MESSAGES = {
  1008: "Không tìm thấy báo cáo này.",
  1009: "Vui lòng chọn loại nội dung cần báo cáo.",
  1010: "Không xác định được nội dung cần báo cáo.",
  1011: "Vui lòng nhập lý do báo cáo.",
  1012: "Bạn đã có 1 báo cáo đang chờ xử lý cho nội dung này rồi.",
  1013: "Trạng thái xử lý không hợp lệ.",
  // BA Backlog GAP-03 (be-report.md, bổ sung 2026-09-18).
  1014: "Nội dung hoặc người dùng bị báo cáo không còn tồn tại.",
  1015: "Vui lòng chọn hành động xử lý trước khi đánh dấu Đã xử lý.",
  1016: "Hành động xử lý không khớp với loại nội dung của báo cáo này.",
};

export const getReportErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  if (REPORT_ERROR_MESSAGES[code]) return REPORT_ERROR_MESSAGES[code];
  if (getRateLimitMessage(error)) return getRateLimitMessage(error);

  const status = error?.response?.status;
  if (status === 401) return "Bạn cần đăng nhập lại để tiếp tục.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  if (status === 404) return "Không tìm thấy báo cáo này.";
  return error?.response?.data?.message || "Đã có lỗi xảy ra. Vui lòng thử lại.";
};
