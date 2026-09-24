import React, { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Typography, Box } from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";

export const MarkCompletedDialog = ({ open, item, onClose, onConfirm }) => {
  const totalPages = item?.book.metadata?.pageCount || 1;
  const [page, setPage] = useState(totalPages);

  useEffect(() => {
    if (item) {
      setPage(item.book.metadata?.pageCount || 1);
    }
  }, [item]);

  if (!item) return null;

  const handleConfirm = () => {
    onConfirm(item.id, page);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1, fontWeight: 600 }}>
        <CheckCircleIcon color="success" />
        Đánh dấu đã đọc xong
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Bạn sắp hoàn thành cuốn <strong>"{item.book.title}"</strong>.
        </Typography>

        <Box sx={{ mt: 1 }}>
          <TextField
            fullWidth
            type="number"
            label="Số trang đã đọc"
            value={page}
            onChange={(e) => setPage(parseInt(e.target.value, 10) || 0)}
            helperText={`Tổng số trang của sách: ${totalPages} trang`}
            inputProps={{ min: 1, max: totalPages }}
            size="small"
          />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          Huỷ
        </Button>
        <Button onClick={handleConfirm} variant="contained" color="success">
          Xác nhận đã đọc
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default MarkCompletedDialog;
