import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Box, CircularProgress, Typography } from "@mui/material";
import { exchangeOAuth2Code } from "../services/authenticationService";
import { getSafeReturnUrl } from "../utils/urlUtils";

// Trang nhận callback sau khi đăng nhập Google thành công — BE redirect
// trình duyệt về đây kèm ?code=<code ngắn hạn>. Code chỉ dùng được đúng 1
// lần, hết hạn sau 60 giây, nên phải đổi lấy JWT thật ngay khi vừa vào trang
// này (không lưu code lại dùng sau) qua POST /identity/auth/oauth2/exchange.
export default function OAuth2Redirect() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const code = searchParams.get("code");

    if (!code) {
      // BE hiện CHƯA có failureHandler riêng cho luồng Google OAuth2 (user bấm
      // Cancel/Deny ở màn hình consent Google sẽ thấy trang lỗi mặc định của
      // Spring Security ngay trên domain identity-service, không quay lại đây).
      // Nhánh "error" dưới đây chỉ có tác dụng ngay khi BE bổ sung failureHandler
      // redirect về /oauth2/redirect?error=... — chuẩn bị sẵn, không chặn gì.
      const error = searchParams.get("error");
      navigate(`/login?reason=${error ? "google_cancelled" : "required"}`, { replace: true });
      return;
    }

    exchangeOAuth2Code(code)
      .then(() => {
        const savedReturnUrl = sessionStorage.getItem("oauth2ReturnUrl");
        sessionStorage.removeItem("oauth2ReturnUrl");
        navigate(getSafeReturnUrl(savedReturnUrl), { replace: true });
      })
      .catch(() => {
        // Code sai/hết hạn/đã dùng (1011) hoặc lỗi khác — không có JWT nào để
        // dùng tiếp, quay lại trang login với thông báo phù hợp.
        navigate("/login?reason=google_failed", { replace: true });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      height="100vh"
      gap={2}
      bgcolor="#f0f2f5"
    >
      <CircularProgress />
      <Typography color="text.secondary">Đang đăng nhập...</Typography>
    </Box>
  );
}
