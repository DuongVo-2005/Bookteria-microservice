import React, { useState } from "react";
import { Paper, Box, Avatar, Typography, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, CircularProgress, Chip } from "@mui/material";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import { useGroup } from "../../context/GroupContext";

// OPS-01 (be-report.md, bổ sung 2026-09-19) — bài viết trong hàng chờ duyệt
// của nhóm bật requireApproval. Không có Thích/Bình luận (chưa publish).
export const PendingPostCard = ({ post, onDecided }) => {
  const { approvePost, rejectPost } = useGroup();
  const [processing, setProcessing] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const handleApprove = () => {
    setProcessing(true);
    approvePost(post.groupId, post.id)
      .then(() => onDecided?.(post.id))
      .catch(() => {})
      .finally(() => setProcessing(false));
  };

  const handleReject = () => {
    setProcessing(true);
    rejectPost(post.groupId, post.id, rejectReason)
      .then(() => {
        setRejectDialogOpen(false);
        setRejectReason("");
        onDecided?.(post.id);
      })
      .catch(() => {})
      .finally(() => setProcessing(false));
  };

  return (
    <Paper elevation={0} sx={{ p: 2.5, mb: 2.5, borderRadius: 3, border: "1px solid rgba(237, 108, 2, 0.25)", bgcolor: "rgba(237, 108, 2, 0.03)" }}>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5 }}>
        <Avatar src={post.author.avatar || undefined} alt={post.author.username} sx={{ width: 40, height: 40 }} />
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            {post.author.username}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {post.createdAt}
          </Typography>
        </Box>
        <Chip label="Đang chờ duyệt" size="small" color="warning" variant="outlined" sx={{ fontWeight: 600, fontSize: "0.7rem" }} />
      </Box>

      <Typography variant="body2" sx={{ fontSize: "0.9rem", lineHeight: 1.6, whiteSpace: "pre-line", mb: post.bookRef ? 1.5 : 2 }}>
        {post.content}
      </Typography>

      {post.bookRef && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2, p: 1, borderRadius: 2, bgcolor: "rgba(25, 118, 210, 0.05)" }}>
          <AutoStoriesIcon sx={{ fontSize: 16, color: "primary.main" }} />
          <Typography variant="caption" sx={{ fontWeight: 600 }}>
            {post.bookRef.bookTitle}
          </Typography>
        </Box>
      )}

      <Box sx={{ display: "flex", gap: 1.5 }}>
        <Button size="small" variant="contained" color="success" startIcon={processing ? <CircularProgress size={14} color="inherit" /> : <CheckCircleOutlineIcon />} onClick={handleApprove} disabled={processing} sx={{ textTransform: "none", fontWeight: 700 }}>
          Duyệt
        </Button>
        <Button size="small" variant="outlined" color="error" startIcon={<CancelOutlinedIcon />} onClick={() => setRejectDialogOpen(true)} disabled={processing} sx={{ textTransform: "none", fontWeight: 700 }}>
          Từ chối
        </Button>
      </Box>

      <Dialog open={rejectDialogOpen} onClose={() => !processing && setRejectDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 700 }}>Từ chối bài viết</DialogTitle>
        <DialogContent>
          <TextField fullWidth multiline minRows={2} maxRows={4} placeholder="Lý do (không bắt buộc)…" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} autoFocus />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setRejectDialogOpen(false)} disabled={processing} sx={{ textTransform: "none" }}>
            Huỷ
          </Button>
          <Button variant="contained" color="error" onClick={handleReject} disabled={processing} sx={{ textTransform: "none", fontWeight: 700 }}>
            {processing ? <CircularProgress size={16} color="inherit" /> : "Xác nhận từ chối"}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default PendingPostCard;
