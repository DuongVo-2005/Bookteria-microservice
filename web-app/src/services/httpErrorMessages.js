// Phase 7 (be-report.md, 2026-09-18): api-gateway giờ có rate limiting
// (Redis-backed, 20 req/s, burst 40, key theo IP) áp dụng cho MỌI route —
// bất kỳ lời gọi API nào trong toàn app đều có thể nhận 429. Dùng chung 1
// message ở đây thay vì lặp lại ở từng *ErrorMessages.js.
export const getRateLimitMessage = (error) => {
  if (error?.response?.status === 429) {
    return "Bạn đang thao tác quá nhanh. Vui lòng thử lại sau ít giây.";
  }
  return null;
};
