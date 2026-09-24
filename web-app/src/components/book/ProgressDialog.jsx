import React, { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Typography, Box, LinearProgress } from "@mui/material";

export const ProgressDialog = ({ open, item, onClose, onSave }) => {
  const [page, setPage] = useState(0);
  const totalPages = item?.book.metadata?.pageCount || 1;

  useEffect(() => {
    if (item) {
      setPage(item.currentPage || 0);
    }
  }, [item]);

  if (!item) return null;

  const handlePageChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (isNaN(val)) {
      setPage(0);
    } else {
      setPage(Math.min(Math.max(0, val), totalPages));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(item.id, page);
    onClose();
  };

  const progressPercent = Math.min(100, Math.round((page / totalPages) * 100));

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle sx={{ fontWeight: 600 }}>Cập nhật tiến độ đọc</DialogTitle>
        <DialogContent>
          <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
            {item.book.title}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 2 }}>
            Tổng số trang: {totalPages} trang
          </Typography>

          <TextField
            fullWidth
            type="number"
            label="Trang hiện tại"
            value={page}
            onChange={handlePageChange}
            inputProps={{ min: 0, max: totalPages }}
            autoFocus
            sx={{ mb: 2 }}
          />

          <Box sx={{ mb: 1 }}>
            <Box sx={{ display: "flex", justifyContent: "space-between", mb: 0.5 }}>
              <Typography variant="caption" color="text.secondary">
                Tiến độ: {progressPercent}%
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {page} / {totalPages} trang
              </Typography>
            </Box>
            <LinearProgress variant="determinate" value={progressPercent} sx={{ height: 8, borderRadius: 4 }} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit">
            Huỷ
          </Button>
          <Button type="submit" variant="contained" color="primary">
            Lưu tiến độ
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default ProgressDialog;
