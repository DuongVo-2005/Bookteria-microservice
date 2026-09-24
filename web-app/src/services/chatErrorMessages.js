import { getRateLimitMessage } from "./httpErrorMessages";

// Mã lỗi lấy đúng từ chat-service/src/main/java/.../exception/ErrorCode.java
// (đã đọc trực tiếp source thật, không suy diễn).
export const CHAT_ERROR_MESSAGES = {
  1006: "Bạn cần đăng nhập lại để tiếp tục.",
  1007: "Bạn không có quyền thực hiện thao tác này.",
  1009: "Dữ liệu không hợp lệ.",
  1010: "Không tìm thấy cuộc trò chuyện này.",
  1011: "Không thể nhắn tin — một trong hai người đang chặn người kia.",
  1012: "Bạn chỉ có thể gửi 1 tin nhắn cho tới khi người nhận chấp nhận lời mời.",
  1013: "Bạn cần chấp nhận lời mời nhắn tin trước khi trả lời.",
  1014: "Lời mời nhắn tin này đã bị từ chối.",
  1015: "Lời mời nhắn tin này không còn ở trạng thái chờ.",
};

export const getChatErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  return (
    CHAT_ERROR_MESSAGES[code] ||
    getRateLimitMessage(error) ||
    error?.response?.data?.message ||
    "Đã có lỗi xảy ra. Vui lòng thử lại."
  );
};
