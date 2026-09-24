import React from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, Avatar, Box, Typography } from "@mui/material";

export const LeaveGroupDialog = ({ open, group, onClose, onConfirm }) => {
  if (!group) return null;

  const handleConfirm = () => {
    onConfirm(group.id);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 420 } } }}>
      <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>Rời khỏi nhóm?</DialogTitle>
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
          Bạn có chắc chắn muốn rời khỏi nhóm <strong>{group.name}</strong>? Bạn sẽ không còn nhận được các thông báo thảo luận và chia sẻ sách từ hội nhóm này.
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" sx={{ fontWeight: 600 }}>
          Ở lại nhóm
        </Button>
        <Button onClick={handleConfirm} variant="contained" color="error" sx={{ fontWeight: 700, px: 2.5 }}>
          Rời nhóm
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default LeaveGroupDialog;
