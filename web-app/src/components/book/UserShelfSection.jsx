import React, { useState, useEffect, useCallback } from "react";
import { Card, Box, Typography, Tabs, Tab, CircularProgress } from "@mui/material";
import { getUserReadingList, getBookById } from "../../services/bookService";
import { ReadOnlyShelfCard } from "./ReadOnlyShelfCard";

const TABS = [
  { value: "READING", label: "Đang đọc" },
  { value: "WANT_TO_READ", label: "Muốn đọc" },
  { value: "COMPLETED", label: "Đã đọc" },
];

// BA v2 Phase 3 §4.3 (be-report.md, bổ sung 2026-09-22) — xem Kệ sách của
// NGƯỜI KHÁC. FE Contract yêu cầu rõ: 403 (code 1048) → ẩn HẲN section này,
// không hiển thị lỗi gì cả (không phải mọi hồ sơ đều cho xem shelf).
export const UserShelfSection = ({ userId }) => {
  const [currentTab, setCurrentTab] = useState("READING");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [forbidden, setForbidden] = useState(false);

  const loadShelf = useCallback(
    (status) => {
      setLoading(true);
      getUserReadingList(userId, 0, 12, status)
        .then((response) => {
          const data = response?.data?.result?.data || [];
          const relevant = data.filter((entry) => entry.status !== "UNAVAILABLE");
          return Promise.all(
            relevant.map((entry) =>
              getBookById(entry.bookId)
                .then((bookResponse) => ({ ...entry, book: bookResponse.data.result }))
                .catch(() => null)
            )
          );
        })
        .then((merged) => setItems(merged.filter(Boolean)))
        .catch((err) => {
          if (err?.response?.status === 403) {
            setForbidden(true);
          }
          setItems([]);
        })
        .finally(() => setLoading(false));
    },
    [userId]
  );

  useEffect(() => {
    if (!forbidden) loadShelf(currentTab);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab, userId]);

  if (forbidden) return null;

  return (
    <Card sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3, mb: 3 }}>
      <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.secondary", textTransform: "uppercase", letterSpacing: 0.8, mb: 2 }}>
        Kệ sách
      </Typography>

      <Tabs value={currentTab} onChange={(_, v) => setCurrentTab(v)} sx={{ mb: 2, minHeight: 36, "& .MuiTab-root": { minHeight: 36, textTransform: "none", fontWeight: 600 } }}>
        {TABS.map((t) => (
          <Tab key={t.value} value={t.value} label={t.label} />
        ))}
      </Tabs>

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
          <CircularProgress size={26} />
        </Box>
      ) : items.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: "center" }}>
          Chưa có sách nào trong mục này.
        </Typography>
      ) : (
        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)" }, gap: 1.5 }}>
          {items.map((item) => (
            <ReadOnlyShelfCard key={item.id || item.bookId} item={item} />
          ))}
        </Box>
      )}
    </Card>
  );
};

export default UserShelfSection;
