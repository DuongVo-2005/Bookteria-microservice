import React, { useState, useEffect } from "react";
import { Card, Box, Typography, Select, MenuItem, CircularProgress } from "@mui/material";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { getMyShelfPrivacy, updateMyShelfPrivacy } from "../../services/bookService";
import { getBookErrorMessage } from "../../services/errorMessages";
import { useToast } from "../../context/ToastContext";

const PRIVACY_OPTIONS = [
  { value: "PUBLIC", label: "Công khai — ai cũng xem được" },
  { value: "FRIENDS_ONLY", label: "Chỉ bạn bè" },
  { value: "PRIVATE", label: "Riêng tư — chỉ mình tôi" },
];

// BA v2 Phase 3 §4.3 (be-report.md, bổ sung 2026-09-22) — thiếu document ở BE
// = mặc định PUBLIC (chưa từng cài đặt gì), GET luôn trả 200.
export const ShelfPrivacySettings = () => {
  const { showSuccess, showError } = useToast();
  const [privacy, setPrivacy] = useState("PUBLIC");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getMyShelfPrivacy()
      .then((response) => setPrivacy(response?.data?.result?.privacy || "PUBLIC"))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleChange = (e) => {
    const next = e.target.value;
    const previous = privacy;
    setPrivacy(next);
    setSaving(true);
    updateMyShelfPrivacy(next)
      .then(() => showSuccess("Đã cập nhật quyền riêng tư Kệ sách."))
      .catch((err) => {
        setPrivacy(previous);
        showError(getBookErrorMessage(err));
      })
      .finally(() => setSaving(false));
  };

  return (
    <Card sx={{ minWidth: 350, maxWidth: 500, boxShadow: 3, borderRadius: 2, padding: 3, mt: 3 }}>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5 }}>
        <VisibilityOutlinedIcon color="primary" />
        <Typography sx={{ fontWeight: 700 }}>Quyền riêng tư Kệ sách</Typography>
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Kiểm soát ai được xem Kệ sách và Lịch sử đọc của bạn khi ghé thăm hồ sơ.
      </Typography>

      {loading ? (
        <CircularProgress size={22} />
      ) : (
        <Select fullWidth size="small" value={privacy} onChange={handleChange} disabled={saving}>
          {PRIVACY_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </Select>
      )}
    </Card>
  );
};

export default ShelfPrivacySettings;
