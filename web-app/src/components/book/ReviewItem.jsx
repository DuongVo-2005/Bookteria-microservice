import React, { useState } from "react";
import {
  Box,
  Typography,
  Avatar,
  Rating,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  Chip,
} from "@mui/material";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import DeleteIcon from "@mui/icons-material/Delete";
import PersonIcon from "@mui/icons-material/Person";
import FlagOutlinedIcon from "@mui/icons-material/FlagOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import ThumbUpOutlinedIcon from "@mui/icons-material/ThumbUpOutlined";
import ThumbUpIcon from "@mui/icons-material/ThumbUp";
import { toggleReviewHelpful } from "../../services/reviewService";

export const ReviewItem = ({ review, onEdit, onDelete, onReport }) => {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  // BA Backlog FEAT-03 (be-report.md, bổ sung 2026-09-19): BE luôn trả content
  // đầy đủ, ẩn/hiện spoiler hoàn toàn ở FE.
  const [spoilerRevealed, setSpoilerRevealed] = useState(false);
  const [helpfulByMe, setHelpfulByMe] = useState(review.helpfulByMe || false);
  const [helpfulCount, setHelpfulCount] = useState(review.helpfulCount || 0);
  const [togglingHelpful, setTogglingHelpful] = useState(false);

  const handleDeleteConfirm = () => {
    setDeleteDialogOpen(false);
    if (onDelete) {
      onDelete(review.id);
    }
  };

  const handleToggleHelpful = () => {
    const nextValue = !helpfulByMe;
    setHelpfulByMe(nextValue);
    setHelpfulCount((c) => Math.max(0, nextValue ? c + 1 : c - 1));
    setTogglingHelpful(true);
    toggleReviewHelpful(review.id)
      .catch(() => {
        setHelpfulByMe(!nextValue);
        setHelpfulCount((c) => Math.max(0, nextValue ? c - 1 : c + 1));
      })
      .finally(() => setTogglingHelpful(false));
  };

  return (
    <>
      <Box
        sx={{
          p: 2,
          mb: 2,
          borderRadius: 2,
          bgcolor: review.isCurrentUser ? "rgba(25, 118, 210, 0.04)" : "transparent",
          border: review.isCurrentUser ? "1px solid rgba(25, 118, 210, 0.2)" : "1px solid rgba(0, 0, 0, 0.06)",
        }}
      >
        <Box sx={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", mb: 1 }}>
          <Box sx={{ display: "flex", alignItems: "center" }}>
            <Avatar src={review.avatar || undefined} sx={{ width: 36, height: 36, bgcolor: review.isCurrentUser ? "primary.main" : "action.selected", color: review.isCurrentUser ? "#fff" : "text.secondary", mr: 1.5 }}>
              <PersonIcon fontSize="small" />
            </Avatar>
            <Box>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                  {review.isCurrentUser ? "Đánh giá của bạn" : review.username || "Người dùng ẩn danh"}
                </Typography>
                {review.isCurrentUser && (
                  <Chip label="Bạn" size="small" color="primary" sx={{ height: 20, fontSize: "0.7rem" }} />
                )}
              </Box>
              <Typography variant="caption" color="text.secondary">
                {review.createdAt}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: "flex", gap: 0.5 }}>
            {review.isCurrentUser && onEdit && (
              <IconButton size="small" onClick={() => onEdit(review)} aria-label="Sửa đánh giá" color="primary">
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            )}
            {onDelete && (
              <IconButton size="small" onClick={() => setDeleteDialogOpen(true)} aria-label="Xoá đánh giá" color="error">
                <DeleteIcon fontSize="small" />
              </IconButton>
            )}
            {!review.isCurrentUser && onReport && (
              <IconButton size="small" onClick={() => onReport(review)} aria-label="Báo cáo đánh giá">
                <FlagOutlinedIcon fontSize="small" />
              </IconButton>
            )}
          </Box>
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
          <Rating value={review.rating} precision={0.5} size="small" readOnly />
        </Box>

        {review.hasSpoiler && !spoilerRevealed ? (
          <Box sx={{ position: "relative", borderRadius: 1.5, overflow: "hidden" }}>
            <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6, whiteSpace: "pre-line", filter: "blur(6px)", userSelect: "none" }}>
              {review.content || "(Không có nội dung)"}
            </Typography>
            <Box sx={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", bgcolor: "rgba(255,255,255,0.4)" }}>
              <Button size="small" variant="contained" color="inherit" startIcon={<VisibilityOffOutlinedIcon fontSize="small" />} onClick={() => setSpoilerRevealed(true)} sx={{ fontWeight: 600, textTransform: "none", bgcolor: "#fff", boxShadow: 2 }}>
                Xem nội dung tiết lộ
              </Button>
            </Box>
          </Box>
        ) : review.content ? (
          <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6, whiteSpace: "pre-line" }}>
            {review.content}
          </Typography>
        ) : (
          <Typography variant="caption" color="text.secondary" sx={{ fontStyle: "italic" }}>
            (Người dùng không để lại nhận xét bằng lời)
          </Typography>
        )}

        <Button
          size="small"
          startIcon={helpfulByMe ? <ThumbUpIcon fontSize="small" /> : <ThumbUpOutlinedIcon fontSize="small" />}
          onClick={handleToggleHelpful}
          disabled={togglingHelpful}
          sx={{ mt: 1, p: 0, minWidth: 0, fontSize: "0.75rem", fontWeight: 600, color: helpfulByMe ? "primary.main" : "text.secondary", textTransform: "none", "&:hover": { bgcolor: "transparent" } }}
        >
          {helpfulCount > 0 ? `Hữu ích (${helpfulCount})` : "Hữu ích"}
        </Button>
      </Box>

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle sx={{ fontWeight: 600 }}>Xác nhận xoá đánh giá</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Bạn có chắc muốn xoá đánh giá này? Thao tác này không thể hoàn tác.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit">
            Huỷ
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            Xoá đánh giá
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default ReviewItem;
