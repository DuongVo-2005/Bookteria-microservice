import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, CircularProgress } from "@mui/material";
import { resetUserPassword } from "../../services/adminUserService";
import { getIdentityErrorMessage } from "../../services/identityErrorMessages";
import { useToast } from "../../context/ToastContext";

// BA v2 Phase 2 §2.2 (be-report.md, bổ sung 2026-09-19) — không nhận lại mật
// khẩu mới trong response (BE chỉ gửi qua email), nên dialog này chỉ xác
// nhận hành động rồi hiện thông báo thành công, không hiển thị mật khẩu nào.
export const ResetPasswordDialog = ({ open, targetUserId, targetUsername, onClose }) => {
  const { showSuccess, showError } = useToast();
  const [submitting, setSubmitting] = useState(false);

  const handleClose = () => {
    if (submitting) return;
    onClose();
  };

  const handleConfirm = () => {
    setSubmitting(true);
    resetUserPassword(targetUserId)
      .then(() => {
        showSuccess(`Đã gửi mật khẩu tạm thời tới email của ${targetUsername || "người dùng này"}.`);
        onClose();
      })
      .catch((err) => showError(getIdentityErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 420 } } }}>
      <DialogTitle sx={{ fontWeight: 700 }}>Đặt lại mật khẩu</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ fontSize: "0.9rem", color: "text.secondary" }}>
          Hệ thống sẽ tạo mật khẩu tạm thời mới và gửi qua email cho <strong>{targetUsername || targetUserId}</strong>. Mật khẩu cũ sẽ không còn dùng được nữa.
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} disabled={submitting} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" onClick={handleConfirm} disabled={submitting} sx={{ textTransform: "none", fontWeight: 700 }}>
          {submitting ? <CircularProgress size={18} color="inherit" /> : "Gửi mật khẩu mới"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ResetPasswordDialog;
