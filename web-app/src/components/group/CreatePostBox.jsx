import React, { useState } from "react";
import { Paper, Box, Avatar, TextField, Button, Chip, Menu, MenuItem, ListItemIcon, ListItemText, Typography, CircularProgress } from "@mui/material";
import SendIcon from "@mui/icons-material/Send";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import CloseIcon from "@mui/icons-material/Close";
import { useGroup } from "../../context/GroupContext";
import { getBooks } from "../../services/bookService";

export const CreatePostBox = ({ groupId, onCreated }) => {
  const { myAvatar, myDisplayName, createPost } = useGroup();

  const [content, setContent] = useState("");
  const [selectedBook, setSelectedBook] = useState(null);
  const [anchorElBooks, setAnchorElBooks] = useState(null);
  const [bookOptions, setBookOptions] = useState([]);
  const [loadingBooks, setLoadingBooks] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleOpenBookMenu = (e) => {
    setAnchorElBooks(e.currentTarget);
    if (bookOptions.length === 0) {
      setLoadingBooks(true);
      getBooks(0, 20)
        .then((response) => setBookOptions(response?.data?.result?.data || []))
        .catch(() => setBookOptions([]))
        .finally(() => setLoadingBooks(false));
    }
  };

  const handleCloseBookMenu = () => setAnchorElBooks(null);

  const handleSelectBook = (book) => {
    setSelectedBook(book);
    handleCloseBookMenu();
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!content.trim()) return;

    setSubmitting(true);
    createPost(groupId, content.trim(), selectedBook?.id)
      .then((newPost) => {
        onCreated?.(newPost);
        setContent("");
        setSelectedBook(null);
      })
      .catch(() => {})
      .finally(() => setSubmitting(false));
  };

  return (
    <Paper elevation={0} component="form" onSubmit={handleSubmit} sx={{ p: 2.5, mb: 3, borderRadius: 3, border: "1px solid rgba(0, 0, 0, 0.08)", boxShadow: "0 2px 8px rgba(0, 0, 0, 0.03)" }}>
      <Box sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}>
        <Avatar src={myAvatar} alt={myDisplayName} sx={{ width: 44, height: 44, mt: 0.5, boxShadow: "0 2px 6px rgba(0,0,0,0.1)" }} />
        <Box sx={{ flexGrow: 1 }}>
          <TextField
            fullWidth
            multiline
            minRows={2}
            maxRows={6}
            placeholder="Chia sẻ suy nghĩ, trích dẫn sách hoặc câu hỏi thảo luận với nhóm…"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            variant="outlined"
            slotProps={{ input: { sx: { borderRadius: 2, bgcolor: "#fafafa", fontSize: "0.95rem" } } }}
          />

          {selectedBook && (
            <Box sx={{ display: "inline-flex", alignItems: "center", gap: 1, mt: 1.5, p: 0.8, px: 1.5, borderRadius: 2, bgcolor: "rgba(25, 118, 210, 0.08)", border: "1px solid rgba(25, 118, 210, 0.2)" }}>
              <AutoStoriesIcon sx={{ fontSize: 16, color: "primary.main" }} />
              <Typography variant="caption" sx={{ fontWeight: 600, color: "primary.dark" }}>
                Đang nhắc đến: {selectedBook.title}
              </Typography>
              <Chip label="Bỏ" size="small" onDelete={() => setSelectedBook(null)} deleteIcon={<CloseIcon sx={{ fontSize: "14px !important" }} />} sx={{ height: 20, fontSize: "0.7rem", ml: 0.5 }} />
            </Box>
          )}

          <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mt: 2, pt: 1.5, borderTop: "1px solid rgba(0,0,0,0.06)" }}>
            <Button size="small" variant="outlined" color="inherit" startIcon={<AutoStoriesIcon sx={{ color: "primary.main" }} />} onClick={handleOpenBookMenu} sx={{ borderRadius: 2, borderColor: "rgba(0,0,0,0.15)", fontWeight: 600, fontSize: "0.8rem" }}>
              Gắn thẻ sách
            </Button>

            <Menu anchorEl={anchorElBooks} open={Boolean(anchorElBooks)} onClose={handleCloseBookMenu} slotProps={{ paper: { sx: { maxHeight: 300, width: 320, borderRadius: 2, boxShadow: 4 } } }}>
              <Box sx={{ px: 2, py: 1 }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700 }}>
                  CHỌN SÁCH THẢO LUẬN
                </Typography>
              </Box>
              {loadingBooks && (
                <Box sx={{ display: "flex", justifyContent: "center", p: 2 }}>
                  <CircularProgress size={20} />
                </Box>
              )}
              {!loadingBooks && bookOptions.length === 0 && (
                <Box sx={{ px: 2, py: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    Không có sách nào.
                  </Typography>
                </Box>
              )}
              {!loadingBooks &&
                bookOptions.map((book) => (
                  <MenuItem key={book.id} onClick={() => handleSelectBook(book)}>
                    <ListItemIcon>
                      <AutoStoriesIcon fontSize="small" color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary={
                        <Typography variant="body2" sx={{ fontSize: "0.85rem", fontWeight: 600 }} noWrap>
                          {book.title}
                        </Typography>
                      }
                      secondary={
                        <Typography variant="caption" color="text.secondary" noWrap>
                          {book.authors?.[0]?.name}
                        </Typography>
                      }
                    />
                  </MenuItem>
                ))}
            </Menu>

            <Button type="submit" variant="contained" color="primary" disabled={!content.trim() || submitting} endIcon={submitting ? undefined : <SendIcon />} sx={{ fontWeight: 700, px: 2.5, py: 0.8, borderRadius: 2 }}>
              {submitting ? <CircularProgress size={18} color="inherit" /> : "Đăng bài"}
            </Button>
          </Box>
        </Box>
      </Box>
    </Paper>
  );
};

export default CreatePostBox;
