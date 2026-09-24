import { getRateLimitMessage } from "./httpErrorMessages";

export const BOOK_ERROR_MESSAGES = {
  1006: "Bạn cần đăng nhập lại để tiếp tục.",
  1007: "Bạn không có quyền thực hiện thao tác này.",
  1011: "Không tìm thấy sách này.",
  1012: "Thể loại này không tồn tại.",
  1013: "Vui lòng nhập tên sách.",
  1014: "Mã ISBN-13 phải gồm đúng 13 chữ số.",
  1015: "Vui lòng chọn ít nhất một tác giả.",
  1016: "Thiếu thông tin tác giả cho một mục đã chọn.",
  1017: "Vui lòng chọn ít nhất một thể loại.",
  1018: "Thiếu thông tin thể loại cho một mục đã chọn.",
  1019: "Vui lòng chọn nhà xuất bản.",
  1020: "Thiếu thông tin nhà xuất bản.",
  1021: "Số trang phải lớn hơn 0.",
  1022: "Vui lòng nhập từ khoá tìm kiếm.",
  1023: "Tác giả này không tồn tại.",
  1024: "Nhà xuất bản này không tồn tại.",
  1026: "Không tìm thấy đánh giá này.",
  1027: "Bạn đã đánh giá sách này rồi.",
  1028: "Không tìm thấy mục này trong kệ sách.",
  1029: "Sách này đã có trong kệ sách của bạn.",
  1030: "Số trang nhập vào không hợp lệ.",
  1031: "Vui lòng chọn số sao đánh giá.",
  1032: "Số sao đánh giá phải trong khoảng 1-5.",
  1033: "Mã ISBN-13 này đã tồn tại.",
  1034: "Đường dẫn (slug) này đã tồn tại.",
  // BA v2 Phase 1 (be-report.md, bổ sung 2026-09-19) — reviews-lock.
  1044: "Đánh giá cho sách này đang tạm khoá.",
  // BA v2 Phase 3 (be-report.md, bổ sung 2026-09-22) — Shelf Privacy.
  1048: "Bạn không có quyền xem kệ sách của người dùng này.",
};

export const getBookErrorMessage = (error) => {
  const code = error?.response?.data?.code;
  return (
    BOOK_ERROR_MESSAGES[code] ||
    getRateLimitMessage(error) ||
    error?.response?.data?.message ||
    "Đã có lỗi xảy ra. Vui lòng thử lại."
  );
};
