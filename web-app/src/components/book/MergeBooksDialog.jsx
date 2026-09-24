import React, { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Box, Typography, TextField, Button, CircularProgress, List, ListItemButton, ListItemAvatar, Avatar, ListItemText, Alert, IconButton } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { searchBooks } from "../../services/searchService";
import { mergeBooks } from "../../services/bookService";
import { getBookErrorMessage } from "../../services/errorMessages";
import { useToast } from "../../context/ToastContext";

// BA Backlog OPS-02 (be-report.md, bổ sung 2026-09-19): gộp sách trùng —
// `book` (prop) là sách GIỮ LẠI (target), chọn 1 sách khác làm bản trùng
// (duplicate) sẽ bị xoá sau khi merge.
export const MergeBooksDialog = ({ open, book, onClose, onMerged }) => {
  const { showError } = useToast();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState(null);
  const [merging, setMerging] = useState(false);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setResults([]);
      setSelected(null);
    }
  }, [open]);

  useEffect(() => {
    const cleanQuery = query.trim();
    if (!cleanQuery) {
      setResults([]);
      return;
    }
    setSearching(true);
    const timer = setTimeout(() => {
      searchBooks(cleanQuery, undefined, undefined, 0, 10)
        .then((res) => setResults((res?.data?.result?.data || []).filter((b) => b.id !== book?.id)))
        .catch(() => setResults([]))
        .finally(() => setSearching(false));
    }, 400);
    return () => clearTimeout(timer);
  }, [query, book?.id]);

  const handleConfirmMerge = () => {
    if (!selected || !book) return;
    setMerging(true);
    mergeBooks(book.id, selected.id)
      .then(() => {
        onMerged?.();
        onClose();
      })
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setMerging(false));
  };

  if (!book) return null;

  return (
    <Dialog open={open} onClose={() => !merging && onClose()} fullWidth maxWidth="sm">
      <DialogTitle sx={{ fontWeight: 700, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        Gộp sách trùng
        <IconButton size="small" onClick={onClose} disabled={merging}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Sách <strong>"{book.title}"</strong> sẽ được <strong>giữ lại</strong>. Chọn sách trùng lặp bên dưới — sách đó sẽ bị{" "}
          <strong>xoá vĩnh viễn</strong> sau khi đánh giá/kệ sách của người dùng được chuyển sang sách này.
        </Typography>

        <TextField fullWidth size="small" placeholder="Tìm sách trùng theo tiêu đề…" value={query} onChange={(e) => setQuery(e.target.value)} autoFocus />

        {searching && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
            <CircularProgress size={20} />
          </Box>
        )}

        {!searching && results.length > 0 && (
          <List sx={{ mt: 1, maxHeight: 260, overflowY: "auto" }}>
            {results.map((b) => (
              <ListItemButton key={b.id} selected={selected?.id === b.id} onClick={() => setSelected(b)} sx={{ borderRadius: 2, mb: 0.5 }}>
                <ListItemAvatar>
                  <Avatar variant="rounded" src={b.metadata?.coverImage || undefined} sx={{ width: 36, height: 50 }} />
                </ListItemAvatar>
                <ListItemText
                  primary={b.title}
                  secondary={b.authors?.[0]?.name}
                  slotProps={{ primary: { sx: { fontSize: "0.88rem", fontWeight: 600 } }, secondary: { sx: { fontSize: "0.75rem" } } }}
                />
              </ListItemButton>
            ))}
          </List>
        )}

        {selected && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            Sẽ gộp <strong>"{selected.title}"</strong> vào <strong>"{book.title}"</strong> và xoá vĩnh viễn "{selected.title}".
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        <Button onClick={onClose} disabled={merging} sx={{ textTransform: "none" }}>
          Huỷ
        </Button>
        <Button variant="contained" color="warning" onClick={handleConfirmMerge} disabled={!selected || merging} sx={{ textTransform: "none", fontWeight: 700 }}>
          {merging ? <CircularProgress size={18} color="inherit" /> : "Xác nhận gộp"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default MergeBooksDialog;
