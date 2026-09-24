import axios from "axios";
import { CONFIG } from "./configuration";
import { getToken, removeToken } from "../services/localStorageService";

const httpClient = axios.create({
  baseURL: CONFIG.API_GATEWAY,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // equivalent to authenticationService.logOut(); imported directly to avoid a circular import (authenticationService imports httpClient)
      const hadToken = Boolean(getToken());
      removeToken();

      // Tránh redirect loop nếu request 401 xảy ra ngay trên trang /login
      // (vd. request nền nào đó còn treo lại từ trước khi logout).
      if (!window.location.pathname.startsWith("/login")) {
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
        // Có token trước đó mà vẫn bị 401 -> phiên đã hết hạn/không hợp lệ.
        // Chưa từng có token -> chỉ đơn giản là chưa đăng nhập.
        const reason = hadToken ? "expired" : "required";
        window.location.href = `/login?returnUrl=${returnUrl}&reason=${reason}`;
      }
    }
    return Promise.reject(error);
  }
);

export default httpClient;
