import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  TextField,
  Typography,
  Snackbar,
  Alert,
} from "@mui/material";

import GoogleIcon from "@mui/icons-material/Google";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { logIn, isAuthenticated } from "../services/authenticationService";
import { CONFIG } from "../configurations/configuration";
import { getSafeReturnUrl } from "../utils/urlUtils";

export default function Login() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const returnUrl = getSafeReturnUrl(searchParams.get("returnUrl"));
  const reason = searchParams.get("reason");

  const handleCloseSnackBar = (event, reason) => {
    if (reason === "clickaway") {
      return;
    }

    setSnackBarOpen(false);
  };

  const handleClick = () => {
    // BE redirect callback (/oauth2/redirect) không giữ lại returnUrl gốc,
    // nên lưu tạm ở sessionStorage để khôi phục lại đúng trang sau khi đăng
    // nhập Google xong (xem OAuth2Redirect.jsx).
    sessionStorage.setItem("oauth2ReturnUrl", returnUrl);
    window.location.href = `${CONFIG.API_GATEWAY}/identity/oauth2/authorization/google`;
  };

  useEffect(() => {
    if (isAuthenticated()) {
      navigate(returnUrl, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [snackBarOpen, setSnackBarOpen] = useState(false);
  const [snackBarMessage, setSnackBarMessage] = useState("");

  // Thông báo lý do quay lại trang login — không hiện chi tiết kỹ thuật
  // (401/Unauthorized/JWT invalid) cho người dùng cuối.
  const authReasonMessage =
    reason === "expired"
      ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
      : reason === "required"
      ? "Vui lòng đăng nhập để tiếp tục."
      : reason === "google_cancelled"
      ? "Đăng nhập bằng Google đã bị huỷ."
      : reason === "google_failed"
      ? "Đăng nhập bằng Google thất bại. Vui lòng thử lại."
      : null;

  const handleSubmit = async (event) => {
    event.preventDefault();

    // Validate client-side trước khi gọi API — tránh gọi thẳng lên BE (sẽ trả
    // 400 chung chung) khi rõ ràng người dùng chưa nhập gì.
    const missingUsername = !username.trim();
    const missingPassword = !password.trim();
    if (missingUsername || missingPassword) {
      setSnackBarMessage(
        missingUsername && missingPassword
          ? "Vui lòng nhập thông tin đăng nhập."
          : missingUsername
          ? "Vui lòng nhập tài khoản."
          : "Vui lòng nhập mật khẩu."
      );
      setSnackBarOpen(true);
      return;
    }

    try {
      await logIn(username, password);
      navigate(returnUrl, { replace: true });
    } catch (error) {
      // identity-service trả 2 mã khác nhau (1005 = không có tài khoản, 1006 =
      // sai mật khẩu) — gộp chung 1 thông báo, không lộ tài khoản có tồn tại
      // hay không (đúng thông lệ bảo mật đăng nhập).
      const code = error?.response?.data?.code;
      const message =
        code === 1005 || code === 1006
          ? "Tài khoản hoặc mật khẩu không chính xác."
          : error?.response?.data?.message || "Đã có lỗi xảy ra. Vui lòng thử lại.";
      setSnackBarMessage(message);
      setSnackBarOpen(true);
    }
  };

  return (
    <>
      <Snackbar
        open={snackBarOpen}
        onClose={handleCloseSnackBar}
        autoHideDuration={3000}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
      >
        <Alert
          onClose={handleCloseSnackBar}
          severity="error"
          variant="filled"
          sx={{ width: "100%" }}
        >
          {snackBarMessage}
        </Alert>
      </Snackbar>
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        height="100vh"
        bgcolor={"#f0f2f5"}
      >
        <Card
          sx={{
            minWidth: 300,
            maxWidth: 400,
            boxShadow: 3,
            borderRadius: 3,
            padding: 4,
          }}
        >
          <CardContent>
            <Typography variant="h5" component="h1" gutterBottom>
              Welcome to Bookteria
            </Typography>
            {authReasonMessage && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {authReasonMessage}
              </Alert>
            )}
            <Box
              component="form"
              display="flex"
              flexDirection="column"
              alignItems="center"
              justifyContent="center"
              width="100%"
              onSubmit={handleSubmit}
            >
              <TextField
                label="Username"
                variant="outlined"
                fullWidth
                margin="normal"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <TextField
                label="Password"
                type="password"
                variant="outlined"
                fullWidth
                margin="normal"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <Button
                type="submit"
                variant="contained"
                color="primary"
                size="large"
                onClick={handleSubmit}
                fullWidth
                sx={{
                  mt: "15px",
                  mb: "25px",
                }}
              >
                Login
              </Button>
              <Divider></Divider>
            </Box>

            <Box display="flex" flexDirection="column" width="100%" gap="25px">
              <Button
                type="button"
                variant="contained"
                color="secondary"
                size="large"
                onClick={handleClick}
                fullWidth
                sx={{ gap: "10px" }}
              >
                <GoogleIcon />
                Continue with Google
              </Button>
              <Button
                type="submit"
                variant="contained"
                color="success"
                size="large"
              >
                Create an account
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
