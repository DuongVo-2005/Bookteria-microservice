import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Box, Typography, Alert, CircularProgress, IconButton } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import { importBookContent } from "../../services/readingService";
import { getReadingErrorMessage } from "../../services/readingErrorMessages";

// Admin nạp nội dung sách thật qua POST /reading/books/{bookId}/import (BE
// tự tách chương từ file ePub/PDF) — thay cho việc nhập tay từng chương.
// PDF không có outline/mục lục sẽ luôn ra đúng 1 chương (giới hạn đã biết ở
// BE, không phải lỗi FE) — hiện rõ qua field `warnings` BE trả về.
export const ImportBookContentDialog = ({ open, book, onClose }) => {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  const handleClose = () => {
    if (uploading) return;
    setFile(null);
    setError(null);
    setResult(null);
    onClose();
  };

  const handleFileChange = (e) => {
    setFile(e.target.files?.[0] || null);
    setError(null);
    setResult(null);
  };

  const handleUpload = () => {
    if (!file || !book) return;
    setUploading(true);
    setError(null);
    importBookContent(book.id, file)
      .then((response) => setResult(response?.data?.result))
      .catch((err) => setError(getReadingErrorMessage(err)))
      .finally(() => setUploading(false));
  };

  if (!book) return null;

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="xs" slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
      <DialogTitle sx={{ fontWeight: 700, display: "flex", justifyContent: "space-between", alignItems: "center", pb: 1 }}>
        <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
          Nạp nội dung sách
        </Typography>
        <IconButton onClick={handleClose} size="small" aria-label="Đóng" disabled={uploading}>
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Tải lên file <strong>.epub</strong> hoặc <strong>.pdf</strong> cho sách "<strong>{book.title}</strong>" — hệ thống sẽ tự tách chương theo mục lục thật trong file.
        </Typography>

        <Button variant="outlined" component="label" startIcon={<UploadFileIcon />} disabled={uploading} sx={{ borderRadius: 2, textTransform: "none", py: 1.2 }}>
          {file ? file.name : "Chọn file ePub/PDF"}
          <input type="file" hidden accept=".epub,.pdf,application/epub+zip,application/pdf" onChange={handleFileChange} />
        </Button>

        {error && <Alert severity="error">{error}</Alert>}

        {result && (
          <Alert severity="success">
            Đã tạo {result.chaptersCreated} chương.
            {result.warnings?.length > 0 && (
              <Box component="ul" sx={{ mt: 1, mb: 0, pl: 2.5 }}>
                {result.warnings.map((w, idx) => (
                  <Typography key={idx} component="li" variant="caption" sx={{ display: "list-item" }}>
                    {w}
                  </Typography>
                ))}
              </Box>
            )}
          </Alert>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={handleClose} color="inherit" disabled={uploading}>
          {result ? "Đóng" : "Huỷ"}
        </Button>
        {!result && (
          <Button onClick={handleUpload} variant="contained" color="primary" disabled={!file || uploading} sx={{ fontWeight: 700 }}>
            {uploading ? <CircularProgress size={20} color="inherit" /> : "Tải lên"}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ImportBookContentDialog;
