import React from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, Avatar, Box, Typography } from "@mui/material";
import BlockIcon from "@mui/icons-material/Block";
import PersonIcon from "@mui/icons-material/Person";

export const BlockConfirmDialog = ({ open, user, onClose, onConfirm }) => {
  if (!user) return null;

  const handleConfirm = () => {
    onConfirm(user.userId);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 420 } } }}>
      <DialogTitle sx={{ fontWeight: 700, pb: 1, display: "flex", alignItems: "center", gap: 1, color: "error.main" }}>
        <BlockIcon />
        <span>Xác nhận chặn người dùng</span>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, my: 1.5 }}>
          <Avatar src={user.avatar || undefined} alt={user.username} sx={{ width: 52, height: 52, bgcolor: "error.light" }}>
            <PersonIcon />
          </Avatar>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              {user.username}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              User ID: {user.userId}
            </Typography>
          </Box>
        </Box>
        <DialogContentText sx={{ mt: 1.5, fontSize: "0.9rem", color: "text.secondary" }}>
          Bạn có chắc chắn muốn chặn <strong>{user.username}</strong>?
          <br />
          Hành động này sẽ <strong>tự động huỷ kết bạn và các lời mời hiện có</strong>. Nếu muốn kết nối lại sau này, bạn phải bỏ chặn và gửi lời mời kết bạn mới từ đầu.
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" sx={{ fontWeight: 600, textTransform: "none" }}>
          Huỷ
        </Button>
        <Button onClick={handleConfirm} variant="contained" color="error" startIcon={<BlockIcon />} sx={{ fontWeight: 700, px: 2.5, textTransform: "none" }}>
          Chặn người này
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default BlockConfirmDialog;
