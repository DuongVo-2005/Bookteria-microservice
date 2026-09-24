// Chỉ chấp nhận returnUrl dạng đường dẫn nội bộ ("/..."), không phải URL
// tuyệt đối/protocol-relative ("//host/...", "http://...") — tránh open
// redirect nếu query string bị chỉnh tay.
export const getSafeReturnUrl = (raw) => {
  if (!raw) return "/";
  try {
    const decoded = decodeURIComponent(raw);
    if (decoded.startsWith("/") && !decoded.startsWith("//")) {
      return decoded;
    }
  } catch (error) {
    // ignore, fall through to default
  }
  return "/";
};
