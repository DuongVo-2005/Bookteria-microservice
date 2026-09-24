import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Box, IconButton, Typography, Tooltip, CircularProgress, Button, LinearProgress } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import FullscreenIcon from "@mui/icons-material/Fullscreen";
import FullscreenExitIcon from "@mui/icons-material/FullscreenExit";
import NavigateBeforeIcon from "@mui/icons-material/NavigateBefore";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import { getBookById } from "../services/bookService";
import { getReadingAccess, getReadingProgress, resolveGoogleBooksAccessCase, GOOGLE_BOOKS_ACCESS_CASE } from "../services/googleBooksService";
import { getGoogleBooksErrorMessage } from "../services/googleBooksErrorMessages";
import { GoogleBooksViewer } from "../components/book/GoogleBooksViewer";
import { EmptyState } from "../components/book/EmptyState";

// Route /read/books/:bookId — trải nghiệm đọc qua Google Books Embedded
// Viewer (KHÁC HẲN /reader/:bookId đã có, dùng nội dung chương thật của
// reading-service). Không bọc Scene/SideMenu — toàn màn hình, cùng phong
// cách với /reader/:bookId. Chỉ áp dụng cho sách có googleBookId (nhập qua
// Admin > Nhập từ Google Books).
export default function BookReaderPage() {
  const { bookId } = useParams();
  const navigate = useNavigate();

  const [book, setBook] = useState(null);
  const [access, setAccess] = useState(null);
  const [progress, setProgress] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(null);

  // Bookmark ở trang này CHỈ cục bộ (local-only) — yêu cầu không có endpoint
  // bookmark nào cho luồng Google Books (khác /reader/:bookId đã có bookmark
  // thật qua reading-service) — không tự chế API giả cho việc này.
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    if (!bookId) return;
    setLoading(true);
    setNotFound(false);
    setError(null);

    Promise.all([getBookById(bookId), getReadingAccess(bookId)])
      .then(([bookResponse, accessResponse]) => {
        setBook(bookResponse?.data?.result || null);
        setAccess(accessResponse?.data?.result || null);
        return getReadingProgress(bookId).catch(() => null);
      })
      .then((progressResponse) => {
        setProgress(progressResponse?.data?.result || null);
      })
      .catch((err) => {
        if (err?.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(getGoogleBooksErrorMessage(err));
        }
      })
      .finally(() => setLoading(false));
  }, [bookId]);

  const handleToggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  const authorNames = (book?.authors || []).map((a) => a.name).join(", ");
  const accessCase = resolveGoogleBooksAccessCase(access);

  // ---- Loading / Error / Not-found — không để trắng màn hình ----
  if (loading) {
    return (
      <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", minHeight: "100vh", gap: 2 }}>
        <CircularProgress />
        <Typography color="text.secondary">Đang kiểm tra quyền đọc...</Typography>
      </Box>
    );
  }

  if (notFound) {
    return (
      <Box sx={{ p: 4, maxWidth: 480, mx: "auto", mt: 8 }}>
        <EmptyState title="Không tìm thấy sách." actionText="Về trang Khám phá" onAction={() => navigate("/books")} />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 4, maxWidth: 480, mx: "auto", mt: 8 }}>
        <EmptyState title="Không thể tải thông tin đọc sách." description={error} actionText="Quay lại" onAction={() => navigate(-1)} />
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: "100vh", display: "flex", flexDirection: "column", bgcolor: "background.default" }}>
      {/* Header: Back, Title, Author, Bookmark, Fullscreen */}
      <Box
        component="header"
        sx={{
          height: 60,
          px: { xs: 1.5, sm: 3 },
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          bgcolor: "background.paper",
          borderBottom: "1px solid rgba(0,0,0,0.08)",
          position: "sticky",
          top: 0,
          zIndex: 1100,
          gap: 1,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, minWidth: 0 }}>
          <Tooltip title="Quay lại chi tiết sách">
            <IconButton size="small" onClick={() => navigate(`/books/${bookId}`)}>
              <ArrowBackIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: { xs: 160, sm: 360 } }}>
              {book?.title}
            </Typography>
            {authorNames && (
              <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: { xs: 160, sm: 360 }, display: "block" }}>
                {authorNames}
              </Typography>
            )}
          </Box>
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", gap: { xs: 0.5, sm: 1 }, flexShrink: 0 }}>
          <Tooltip title={isBookmarked ? "Đã đánh dấu (chỉ lưu tạm trên trình duyệt)" : "Đánh dấu (chỉ lưu tạm trên trình duyệt)"}>
            <IconButton size="small" onClick={() => setIsBookmarked((v) => !v)} sx={{ color: isBookmarked ? "#f59e0b" : "inherit" }}>
              {isBookmarked ? <BookmarkIcon fontSize="small" /> : <BookmarkBorderIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
          <Tooltip title={isFullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"}>
            <IconButton size="small" onClick={handleToggleFullscreen} sx={{ display: { xs: "none", sm: "inline-flex" } }}>
              {isFullscreen ? <FullscreenExitIcon fontSize="small" /> : <FullscreenIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {/* Tiến độ đọc lần trước (nếu có) */}
      {progress && progress.progressPercent > 0 && (
        <Box sx={{ px: 2, py: 1, bgcolor: "rgba(25, 118, 210, 0.06)", borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
          <Typography variant="caption" color="primary.main" sx={{ fontWeight: 600 }}>
            Tiếp tục đọc từ trang {progress.currentPage} ({progress.progressPercent}%)
          </Typography>
        </Box>
      )}

      {/* Khu vực đọc trung tâm — theo đúng 5 case quyền truy cập */}
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column", p: { xs: 1.5, sm: 3 } }}>
        {(accessCase === GOOGLE_BOOKS_ACCESS_CASE.FULL_EMBED || accessCase === GOOGLE_BOOKS_ACCESS_CASE.PARTIAL_EMBED) && (
          <GoogleBooksViewer googleBookId={access.googleBookId} viewability={access.viewability} onProgressChange={undefined} />
        )}

        {accessCase === GOOGLE_BOOKS_ACCESS_CASE.EXTERNAL_ONLY && (
          <Box sx={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2, textAlign: "center" }}>
            <Typography variant="body1" color="text.secondary">
              Sách này không hỗ trợ đọc nhúng trực tiếp tại đây.
            </Typography>
            <Button variant="contained" startIcon={<OpenInNewIcon />} onClick={() => window.open(access.webReaderLink, "_blank", "noopener,noreferrer")} sx={{ borderRadius: 2, textTransform: "none", fontWeight: 700 }}>
              Đọc trên Google Books
            </Button>
          </Box>
        )}

        {accessCase === GOOGLE_BOOKS_ACCESS_CASE.NO_ACCESS && (
          <Box sx={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2, textAlign: "center" }}>
            <Typography variant="body1" color="text.secondary">
              Cuốn sách này hiện không có nội dung đọc trực tuyến.
            </Typography>
            {access?.webReaderLink && (
              <Button variant="outlined" startIcon={<OpenInNewIcon />} onClick={() => window.open(access.webReaderLink, "_blank", "noopener,noreferrer")} sx={{ borderRadius: 2, textTransform: "none" }}>
                Xem trên Google Books
              </Button>
            )}
          </Box>
        )}

        {accessCase === GOOGLE_BOOKS_ACCESS_CASE.UNKNOWN && (
          <Box sx={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2, textAlign: "center" }}>
            <Typography variant="body1" color="text.secondary">
              Chưa xác định được quyền đọc cho cuốn sách này.
            </Typography>
            {access?.webReaderLink && (
              <Button variant="outlined" startIcon={<OpenInNewIcon />} onClick={() => window.open(access.webReaderLink, "_blank", "noopener,noreferrer")} sx={{ borderRadius: 2, textTransform: "none" }}>
                Xem trên Google Books
              </Button>
            )}
          </Box>
        )}
      </Box>

      {/* Footer: Previous / Progress / Next */}
      <Box
        component="footer"
        sx={{
          minHeight: 64,
          px: { xs: 2, sm: 4 },
          py: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 2,
          bgcolor: "background.paper",
          borderTop: "1px solid rgba(0,0,0,0.08)",
          position: "sticky",
          bottom: 0,
          overflowX: "auto",
        }}
      >
        <Tooltip title="Điều hướng trang do khung xem Google Books tự quản lý bên trong">
          <span>
            <Button size="small" variant="outlined" startIcon={<NavigateBeforeIcon />} disabled sx={{ borderRadius: 2, textTransform: "none", flexShrink: 0 }}>
              Trang trước
            </Button>
          </span>
        </Tooltip>

        <Box sx={{ flex: 1, maxWidth: 400, mx: "auto" }}>
          {progress && progress.progressPercent > 0 ? (
            <LinearProgress variant="determinate" value={progress.progressPercent} sx={{ height: 6, borderRadius: 3 }} />
          ) : (
            <Typography variant="caption" color="text.disabled" sx={{ display: "block", textAlign: "center" }}>
              Chưa có dữ liệu tiến độ
            </Typography>
          )}
        </Box>

        <Tooltip title="Điều hướng trang do khung xem Google Books tự quản lý bên trong">
          <span>
            <Button size="small" variant="outlined" endIcon={<NavigateNextIcon />} disabled sx={{ borderRadius: 2, textTransform: "none", flexShrink: 0 }}>
              Trang sau
            </Button>
          </span>
        </Tooltip>
      </Box>
    </Box>
  );
}
