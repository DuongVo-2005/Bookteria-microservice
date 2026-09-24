import React, { useState } from "react";
import { Paper, Box, Typography, LinearProgress } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { PlaceholderCover } from "./PlaceholderCover";

const STATUS_LABELS = {
  WANT_TO_READ: "Muốn đọc",
  READING: "Đang đọc",
  COMPLETED: "Đã đọc",
};

// Bản rút gọn, CHỈ ĐỌC của ShelfItem — dùng để xem Kệ sách của NGƯỜI KHÁC
// (BA v2 Phase 3 §4.3, be-report.md, 2026-09-22). Không có menu hành động
// (đổi trạng thái/xoá...) vì đó là dữ liệu của người khác, không phải của
// mình — khác hẳn ngữ cảnh ShelfItem (luôn là kệ sách của chính mình).
export const ReadOnlyShelfCard = ({ item }) => {
  const navigate = useNavigate();
  const [imgError, setImgError] = useState(false);
  const book = item.book;
  if (!book) return null;

  const mainAuthor = book.authors?.find((a) => a.role === "MAIN_AUTHOR")?.name || book.authors?.[0]?.name || "Tác giả";

  return (
    <Paper
      variant="outlined"
      onClick={() => navigate(`/books/${book.id}`)}
      sx={{ p: 1.5, borderRadius: 2.5, display: "flex", gap: 1.5, cursor: "pointer", "&:hover": { borderColor: "primary.main" } }}
    >
      <Box sx={{ width: 52, height: 76, flexShrink: 0, borderRadius: 1.5, overflow: "hidden", bgcolor: "#f5f5f5" }}>
        {book.metadata?.coverImage && !imgError ? (
          <Box component="img" src={book.metadata.coverImage} alt={book.title} onError={() => setImgError(true)} sx={{ width: "100%", height: "100%", objectFit: "cover" }} />
        ) : (
          <PlaceholderCover title={book.title} />
        )}
      </Box>
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: 700, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {book.title}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5 }}>
          {mainAuthor}
        </Typography>
        <Typography variant="caption" sx={{ fontWeight: 600, color: "primary.main" }}>
          {STATUS_LABELS[item.status] || item.status}
        </Typography>
        {item.status === "READING" && typeof item.progressPercent === "number" && (
          <LinearProgress variant="determinate" value={item.progressPercent} sx={{ height: 4, borderRadius: 2, mt: 0.75 }} />
        )}
      </Box>
    </Paper>
  );
};

export default ReadOnlyShelfCard;
