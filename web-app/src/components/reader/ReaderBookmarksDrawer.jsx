import React, { useState } from "react";
import { Drawer, Box, Typography, Tabs, Tab, IconButton, List, Paper, Button, TextField, Dialog, DialogTitle, DialogContent, DialogActions, Tooltip } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import BorderColorIcon from "@mui/icons-material/BorderColor";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ShareIcon from "@mui/icons-material/Share";
import BoltIcon from "@mui/icons-material/Bolt";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import { EmptyState } from "../book/EmptyState";

const colorMap = {
  yellow: { bg: "#fef08a", border: "#eab308", label: "Vàng" },
  green: { bg: "#bbf7d0", border: "#22c55e", label: "Xanh lá" },
  pink: { bg: "#fbcfe8", border: "#ec4899", label: "Hồng" },
  blue: { bg: "#bfdbfe", border: "#3b82f6", label: "Xanh dương" },
  purple: { bg: "#e9d5ff", border: "#a855f7", label: "Tím" },
};

export const ReaderBookmarksDrawer = ({ open, onClose, bookmarks, highlights, onSelectBookmark, onSelectHighlight, onDeleteBookmark, onDeleteHighlight, onUpdateHighlightNote, onShareQuote, onQuickShareHighlight, drawerBg, textColor, borderColor }) => {
  const [activeTab, setActiveTab] = useState("HIGHLIGHTS");
  const [editingHighlight, setEditingHighlight] = useState(null);
  const [editNoteText, setEditNoteText] = useState("");

  const handleOpenEditNote = (hl) => {
    setEditingHighlight(hl);
    setEditNoteText(hl.note || "");
  };

  const handleSaveNote = () => {
    if (editingHighlight) {
      onUpdateHighlightNote(editingHighlight.id, editNoteText);
      setEditingHighlight(null);
    }
  };

  return (
    <>
      <Drawer anchor="right" open={open} onClose={onClose} slotProps={{ paper: { sx: { width: { xs: "90vw", sm: 420 }, maxWidth: "100%", bgcolor: drawerBg, color: textColor, borderLeft: `1px solid ${borderColor}` } } }}>
        <Box sx={{ p: 2, display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid ${borderColor}` }}>
          <Typography variant="h6" sx={{ fontWeight: 700, fontSize: "1.05rem" }}>
            Ghi chú & Đánh dấu
          </Typography>
          <IconButton size="small" onClick={onClose} sx={{ color: "inherit" }}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>

        <Box sx={{ borderBottom: `1px solid ${borderColor}` }}>
          <Tabs value={activeTab} onChange={(_, val) => setActiveTab(val)} variant="fullWidth" sx={{ "& .MuiTab-root": { textTransform: "none", fontWeight: 600, fontSize: "0.9rem", minHeight: 48 } }}>
            <Tab icon={<BorderColorIcon fontSize="small" />} iconPosition="start" label={`Tô sáng (${highlights.length})`} value="HIGHLIGHTS" />
            <Tab icon={<BookmarkIcon fontSize="small" />} iconPosition="start" label={`Dấu trang (${bookmarks.length})`} value="BOOKMARKS" />
          </Tabs>
        </Box>

        {activeTab === "HIGHLIGHTS" && (
          <Box sx={{ flex: 1, overflowY: "auto", p: 2 }}>
            {highlights.length > 0 ? (
              <List sx={{ p: 0 }}>
                {highlights.map((hl) => {
                  const style = colorMap[hl.color] || colorMap.yellow;
                  return (
                    <Paper
                      key={hl.id}
                      elevation={0}
                      sx={{ p: 2, mb: 2, borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.02)", border: "1px solid rgba(0,0,0,0.08)", borderLeft: `5px solid ${style.border}`, transition: "box-shadow 0.2s", "&:hover": { boxShadow: "0 4px 12px rgba(0,0,0,0.06)" } }}
                    >
                      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
                        <Typography variant="caption" sx={{ fontWeight: 700, color: "primary.main" }}>
                          {hl.chapterTitle}
                        </Typography>
                        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, opacity: 0.6 }}>
                          <AccessTimeIcon sx={{ fontSize: 11 }} />
                          <Typography variant="caption">{hl.createdAt}</Typography>
                        </Box>
                      </Box>

                      <Box
                        onClick={() => {
                          onSelectHighlight(hl);
                          onClose();
                        }}
                        sx={{ p: 1.2, borderRadius: 1.5, bgcolor: style.bg, color: "#1f2937", fontStyle: "italic", fontSize: "0.9rem", cursor: "pointer", lineHeight: 1.5, mb: hl.note ? 1.5 : 1 }}
                      >
                        "{hl.selectedText}"
                      </Box>

                      {hl.note && (
                        <Box sx={{ mb: 1.5, pl: 1, borderLeft: "2px solid rgba(0,0,0,0.15)" }}>
                          <Typography variant="body2" sx={{ fontSize: "0.85rem" }}>
                            {hl.note}
                          </Typography>
                        </Box>
                      )}

                      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "flex-end", gap: 0.5, mt: 1 }}>
                        <Tooltip title="Chia sẻ nhanh lên Bảng tin (1 chạm, không cần chỉnh sửa)">
                          <IconButton size="small" onClick={() => onQuickShareHighlight(hl.id)} color="primary">
                            <BoltIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        <Tooltip title="Chia sẻ trích dẫn (tuỳ chỉnh thiệp trước khi đăng)">
                          <IconButton size="small" onClick={() => onShareQuote(hl.selectedText, hl.id)} color="primary">
                            <ShareIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        <Tooltip title="Thêm/Sửa ghi chú">
                          <IconButton size="small" onClick={() => handleOpenEditNote(hl)}>
                            <EditOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        <Tooltip title="Xoá tô sáng">
                          <IconButton size="small" onClick={() => onDeleteHighlight(hl.id)} color="error">
                            <DeleteOutlineOutlinedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </Paper>
                  );
                })}
              </List>
            ) : (
              <EmptyState title="Chưa có đoạn tô sáng nào" description="Bôi đen đoạn văn bản khi đọc sách để tô màu highlight và lưu ghi chú suy ngẫm." />
            )}
          </Box>
        )}

        {activeTab === "BOOKMARKS" && (
          <Box sx={{ flex: 1, overflowY: "auto", p: 2 }}>
            {bookmarks.length > 0 ? (
              <List sx={{ p: 0 }}>
                {bookmarks.map((bm) => (
                  <Paper key={bm.id} elevation={0} sx={{ p: 2, mb: 2, borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.02)", border: "1px solid rgba(0,0,0,0.08)", transition: "box-shadow 0.2s", "&:hover": { boxShadow: "0 4px 12px rgba(0,0,0,0.06)" } }}>
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", mb: 1 }}>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                        <BookmarkIcon fontSize="small" color="primary" />
                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                          {bm.chapterTitle}
                        </Typography>
                      </Box>
                      <IconButton size="small" onClick={() => onDeleteBookmark(bm.id)} color="error">
                        <DeleteOutlineOutlinedIcon fontSize="small" />
                      </IconButton>
                    </Box>

                    {bm.snippet && (
                      <Typography variant="body2" color="text.secondary" sx={{ fontSize: "0.85rem", mb: 1.5, fontStyle: "italic" }}>
                        {bm.snippet}
                      </Typography>
                    )}

                    {bm.note && (
                      <Typography variant="caption" sx={{ display: "block", mb: 1.5, color: "text.primary" }}>
                        Ghi chú: {bm.note}
                      </Typography>
                    )}

                    <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", pt: 1, borderTop: "1px dashed rgba(0,0,0,0.1)" }}>
                      <Typography variant="caption" color="text.disabled">
                        {bm.createdAt}
                      </Typography>
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          onSelectBookmark(bm);
                          onClose();
                        }}
                        sx={{ textTransform: "none", borderRadius: 1.5, py: 0.2, fontSize: "0.75rem" }}
                      >
                        Nhảy tới trang
                      </Button>
                    </Box>
                  </Paper>
                ))}
              </List>
            ) : (
              <EmptyState title="Chưa có dấu trang nào" description="Bấm vào biểu tượng Đánh dấu trang trên thanh công cụ để lưu vị trí đang đọc." />
            )}
          </Box>
        )}
      </Drawer>

      <Dialog open={Boolean(editingHighlight)} onClose={() => setEditingHighlight(null)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 700 }}>Ghi chú cho đoạn trích</DialogTitle>
        <DialogContent>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 2, fontStyle: "italic", bgcolor: "rgba(0,0,0,0.03)", p: 1, borderRadius: 1.5 }}>
            "{editingHighlight?.selectedText}"
          </Typography>
          <TextField autoFocus fullWidth multiline rows={3} label="Nội dung suy ngẫm/ghi chú" placeholder="Nhập ghi chú của bạn về câu trích dẫn này..." value={editNoteText} onChange={(e) => setEditNoteText(e.target.value)} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setEditingHighlight(null)} color="inherit">
            Huỷ
          </Button>
          <Button onClick={handleSaveNote} variant="contained" color="primary">
            Lưu ghi chú
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default ReaderBookmarksDrawer;
