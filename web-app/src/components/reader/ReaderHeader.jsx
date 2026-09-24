import React from "react";
import { Box, Typography, IconButton, Tooltip, Badge } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import MenuIcon from "@mui/icons-material/Menu";
import FormatSizeIcon from "@mui/icons-material/FormatSize";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import BorderColorIcon from "@mui/icons-material/BorderColor";
import FullscreenIcon from "@mui/icons-material/Fullscreen";
import FullscreenExitIcon from "@mui/icons-material/FullscreenExit";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import WbIncandescentIcon from "@mui/icons-material/WbIncandescent";
import { useNavigate } from "react-router-dom";

export const ReaderHeader = ({ book, currentChapter, settings, onUpdateSettings, onToggleToC, onOpenSettings, onToggleBookmark, isCurrentBookmarked, onOpenNotesDrawer, notesCount, isFullscreen, onToggleFullscreen, headerBg, headerTextColor, borderColor }) => {
  const navigate = useNavigate();

  const handleQuickThemeCycle = () => {
    if (settings.theme === "light") {
      onUpdateSettings({ theme: "sepia" });
    } else if (settings.theme === "sepia") {
      onUpdateSettings({ theme: "dark" });
    } else {
      onUpdateSettings({ theme: "light" });
    }
  };

  const getThemeIcon = () => {
    if (settings.theme === "light") return <Brightness7Icon fontSize="small" />;
    if (settings.theme === "sepia") return <WbIncandescentIcon fontSize="small" />;
    return <Brightness4Icon fontSize="small" />;
  };

  return (
    <Box
      component="header"
      sx={{
        height: 60,
        px: { xs: 1.5, sm: 3 },
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        bgcolor: headerBg,
        color: headerTextColor,
        borderBottom: `1px solid ${borderColor}`,
        position: "sticky",
        top: 0,
        zIndex: 1100,
        transition: "background-color 0.3s, color 0.3s, border-color 0.3s",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, minWidth: 0 }}>
        <Tooltip title="Quay lại chi tiết sách">
          <IconButton size="small" onClick={() => navigate(`/books/${book.id}`)} sx={{ color: "inherit" }}>
            <ArrowBackIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <Tooltip title="Mục lục các chương">
          <IconButton size="small" onClick={onToggleToC} sx={{ color: "inherit" }}>
            <MenuIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <Box sx={{ minWidth: 0, ml: 0.5, display: { xs: "none", md: "block" } }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 320, lineHeight: 1.2 }}>
            {book.title}
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.75, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 320, display: "block" }}>
            {currentChapter.title}
          </Typography>
        </Box>
      </Box>

      <Box sx={{ display: { xs: "block", md: "none" }, textAlign: "center", minWidth: 0, px: 1 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 700, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 180, fontSize: "0.85rem" }}>
          {currentChapter.title}
        </Typography>
      </Box>

      <Box sx={{ display: "flex", alignItems: "center", gap: { xs: 0.5, sm: 1 }, flexShrink: 0 }}>
        <Tooltip title={`Chủ đề: ${settings.theme} (Bấm để chuyển đổi)`}>
          <IconButton size="small" onClick={handleQuickThemeCycle} sx={{ color: "inherit" }}>
            {getThemeIcon()}
          </IconButton>
        </Tooltip>

        <Tooltip title="Cài đặt giao diện đọc">
          <IconButton size="small" onClick={onOpenSettings} sx={{ color: "inherit" }}>
            <FormatSizeIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <Tooltip title={isCurrentBookmarked ? "Đã đánh dấu chương này" : "Đánh dấu trang này"}>
          <IconButton size="small" onClick={onToggleBookmark} sx={{ color: isCurrentBookmarked ? "#f59e0b" : "inherit" }}>
            {isCurrentBookmarked ? <BookmarkIcon fontSize="small" /> : <BookmarkBorderIcon fontSize="small" />}
          </IconButton>
        </Tooltip>

        <Tooltip title="Danh sách Tô sáng & Ghi chú">
          <IconButton size="small" onClick={onOpenNotesDrawer} sx={{ color: "inherit" }}>
            <Badge badgeContent={notesCount} color="error" max={99}>
              <BorderColorIcon fontSize="small" />
            </Badge>
          </IconButton>
        </Tooltip>

        <Tooltip title={isFullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"}>
          <IconButton size="small" onClick={onToggleFullscreen} sx={{ color: "inherit", display: { xs: "none", sm: "inline-flex" } }}>
            {isFullscreen ? <FullscreenExitIcon fontSize="small" /> : <FullscreenIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      </Box>
    </Box>
  );
};

export default ReaderHeader;
