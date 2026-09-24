import { getRateLimitMessage } from "./httpErrorMessages";

// post-service ErrorCode mới (be-report.md, Phase 3, 2026-09-18) — chỉ map
// đúng 4 mã BE mô tả rõ nghĩa (1010/1013/1014/1015). Báo cáo chỉ nói chung
// "thêm 7 mã lỗi (1009-1015)" mà không nêu rõ nghĩa 1009/1011/1012 — không tự
// đoán, để fallback theo HTTP status hoặc message thật từ BE.
export const POST_ERROR_MESSAGES = {
  1010: "Bạn không có quyền xem bài đăng này.",
  1013: "Nội dung bài đăng không được để trống.",
  1014: "Không tìm thấy cuốn sách này.",
  1015: "Bạn không có quyền chỉnh sửa/xoá bình luận này.",
};

export const getPostErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  if (POST_ERROR_MESSAGES[code]) return POST_ERROR_MESSAGES[code];
  if (getRateLimitMessage(error)) return getRateLimitMessage(error);

  const status = error?.response?.status;
  if (status === 401) return "Bạn cần đăng nhập lại để tiếp tục.";
  if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
  if (status === 404) return "Không tìm thấy bài đăng này.";
  return error?.response?.data?.message || "Đã có lỗi xảy ra. Vui lòng thử lại.";
};
