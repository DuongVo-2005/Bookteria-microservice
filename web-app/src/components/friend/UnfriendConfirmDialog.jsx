import React from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, Avatar, Box, Typography } from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";

export const UnfriendConfirmDialog = ({ open, friend, onClose, onConfirm }) => {
  if (!friend) return null;

  const handleConfirm = () => {
    onConfirm(friend.userId);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 420 } } }}>
      <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>Xác nhận xoá kết bạn</DialogTitle>
      <DialogContent>
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, my: 1 }}>
          <Avatar src={friend.avatar || undefined} alt={friend.username} sx={{ width: 52, height: 52, bgcolor: "primary.light" }}>
            <PersonIcon />
          </Avatar>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              {friend.username}
            </Typography>
          </Box>
        </Box>
        <DialogContentText sx={{ mt: 1.5, fontSize: "0.9rem", color: "text.secondary" }}>
          Bạn có chắc chắn muốn hủy kết bạn với <strong>{friend.username}</strong>? Hành động này không thể hoàn tác trực tiếp trên giao diện (nếu muốn kết nối lại sẽ phải gửi lời mời mới từ đầu).
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" sx={{ fontWeight: 600, textTransform: "none" }}>
          Giữ lại
        </Button>
        <Button onClick={handleConfirm} variant="contained" color="error" sx={{ fontWeight: 700, px: 2.5, textTransform: "none" }}>
          Xoá kết bạn
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default UnfriendConfirmDialog;
