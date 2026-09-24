import React, { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Typography, Button, CircularProgress, FormGroup, FormControlLabel, Checkbox, Box } from "@mui/material";
import { getIdentityUser, updateUserPermissions } from "../../services/adminUserService";
import { getIdentityErrorMessage } from "../../services/identityErrorMessages";
import { useToast } from "../../context/ToastContext";

// BA v2 Phase 1 P1-01 + P1-02 (be-report.md, bổ sung 2026-09-19) — danh sách
// permission cố định đã seed sẵn (Flyway V6), không có endpoint liệt kê động
// nên hardcode đúng 9 permission đã xác nhận trong report.
const PERMISSION_GROUPS = [
  {
    label: "Sách",
    permissions: [
      { value: "book:create", label: "Tạo sách" },
      { value: "book:update", label: "Sửa sách" },
      { value: "book:delete", label: "Xoá sách" },
      { value: "book:import", label: "Nhập sách (Google Books)" },
    ],
  },
  {
    label: "Nội dung đọc",
    permissions: [{ value: "reading:import_content", label: "Nạp nội dung sách (ePub/PDF)" }],
  },
  {
    label: "Đánh giá",
    permissions: [{ value: "review:lock", label: "Khoá/mở khoá đánh giá sách" }],
  },
  {
    label: "Danh mục",
    permissions: [
      { value: "author:manage", label: "Quản lý tác giả" },
      { value: "category:manage", label: "Quản lý thể loại" },
      { value: "publisher:manage", label: "Quản lý nhà xuất bản" },
    ],
  },
];

// Chỉ quản lý permission gán TRỰC TIẾP cho user (khác với permission suy ra từ
// Role) — PATCH /identity/users/{userId}/permissions THAY THẾ toàn bộ danh
// sách, không phải thêm/bớt từng cái, nên luôn gửi lại đầy đủ danh sách hiện
// đang chọn trong dialog, không gửi phần chênh lệch.
export const ManagePermissionsDialog = ({ open, targetUserId, targetUsername, onClose }) => {
  const { showSuccess, showError } = useToast();
  const [selected, setSelected] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    if (!open || !targetUserId) return;
    setLoading(true);
    setLoadError(null);
    getIdentityUser(targetUserId)
      .then((response) => setSelected(response.data?.result?.permissions || []))
      .catch((err) => setLoadError(getIdentityErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [open, targetUserId]);

  const handleClose = () => {
    if (submitting) return;
    onClose();
  };

  const togglePermission = (value) => {
    setSelected((prev) => (prev.includes(value) ? prev.filter((p) => p !== value) : [...prev, value]));
  };

  const handleSubmit = () => {
    setSubmitting(true);
    updateUserPermissions(targetUserId, selected)
      .then(() => {
        showSuccess(`Đã cập nhật quyền cho ${targetUsername || "người dùng này"}.`);
        handleClose();
      })
      .catch((err) => showError(getIdentityErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ fontWeight: 700 }}>Quản lý quyền</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Permission cấp trực tiếp cho <strong>{targetUsername || targetUserId}</strong>, cộng dồn với permission suy ra từ Role đã gán (không thay thế Role).
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
            <CircularProgress size={26} />
          </Box>
        ) : loadError ? (
          <Typography variant="body2" color="error.main">
            {loadError}
          </Typography>
        ) : (
          PERMISSION_GROUPS.map((group) => (
            <Box key={group.label} sx={{ mb: 1.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                {group.label}
              </Typography>
              <FormGroup>
                {group.permissions.map((perm) => (
                  <FormControlLabel
                    key={perm.value}
                    control={<Checkbox size="small" checked={selected.includes(perm.value)} onChange={() => togglePermission(perm.value)} />}
                    label={perm.label}
                  />
                ))}
              </FormGroup>
            </Box>
          ))
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} disabled={submitting} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={submitting || loading || Boolean(loadError)} sx={{ textTransform: "none", fontWeight: 700 }}>
          {submitting ? <CircularProgress size={18} color="inherit" /> : "Lưu"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ManagePermissionsDialog;
