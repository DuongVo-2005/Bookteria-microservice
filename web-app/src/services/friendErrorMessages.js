import { getRateLimitMessage } from "./httpErrorMessages";

export const FRIEND_ERROR_MESSAGES = {
  1006: "Bạn cần đăng nhập lại để tiếp tục.",
  1007: "Bạn không có quyền thực hiện thao tác này.",
  1008: "Không tìm thấy người dùng này.",
  1009: "Không thể tự gửi lời mời kết bạn cho chính mình.",
  1010: "Đã có lời mời kết bạn đang chờ giữa hai người.",
  1011: "Hai bạn đã là bạn bè.",
  1012: "Không tìm thấy lời mời kết bạn này.",
  1013: "Lời mời kết bạn này không còn ở trạng thái chờ.",
  1014: "Hai người chưa phải là bạn bè.",
  1015: "Bạn đã chặn người này rồi.",
  1016: "Bạn chưa từng chặn người này.",
  1017: "Không thể tự chặn chính mình.",
  1018: "Không thể gửi lời mời vì một trong hai bên đang chặn bên kia.",
};

export const getFriendErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  return (
    FRIEND_ERROR_MESSAGES[code] ||
    getRateLimitMessage(error) ||
    error?.response?.data?.message ||
    "Đã có lỗi xảy ra. Vui lòng thử lại."
  );
};
