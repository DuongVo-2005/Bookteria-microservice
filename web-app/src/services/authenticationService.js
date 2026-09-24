import { getToken, removeToken, setToken } from "./localStorageService";
import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";

export const logIn = async (username, password) => {
  const response = await httpClient.post(API.LOGIN, {
    username: username,
    password: password,
  });

  setToken(response.data?.result?.token);

  return response;
};

export const logOut = () => {
  removeToken();
};

// Đổi "code" ngắn hạn (dùng 1 lần, hết hạn 60s) nhận được ở redirect callback
// sau khi đăng nhập Google thành công, lấy JWT thật — thay cho cách cũ nhận
// thẳng token qua query param (rủi ro lộ token qua URL/log/lịch sử trình
// duyệt). Phải gọi ngay khi vừa nhận redirect, không lưu code lại dùng sau.
export const exchangeOAuth2Code = async (code) => {
  const response = await httpClient.post(API.OAUTH2_EXCHANGE, { code });
  setToken(response.data?.result?.token);
  return response;
};

// Decode phần payload của JWT (base64url), dùng chung cho isAuthenticated/
// getCurrentUserId/isAdmin — không dùng thư viện ngoài, giữ nguyên cách làm
// đã có sẵn trong file này, chỉ gộp lại 1 chỗ thay vì lặp lại 3 lần.
const decodeToken = (token) => {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(
      decodeURIComponent(
        atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
          .split("")
          .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
          .join("")
      )
    );
  } catch (error) {
    return null;
  }
};

// Trả về boolean: có token VÀ token đó chưa hết hạn (đọc claim `exp` chuẩn
// JWT, tính bằng giây). Việc BE từ chối JWT hết hạn/sai vẫn là nguồn xác
// nhận cuối cùng (qua 401 + interceptor) — kiểm tra `exp` ở đây chỉ để FE
// phát hiện sớm, tránh render trang riêng tư rồi mới gọi API thất bại.
export const isAuthenticated = () => {
  const token = getToken();
  if (!token) return false;

  const decoded = decodeToken(token);
  if (!decoded) return false;

  if (decoded.exp && decoded.exp * 1000 <= Date.now()) {
    return false;
  }

  return true;
};

export const getCurrentUserId = () => {
  const token = getToken();
  if (!token) return null;
  const decoded = decodeToken(token);
  return decoded?.sub || null;
};

// BA v2 Phase 1 (be-report.md, bổ sung 2026-09-19): từ khi User có thể được
// gán permission trực tiếp (ngoài Role), `scope` claim không còn luôn là 1
// token đơn "ROLE_ADMIN" nữa — buildScope() giờ nối cả ROLE_X lẫn từng
// permission name (cách nhau bằng dấu cách, chuẩn Spring authority string).
// So sánh bằng "===" như trước sẽ vỡ ngay khi 1 ADMIN được cấp thêm permission
// trực tiếp — phải tách token và kiểm tra có chứa "ROLE_ADMIN" hay không.
const getScopeTokens = () => {
  const token = getToken();
  if (!token) return [];
  const decoded = decodeToken(token);
  if (!decoded?.scope) return [];
  return decoded.scope.split(" ").filter(Boolean);
};

export const isAdmin = () => getScopeTokens().includes("ROLE_ADMIN");

// ADMIN luôn được coi là có mọi permission ("Ultimate Authority", be-report.md
// §2.2) — không cần được cấp permission đó trực tiếp.
export const hasPermission = (permission) => {
  const tokens = getScopeTokens();
  return tokens.includes("ROLE_ADMIN") || tokens.includes(permission);
};
