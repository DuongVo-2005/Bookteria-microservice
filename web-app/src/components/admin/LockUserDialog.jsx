import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Typography, TextField, Button, CircularProgress, Select, MenuItem, FormControl, InputLabel } from "@mui/material";
import { lockUser } from "../../services/adminUserService";
import { getIdentityErrorMessage } from "../../services/identityErrorMessages";
import { useToast } from "../../context/ToastContext";

const DURATION_OPTIONS = [
  { value: "THREE_DAYS", label: "3 ngày" },
  { value: "SEVEN_DAYS", label: "7 ngày" },
  { value: "THIRTY_DAYS", label: "30 ngày" },
  { value: "PERMANENT", label: "Vĩnh viễn" },
];

// BA Backlog OPS-03 (be-report.md, bổ sung 2026-09-19). Không có API nào cho
// FE biết trạng thái khoá hiện tại của 1 user (profile-service không có field
// này, không có endpoint "get user by id" nào khác được xác nhận cho FE) —
// dialog này chỉ thực hiện hành động khoá, không hiển thị trạng thái hiện tại.
export const LockUserDialog = ({ open, targetUserId, targetUsername, onClose, onLocked }) => {
  const { showSuccess, showError } = useToast();
  const [reason, setReason] = useState("");
  const [duration, setDuration] = useState("SEVEN_DAYS");
  const [submitting, setSubmitting] = useState(false);

  const handleClose = () => {
    if (submitting) return;
    setReason("");
    setDuration("SEVEN_DAYS");
    onClose();
  };

  const handleSubmit = () => {
    if (!reason.trim()) return;
    setSubmitting(true);
    lockUser(targetUserId, reason.trim(), duration)
      .then(() => {
        showSuccess(`Đã khoá tài khoản ${targetUsername || ""}.`);
        onLocked?.();
        handleClose();
      })
      .catch((err) => showError(getIdentityErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="xs">
      <DialogTitle sx={{ fontWeight: 700, color: "error.main" }}>Khoá tài khoản</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Khoá tài khoản <strong>{targetUsername || targetUserId}</strong> — người dùng sẽ bị đăng xuất ngay và không thể đăng nhập lại cho tới khi hết hạn (hoặc được mở khoá thủ công).
        </Typography>
        <TextField fullWidth multiline minRows={2} maxRows={4} label="Lý do khoá" required value={reason} onChange={(e) => setReason(e.target.value)} sx={{ mb: 2 }} autoFocus />
        <FormControl fullWidth size="small">
          <InputLabel>Thời hạn</InputLabel>
          <Select label="Thời hạn" value={duration} onChange={(e) => setDuration(e.target.value)}>
            {DURATION_OPTIONS.map((opt) => (
              <MenuItem key={opt.value} value={opt.value}>
                {opt.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} disabled={submitting} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" color="error" onClick={handleSubmit} disabled={submitting || !reason.trim()} sx={{ textTransform: "none", fontWeight: 700 }}>
          {submitting ? <CircularProgress size={18} color="inherit" /> : "Khoá tài khoản"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default LockUserDialog;
