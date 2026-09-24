import React from "react";
import { Paper, Box, Typography, IconButton, Tooltip } from "@mui/material";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import DeleteIcon from "@mui/icons-material/Delete";

// BA Backlog GAP-02 (be-report.md, bổ sung 2026-09-18): ReadingList.status
// thêm giá trị UNAVAILABLE khi sách bị Admin xoá thật khỏi catalog. Entity
// ReadingList KHÔNG lưu snapshot title/cover/author (chỉ userId+bookId+
// status+tiến độ) — sách đã bị xoá nên KHÔNG thể gọi getBookById() để lấy
// lại thông tin (sẽ 404), khác hẳn ShelfItem.jsx (luôn có đủ item.book).
export const UnavailableShelfItem = ({ item, onRemove }) => {
  return (
    <Paper
      elevation={0}
      sx={{
        p: 2,
        mb: 2,
        borderRadius: 2,
        border: "1px dashed rgba(0,0,0,0.15)",
        bgcolor: "rgba(0,0,0,0.02)",
        display: "flex",
        alignItems: "center",
        gap: 2,
      }}
    >
      <Box sx={{ width: 60, height: 90, flexShrink: 0, borderRadius: 1, bgcolor: "action.disabledBackground", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <AutoStoriesIcon sx={{ color: "text.disabled", fontSize: 26 }} />
      </Box>

      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        <Typography variant="subtitle1" color="text.disabled" sx={{ fontWeight: 600, fontStyle: "italic" }}>
          Sách không còn tồn tại
        </Typography>
        <Typography variant="caption" color="text.disabled">
          Sách này đã bị gỡ khỏi hệ thống nhưng vẫn còn trong lịch sử kệ sách của bạn.
        </Typography>
        {item.progressPercent > 0 && (
          <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 0.3 }}>
            Tiến độ đã lưu trước đó: {item.progressPercent}%
          </Typography>
        )}
      </Box>

      <Tooltip title="Xoá khỏi kệ">
        <IconButton onClick={() => onRemove(item.id)} color="error" size="small">
          <DeleteIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Paper>
  );
};

export default UnavailableShelfItem;
