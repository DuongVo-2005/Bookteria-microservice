import React, { useEffect, useRef, useState } from "react";
import { Box, CircularProgress, Typography, Chip } from "@mui/material";

const JSAPI_SRC = "https://www.google.com/books/jsapi.js";

// Load đúng 1 lần cho cả app (nhiều Viewer trên cùng trang chỉ cần 1 script
// tag) — cache lại promise để lần gọi sau không tự chèn thêm <script>.
let jsapiLoadPromise = null;
const loadGoogleBooksJsApi = () => {
  if (window.google?.books) return Promise.resolve();
  if (jsapiLoadPromise) return jsapiLoadPromise;

  jsapiLoadPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = JSAPI_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      jsapiLoadPromise = null;
      reject(new Error("Không tải được Google Books JSAPI"));
    };
    document.head.appendChild(script);
  });
  return jsapiLoadPromise;
};

// Bọc đúng theo Google Books Embedded Viewer API thật (google.books.load() +
// google.books.setOnLoadCallback() + new google.books.DefaultViewer(el).load(id))
// — KHÔNG dùng iframe tự chế, đúng yêu cầu.
//
// ⚠️ API công khai của Google KHÔNG có sự kiện "đổi trang"/"vị trí đọc" nào
// được tài liệu hoá — prop `onProgressChange` được giữ lại đúng theo yêu cầu
// nhưng KHÔNG được gọi ở đâu cả (không giả lập page number). `viewability`
// chỉ dùng để hiện nhãn UI (vd. "Bản xem trước"), không cấu hình được viewer
// theo giá trị này — Google tự giới hạn quyền xem theo đúng volume ở phía họ.
export const GoogleBooksViewer = ({ googleBookId, viewability, onProgressChange }) => {
  const containerRef = useRef(null);
  const [status, setStatus] = useState("loading"); // loading | ready | error
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    const containerEl = containerRef.current;
    if (!googleBookId || !containerEl) return undefined;

    let cancelled = false;
    setStatus("loading");
    setErrorMessage(null);

    loadGoogleBooksJsApi()
      .then(() => {
        if (cancelled) return;
        window.google.books.load();
        window.google.books.setOnLoadCallback(() => {
          if (cancelled) return;
          try {
            containerEl.innerHTML = "";
            const viewer = new window.google.books.DefaultViewer(containerEl);
            viewer.load(
              googleBookId,
              () => {
                if (!cancelled) setStatus("ready");
              },
              () => {
                if (!cancelled) {
                  setStatus("error");
                  setErrorMessage("Google Books không thể tải nội dung sách này.");
                }
              }
            );
          } catch (err) {
            if (!cancelled) {
              setStatus("error");
              setErrorMessage("Không thể khởi tạo trình đọc Google Books.");
            }
          }
        });
      })
      .catch(() => {
        if (!cancelled) {
          setStatus("error");
          setErrorMessage("Không tải được Google Books JSAPI.");
        }
      });

    return () => {
      cancelled = true;
      containerEl.innerHTML = "";
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [googleBookId]);

  // Giữ tham chiếu prop để không bị lint cảnh báo "unused" — xem ghi chú ở
  // trên: chưa có nguồn sự kiện thật nào để gọi hàm này.
  void onProgressChange;

  return (
    <Box sx={{ position: "relative", width: "100%", height: "100%", minHeight: 480, bgcolor: "#f5f5f5", borderRadius: 2, overflow: "hidden" }}>
      {viewability === "PARTIAL" && status === "ready" && (
        <Chip label="Bản xem trước" size="small" color="warning" sx={{ position: "absolute", top: 10, right: 10, zIndex: 2, fontWeight: 700 }} />
      )}

      {status === "loading" && (
        <Box sx={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 1.5 }}>
          <CircularProgress />
          <Typography variant="body2" color="text.secondary">
            Đang tải trình đọc Google Books...
          </Typography>
        </Box>
      )}

      {status === "error" && (
        <Box sx={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 1, p: 3, textAlign: "center" }}>
          <Typography variant="body2" color="error">
            {errorMessage}
          </Typography>
        </Box>
      )}

      <Box ref={containerRef} sx={{ width: "100%", height: "100%", minHeight: 480 }} />
    </Box>
  );
};

export default GoogleBooksViewer;
