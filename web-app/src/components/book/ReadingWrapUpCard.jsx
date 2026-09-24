import React, { useState, useEffect } from "react";
import { Card, Box, Typography, Chip, CircularProgress } from "@mui/material";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import { getMyReadingWrapUp } from "../../services/readingListService";

// BA v2 Phase 3 §4.2 (be-report.md, bổ sung 2026-09-22) — Reading Wrap-up.
// BE chỉ mô tả response bằng lời ("số sách hoàn thành + top thể loại/tác giả
// trong khoảng thời gian"), KHÔNG liệt kê field JSON cụ thể — đọc phòng thủ
// nhiều tên field khả dĩ thay vì tự đoán 1 shape duy nhất, không hiển thị gì
// nếu không tìm được field nào khớp (tránh hiện "0"/"undefined" sai lệch).
export const ReadingWrapUpCard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMyReadingWrapUp()
      .then((response) => setData(response?.data?.result || null))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Card sx={{ p: 3, mb: 3, borderRadius: 3, display: "flex", justifyContent: "center" }}>
        <CircularProgress size={24} />
      </Card>
    );
  }

  const booksCompleted = data?.booksCompleted ?? data?.completedCount ?? data?.totalCompleted ?? data?.completedBooks;
  const topCategories = data?.topCategories || data?.topGenres || [];
  const topAuthors = data?.topAuthors || [];

  if (!data || (booksCompleted === undefined && topCategories.length === 0 && topAuthors.length === 0)) {
    return null;
  }

  const chipLabel = (entry) => {
    if (typeof entry === "string") return entry;
    const name = entry?.name || entry?.category || entry?.author || entry?.authorName || entry?.label;
    const count = entry?.count ?? entry?.total;
    if (!name) return null;
    return count !== undefined ? `${name} (${count})` : name;
  };

  return (
    <Card sx={{ p: 3, mb: 3, borderRadius: 3, background: "linear-gradient(135deg, rgba(25,118,210,0.06), rgba(156,39,176,0.06))" }}>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
        <AutoAwesomeIcon color="primary" />
        <Typography variant="h6" sx={{ fontWeight: 700 }}>
          Tổng kết đọc sách
        </Typography>
      </Box>

      {booksCompleted !== undefined && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
          <MenuBookIcon sx={{ color: "primary.main" }} />
          <Typography variant="body1">
            Bạn đã hoàn thành <strong>{booksCompleted}</strong> cuốn sách.
          </Typography>
        </Box>
      )}

      {topCategories.length > 0 && (
        <Box sx={{ mb: 1.5 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5, fontWeight: 600 }}>
            Thể loại đọc nhiều nhất
          </Typography>
          <Box sx={{ display: "flex", gap: 0.75, flexWrap: "wrap" }}>
            {topCategories.map((c, idx) => {
              const label = chipLabel(c);
              return label ? <Chip key={idx} size="small" label={label} color="primary" variant="outlined" /> : null;
            })}
          </Box>
        </Box>
      )}

      {topAuthors.length > 0 && (
        <Box>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5, fontWeight: 600 }}>
            Tác giả đọc nhiều nhất
          </Typography>
          <Box sx={{ display: "flex", gap: 0.75, flexWrap: "wrap" }}>
            {topAuthors.map((a, idx) => {
              const label = chipLabel(a);
              return label ? <Chip key={idx} size="small" label={label} color="secondary" variant="outlined" /> : null;
            })}
          </Box>
        </Box>
      )}
    </Card>
  );
};

export default ReadingWrapUpCard;
