import React, { useState } from "react";
import { Drawer, Box, Typography, List, ListItemButton, ListItemIcon, ListItemText, IconButton, Divider, TextField, InputAdornment, Chip } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import RadioButtonUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import PlayCircleFilledIcon from "@mui/icons-material/PlayCircleFilled";
import SearchIcon from "@mui/icons-material/Search";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import MenuBookIcon from "@mui/icons-material/MenuBook";

export const ReaderTableOfContents = ({ open, onClose, book, chapters, currentChapterId, onSelectChapter, progressPercent, drawerBg, textColor, activeItemBg, borderColor }) => {
  const [searchQuery, setSearchQuery] = useState("");

  const filteredChapters = chapters.filter((c) => c.title.toLowerCase().includes(searchQuery.toLowerCase()) || (c.subtitle && c.subtitle.toLowerCase().includes(searchQuery.toLowerCase())));

  const currentIndex = chapters.findIndex((c) => c.id === currentChapterId);

  return (
    <Drawer anchor="left" open={open} onClose={onClose} slotProps={{ paper: { sx: { width: { xs: "85vw", sm: 380 }, maxWidth: "100%", bgcolor: drawerBg, color: textColor, borderRight: `1px solid ${borderColor}` } } }}>
      <Box sx={{ p: 2.5, display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid ${borderColor}` }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1.2 }}>
          <MenuBookIcon color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 700, fontSize: "1.1rem" }}>
            Mục lục sách
          </Typography>
        </Box>
        <IconButton size="small" onClick={onClose} sx={{ color: "inherit" }}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </Box>

      <Box sx={{ p: 2.5, pb: 1.5 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.3 }}>
          {book.title}
        </Typography>
        <Typography variant="caption" sx={{ opacity: 0.75, display: "block", mt: 0.3 }}>
          Tác giả: {book.authors?.map((a) => a.name).join(", ")}
        </Typography>

        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mt: 1.5 }}>
          <Typography variant="caption" sx={{ fontWeight: 600 }}>
            Tiến độ tổng thể:
          </Typography>
          <Chip label={`${progressPercent}%`} size="small" color="primary" sx={{ fontWeight: 700, height: 22, fontSize: "0.75rem" }} />
        </Box>
      </Box>

      <Box sx={{ px: 2, pb: 1.5 }}>
        <TextField
          size="small"
          fullWidth
          placeholder="Tìm tên chương..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" sx={{ opacity: 0.6 }} />
                </InputAdornment>
              ),
              sx: { borderRadius: 2, fontSize: "0.85rem", bgcolor: "rgba(0,0,0,0.03)", color: "inherit" },
            },
          }}
        />
      </Box>

      <Divider sx={{ borderColor }} />

      <List sx={{ flex: 1, overflowY: "auto", p: 1 }}>
        {filteredChapters.map((chapter, index) => {
          const isCurrent = chapter.id === currentChapterId;
          const isRead = index < currentIndex;

          return (
            <ListItemButton
              key={chapter.id}
              onClick={() => {
                onSelectChapter(chapter, index);
                onClose();
              }}
              sx={{ borderRadius: 2, mb: 0.5, py: 1.2, bgcolor: isCurrent ? activeItemBg : "transparent", "&:hover": { bgcolor: isCurrent ? activeItemBg : "rgba(0,0,0,0.04)" } }}
            >
              <ListItemIcon sx={{ minWidth: 36, color: isCurrent ? "primary.main" : "inherit" }}>
                {isCurrent ? <PlayCircleFilledIcon fontSize="small" color="primary" /> : isRead ? <CheckCircleIcon fontSize="small" color="success" /> : <RadioButtonUncheckedIcon fontSize="small" sx={{ opacity: 0.4 }} />}
              </ListItemIcon>

              <ListItemText
                primary={
                  <Typography variant="body2" sx={{ fontWeight: isCurrent ? 700 : 500, color: isCurrent ? "primary.main" : "inherit" }}>
                    {chapter.title}
                  </Typography>
                }
                secondary={
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 0.3 }}>
                    {chapter.subtitle && (
                      <Typography variant="caption" sx={{ opacity: 0.7, display: "block", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 180 }}>
                        {chapter.subtitle}
                      </Typography>
                    )}
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.3, opacity: 0.6, ml: "auto" }}>
                      <AccessTimeIcon sx={{ fontSize: 11 }} />
                      <Typography variant="caption">{chapter.readTimeMinutes}p</Typography>
                    </Box>
                  </Box>
                }
              />
            </ListItemButton>
          );
        })}
      </List>
    </Drawer>
  );
};

export default ReaderTableOfContents;
