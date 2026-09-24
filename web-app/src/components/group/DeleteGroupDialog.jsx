import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, Avatar, Box, Typography, CircularProgress } from "@mui/material";

// Phase 4 (be-report.md, 2026-09-18): xoá nhóm là thao tác không thể hoàn
// tác — xoá cascade toàn bộ thành viên/bài đăng/bình luận/lượt thích trong
// nhóm ở phía BE, nên bắt buộc xác nhận riêng (khác Rời nhóm, không cascade).
export const DeleteGroupDialog = ({ open, group, onClose, onConfirm }) => {
  const [deleting, setDeleting] = useState(false);
  if (!group) return null;

  const handleConfirm = () => {
    setDeleting(true);
    Promise.resolve(onConfirm(group.id))
      .then(() => onClose())
      .finally(() => setDeleting(false));
  };

  return (
    <Dialog open={open} onClose={() => !deleting && onClose()} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 440 } } }}>
      <DialogTitle sx={{ fontWeight: 700, pb: 1, color: "error.main" }}>Xoá hội nhóm?</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, my: 1 }}>
          <Avatar src={group.avatar} alt={group.name} variant="rounded" sx={{ width: 52, height: 52, borderRadius: 2 }} />
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
              {group.name}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {group.memberCount} thành viên • {group.category}
            </Typography>
          </Box>
        </Box>
        <DialogContentText sx={{ mt: 1.5, fontSize: "0.9rem" }}>
          Toàn bộ thành viên, bài đăng, bình luận và lượt thích trong nhóm <strong>{group.name}</strong> sẽ bị xoá vĩnh viễn. Thao tác này{" "}
          <strong>không thể hoàn tác</strong>.
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={deleting} sx={{ fontWeight: 600 }}>
          Huỷ
        </Button>
        <Button onClick={handleConfirm} variant="contained" color="error" disabled={deleting} sx={{ fontWeight: 700, px: 2.5 }}>
          {deleting ? <CircularProgress size={18} color="inherit" /> : "Xoá vĩnh viễn"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default DeleteGroupDialog;
