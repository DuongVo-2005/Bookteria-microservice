import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, CircularProgress, TextField, Typography } from "@mui/material";
import { deactivateUser } from "../../services/adminUserService";
import { getIdentityErrorMessage } from "../../services/identityErrorMessages";
import { useToast } from "../../context/ToastContext";

// BA v2 Phase 2 §2.2 (be-report.md, bổ sung 2026-09-19) — "soft delete" thật
// (anonymize tên/thông tin cá nhân qua profile-service), KHÔNG có cơ chế
// khôi phục (khác unlockUser) — đúng như BE tự ghi trong Known Issues. Vì
// không thể hoàn tác, bắt gõ lại username để xác nhận (khác các dialog xác
// nhận khác trong dự án chỉ cần bấm nút, do mức độ nghiêm trọng cao hơn hẳn).
export const DeactivateAccountDialog = ({ open, targetUserId, targetUsername, onClose }) => {
  const { showSuccess, showError } = useToast();
  const [confirmText, setConfirmText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleClose = () => {
    if (submitting) return;
    setConfirmText("");
    onClose();
  };

  const isConfirmed = targetUsername && confirmText.trim() === targetUsername;

  const handleConfirm = () => {
    if (!isConfirmed) return;
    setSubmitting(true);
    deactivateUser(targetUserId)
      .then(() => {
        showSuccess(`Đã vô hiệu hoá tài khoản ${targetUsername}.`);
        handleClose();
      })
      .catch((err) => showError(getIdentityErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} slotProps={{ paper: { sx: { borderRadius: 3, p: 1, maxWidth: 440 } } }}>
      <DialogTitle sx={{ fontWeight: 700, color: "error.main" }}>Vô hiệu hoá tài khoản</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ fontSize: "0.9rem", color: "text.secondary", mb: 1.5 }}>
          Tài khoản <strong>{targetUsername || targetUserId}</strong> sẽ bị đăng xuất ngay, không thể đăng nhập lại, và thông tin cá nhân (tên, ảnh...) sẽ được ẩn danh thành "Người dùng đã xoá".
        </DialogContentText>
        <Typography variant="caption" sx={{ display: "block", fontWeight: 700, color: "error.main", mb: 1.5 }}>
          ⚠️ Hành động này KHÔNG THỂ hoàn tác — không có tính năng khôi phục lại tài khoản.
        </Typography>
        <TextField
          fullWidth
          size="small"
          label={`Gõ "${targetUsername || ""}" để xác nhận`}
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          autoFocus
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} disabled={submitting} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" color="error" onClick={handleConfirm} disabled={submitting || !isConfirmed} sx={{ textTransform: "none", fontWeight: 700 }}>
          {submitting ? <CircularProgress size={18} color="inherit" /> : "Vô hiệu hoá vĩnh viễn"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default DeactivateAccountDialog;
