import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Box, Typography, TextField, Button, CircularProgress, IconButton } from "@mui/material";
import FlagIcon from "@mui/icons-material/Flag";
import CloseIcon from "@mui/icons-material/Close";
import { createReport } from "../../services/reportService";
import { getReportErrorMessage } from "../../services/reportErrorMessages";
import { useToast } from "../../context/ToastContext";

// Dialog "Báo cáo vi phạm" dùng chung cho mọi loại nội dung (POST/COMMENT/
// USER/GROUP/REVIEW — đúng ReportTargetType be-report.md Phase 4 đã xác
// nhận). report-service không tự ẩn/xoá nội dung — chỉ tạo "phiếu ghi nhận"
// cho ADMIN xem xét sau, nên chỉ cần targetType/targetId/reason.
export const ReportDialog = ({ open, onClose, targetType, targetId, targetLabel }) => {
  const { showSuccess, showError } = useToast();
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleClose = () => {
    if (submitting) return;
    setReason("");
    onClose();
  };

  const handleSubmit = () => {
    if (!reason.trim()) return;
    setSubmitting(true);
    createReport(targetType, targetId, reason.trim())
      .then(() => {
        showSuccess("Đã gửi báo cáo. Cảm ơn bạn đã giúp Bookteria an toàn hơn.");
        setReason("");
        onClose();
      })
      .catch((err) => showError(getReportErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ fontWeight: 700, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <FlagIcon color="error" />
          Báo cáo vi phạm
        </Box>
        <IconButton size="small" onClick={handleClose} disabled={submitting}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>
      <DialogContent>
        {targetLabel && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Báo cáo: <strong>{targetLabel}</strong>
          </Typography>
        )}
        <TextField
          fullWidth
          multiline
          minRows={3}
          maxRows={6}
          autoFocus
          placeholder="Mô tả lý do bạn báo cáo nội dung này…"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          variant="outlined"
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} disabled={submitting} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" color="error" onClick={handleSubmit} disabled={submitting || !reason.trim()} sx={{ textTransform: "none", fontWeight: 700 }}>
          {submitting ? <CircularProgress size={18} color="inherit" /> : "Gửi báo cáo"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ReportDialog;
