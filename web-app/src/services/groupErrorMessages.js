import { getRateLimitMessage } from "./httpErrorMessages";

// Mã lỗi lấy đúng từ group-service/src/main/java/.../exception/ErrorCode.java
// (đã đọc trực tiếp source thật, không suy đoán).
export const GROUP_ERROR_MESSAGES = {
  1006: "Bạn cần đăng nhập lại để tiếp tục.",
  1007: "Bạn không có quyền thực hiện thao tác này.",
  1008: "Không tìm thấy hội nhóm này.",
  1009: "Bạn không phải là thành viên của nhóm này.",
  1010: "Bạn đã là thành viên của nhóm này rồi.",
  1011: "Trưởng nhóm phải chuyển quyền sở hữu trước khi rời nhóm.",
  1012: "Bạn không có quyền thực hiện thao tác này trong nhóm.",
  1013: "Không thể thay đổi vai trò của trưởng nhóm.",
  1014: "Không tìm thấy bài đăng này.",
  1015: "Không tìm thấy bình luận này.",
  1016: "Không tìm thấy thành viên này trong nhóm.",
  1017: "Không tìm thấy người dùng này.",
  1018: "Không tìm thấy cuốn sách này.",
  1019: "Bạn đã là trưởng nhóm này rồi.",
  // BA v2 Phase 2 (be-report.md, bổ sung 2026-09-19) — Group.status=FROZEN.
  1021: "Nhóm này đang tạm khoá, không thể đăng bài mới.",
};

export const getGroupErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  return (
    GROUP_ERROR_MESSAGES[code] ||
    getRateLimitMessage(error) ||
    error?.response?.data?.message ||
    "Đã có lỗi xảy ra. Vui lòng thử lại."
  );
};
