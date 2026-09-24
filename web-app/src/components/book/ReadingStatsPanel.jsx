import React, { useState, useEffect } from "react";
import { Box, Paper, Typography, Tooltip, CircularProgress, Chip, LinearProgress, TextField, Button } from "@mui/material";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import LocalFireDepartmentIcon from "@mui/icons-material/LocalFireDepartment";
import EmojiEventsIcon from "@mui/icons-material/EmojiEvents";
import { getMyReadingStats } from "../../services/readingListService";
import { getReadingStreak, getReadingChallenge, setReadingChallenge } from "../../services/gamificationService";
import { getBookErrorMessage } from "../../services/errorMessages";

const CURRENT_YEAR = new Date().getFullYear();

const BADGE_LABELS = { "7_DAYS": "🔥 7 ngày", "30_DAYS": "🔥 30 ngày", "100_DAYS": "🔥 100 ngày" };

// §17 Reading Statistics (be-report.md Phase 5, 2026-09-18) — GET
// /book/me/reading-list/stats. completedByMonth luôn đúng 12 phần tử, không
// cần FE tự lấp khoảng trống. Biểu đồ cột tự vẽ bằng CSS (không có thư viện
// chart nào trong dependencies hiện tại — dự án chưa từng dùng recharts/
// chart.js, giữ nhất quán phong cách "tự dựng bằng MUI primitives" đã áp
// dụng cho LinearProgress ở nơi khác thay vì thêm dependency mới cho 12 cột).
export const ReadingStatsPanel = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // FEAT-02 (be-report.md, bổ sung 2026-09-19) — tải riêng, không chặn phần
  // thống kê chính nếu 1 trong 2 API này chậm/lỗi.
  const [streak, setStreak] = useState(null);
  const [challenge, setChallenge] = useState(null);
  const [targetInput, setTargetInput] = useState("");
  const [savingTarget, setSavingTarget] = useState(false);

  useEffect(() => {
    getMyReadingStats()
      .then((res) => setStats(res?.data?.result || null))
      .catch((err) => setError(getBookErrorMessage(err)))
      .finally(() => setLoading(false));

    getReadingStreak()
      .then((res) => setStreak(res?.data?.result || null))
      .catch(() => setStreak(null));

    getReadingChallenge(CURRENT_YEAR)
      .then((res) => setChallenge(res?.data?.result || null))
      .catch(() => setChallenge(null));
  }, []);

  const handleSetTarget = () => {
    const target = parseInt(targetInput, 10);
    if (!target || target <= 0) return;
    setSavingTarget(true);
    setReadingChallenge(CURRENT_YEAR, target)
      .then((res) => setChallenge(res?.data?.result || { year: CURRENT_YEAR, targetBooks: target, completedBooks: 0, percent: 0 }))
      .catch(() => {})
      .finally(() => setSavingTarget(false));
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (error || !stats) return null;

  const monthLabel = (yyyyMM) => {
    const [, m] = yyyyMM.split("-");
    return `T${parseInt(m, 10)}`;
  };

  const maxCount = Math.max(1, ...stats.completedByMonth.map((m) => m.count));

  return (
    <Paper elevation={0} sx={{ p: { xs: 2, sm: 3 }, mb: 3, borderRadius: 3, border: "1px solid rgba(0,0,0,0.06)" }}>
      <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
        Thống kê đọc sách
      </Typography>

      <Box sx={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 1.5, mb: 3 }}>
        <Box sx={{ textAlign: "center" }}>
          <AutoStoriesIcon color="primary" sx={{ mb: 0.3 }} />
          <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
            {stats.booksReading}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Đang đọc
          </Typography>
        </Box>
        <Box sx={{ textAlign: "center" }}>
          <CheckCircleIcon color="success" sx={{ mb: 0.3 }} />
          <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
            {stats.booksCompleted}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Đã đọc xong
          </Typography>
        </Box>
        <Box sx={{ textAlign: "center" }}>
          <BookmarkBorderIcon color="action" sx={{ mb: 0.3 }} />
          <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
            {stats.booksWantToRead}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Muốn đọc
          </Typography>
        </Box>
        <Box sx={{ textAlign: "center" }}>
          <MenuBookIcon color="disabled" sx={{ mb: 0.3 }} />
          <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
            {stats.totalBooks}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Tổng cộng
          </Typography>
        </Box>
      </Box>

      <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
        Sách hoàn thành theo tháng (12 tháng gần nhất)
      </Typography>
      <Box sx={{ display: "flex", alignItems: "flex-end", gap: { xs: 0.5, sm: 1 }, height: 90 }}>
        {stats.completedByMonth.map((m) => (
          <Tooltip key={m.month} title={`${m.month}: ${m.count} cuốn`}>
            <Box sx={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", height: "100%", justifyContent: "flex-end" }}>
              <Box
                sx={{
                  width: "100%",
                  maxWidth: 22,
                  height: `${Math.max(4, (m.count / maxCount) * 64)}px`,
                  bgcolor: m.count > 0 ? "primary.main" : "rgba(0,0,0,0.08)",
                  borderRadius: "3px 3px 0 0",
                  transition: "height 0.2s",
                }}
              />
              <Typography variant="caption" color="text.disabled" sx={{ fontSize: "0.62rem", mt: 0.5 }}>
                {monthLabel(m.month)}
              </Typography>
            </Box>
          </Tooltip>
        ))}
      </Box>

      {(streak || challenge) && (
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 2, mt: 3, pt: 3, borderTop: "1px solid rgba(0,0,0,0.06)" }}>
          {streak && (
            <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "rgba(255, 87, 34, 0.05)" }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.8, mb: 0.5 }}>
                <LocalFireDepartmentIcon sx={{ color: "#f4511e", fontSize: 20 }} />
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                  Chuỗi ngày đọc: {streak.currentStreak || 0} ngày
                </Typography>
              </Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: streak.badges?.length ? 0.8 : 0 }}>
                Kỷ lục: {streak.longestStreak || 0} ngày
              </Typography>
              {streak.badges?.length > 0 && (
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.6 }}>
                  {streak.badges.map((b) => (
                    <Chip key={b} label={BADGE_LABELS[b] || b} size="small" sx={{ height: 22, fontSize: "0.7rem", fontWeight: 600 }} />
                  ))}
                </Box>
              )}
            </Box>
          )}

          <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "rgba(25, 118, 210, 0.04)" }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.8, mb: 1 }}>
              <EmojiEventsIcon sx={{ color: "primary.main", fontSize: 20 }} />
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                Thử thách đọc sách {CURRENT_YEAR}
              </Typography>
            </Box>
            {challenge?.targetBooks > 0 ? (
              <>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 0.5 }}>
                  Đã đọc {challenge.completedBooks}/{challenge.targetBooks} cuốn ({challenge.percent}%)
                </Typography>
                <LinearProgress variant="determinate" value={Math.min(100, challenge.percent || 0)} sx={{ height: 6, borderRadius: 3 }} />
              </>
            ) : (
              <Box sx={{ display: "flex", gap: 1 }}>
                <TextField size="small" type="number" placeholder="VD: 24" value={targetInput} onChange={(e) => setTargetInput(e.target.value)} sx={{ maxWidth: 100 }} slotProps={{ htmlInput: { min: 1 } }} />
                <Button size="small" variant="contained" onClick={handleSetTarget} disabled={savingTarget || !targetInput} sx={{ textTransform: "none", fontWeight: 700 }}>
                  {savingTarget ? <CircularProgress size={16} color="inherit" /> : "Đặt mục tiêu"}
                </Button>
              </Box>
            )}
          </Box>
        </Box>
      )}
    </Paper>
  );
};

export default ReadingStatsPanel;
