import React from "react";
import { Alert, Button, Box } from "@mui/material";

export const ErrorBanner = ({ message = "Đã có lỗi xảy ra khi tải dữ liệu.", onRetry }) => {
  return (
    <Box sx={{ mb: 3 }}>
      <Alert
        severity="error"
        action={
          onRetry && (
            <Button color="inherit" size="small" onClick={onRetry} sx={{ fontWeight: 600 }}>
              Thử lại
            </Button>
          )
        }
      >
        {message}
      </Alert>
    </Box>
  );
};

export default ErrorBanner;
