import React, { useState } from "react";
import {
  Paper,
  Box,
  Typography,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  LinearProgress,
  Button,
  Divider,
} from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import { useNavigate } from "react-router-dom";
import { PlaceholderCover } from "./PlaceholderCover";

export const ShelfItem = ({
  item,
  currentTab,
  onUpdateStatus,
  onOpenProgressDialog,
  onOpenMarkCompletedDialog,
  onRemove,
}) => {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [imgError, setImgError] = useState(false);
  const openMenu = Boolean(anchorEl);

  const mainAuthor =
    item.book.authors.find((a) => a.role === "MAIN_AUTHOR")?.name ||
    item.book.authors[0]?.name ||
    "Tác giả";

  const totalPages = item.book.metadata?.pageCount || 1;
  const currentPage = item.currentPage || 0;
  const progressPercent = Math.min(100, Math.round((currentPage / totalPages) * 100));

  const handleMenuOpen = (e) => {
    e.stopPropagation();
    setAnchorEl(e.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleBookClick = () => {
    navigate(`/books/${item.book.id}`);
  };

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2,
        mb: 2,
        borderRadius: 2,
        border: "1px solid rgba(0, 0, 0, 0.08)",
        display: "flex",
        flexDirection: { xs: "column", sm: "row" },
        alignItems: { xs: "flex-start", sm: "center" },
        gap: 2,
        transition: "box-shadow 0.2s, border-color 0.2s",
        "&:hover": {
          borderColor: "primary.light",
          boxShadow: "0 2px 8px rgba(0, 0, 0, 0.05)",
        },
      }}
    >
      <Box
        onClick={handleBookClick}
        sx={{
          width: 60,
          height: 90,
          flexShrink: 0,
          cursor: "pointer",
          borderRadius: 1,
          overflow: "hidden",
          boxShadow: "0 2px 6px rgba(0,0,0,0.12)",
        }}
      >
        {item.book.metadata?.coverImage && !imgError ? (
          <Box
            component="img"
            src={item.book.metadata.coverImage}
            alt={item.book.title}
            onError={() => setImgError(true)}
            sx={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        ) : (
          <PlaceholderCover title={item.book.title} />
        )}
      </Box>

      <Box sx={{ flexGrow: 1, minWidth: 0, width: "100%" }}>
        <Typography
          variant="subtitle1"
          onClick={handleBookClick}
          sx={{
            fontWeight: 600,
            cursor: "pointer",
            lineHeight: 1.3,
            "&:hover": { color: "primary.main" },
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: { sm: "nowrap" },
          }}
        >
          {item.book.title}
        </Typography>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          {mainAuthor}
        </Typography>

        {currentTab === "READING" && (
          <Box sx={{ mt: 1, maxWidth: 460 }}>
            <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 0.5 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                trang {currentPage}/{totalPages} ({progressPercent}%)
              </Typography>
              <Button
                size="small"
                variant="text"
                startIcon={<EditIcon sx={{ fontSize: 14 }} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenProgressDialog(item);
                }}
                sx={{ p: 0, minWidth: "auto", fontSize: "0.75rem" }}
              >
                Cập nhật tiến độ
              </Button>
            </Box>
            <LinearProgress
              variant="determinate"
              value={progressPercent}
              sx={{ height: 6, borderRadius: 3, bgcolor: "rgba(25, 118, 210, 0.1)" }}
            />
          </Box>
        )}

        {currentTab === "COMPLETED" && (
          <Typography variant="caption" color="success.main" sx={{ display: "flex", alignItems: "center", gap: 0.5, fontWeight: 500 }}>
            <CheckCircleIcon fontSize="small" /> Đã đọc xong ({totalPages} trang)
          </Typography>
        )}
      </Box>

      <Box sx={{ display: "flex", alignItems: "center", gap: 1, alignSelf: { xs: "flex-end", sm: "center" }, flexShrink: 0 }}>
        <Button
          size="small"
          variant="contained"
          color="primary"
          startIcon={<AutoStoriesIcon />}
          onClick={() => navigate(`/reader/${item.book.id}`)}
          sx={{ fontWeight: 700, borderRadius: 2, textTransform: "none", fontSize: "0.82rem", px: 2, display: { xs: "none", sm: "inline-flex" } }}
        >
          Đọc sách
        </Button>

        <IconButton onClick={handleMenuOpen} aria-label="Tác vụ" size="medium">
          <MoreVertIcon />
        </IconButton>

        <Menu
          anchorEl={anchorEl}
          open={openMenu}
          onClose={handleMenuClose}
          slotProps={{
            paper: {
              sx: { minWidth: 200, borderRadius: 2, boxShadow: 3 },
            },
          }}
        >
          {currentTab !== "READING" && (
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onUpdateStatus(item.id, "READING");
              }}
            >
              <ListItemIcon>
                <AutoStoriesIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Chuyển sang Đang đọc" />
            </MenuItem>
          )}

          {currentTab !== "WANT_TO_READ" && (
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onUpdateStatus(item.id, "WANT_TO_READ");
              }}
            >
              <ListItemIcon>
                <BookmarkBorderIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Chuyển sang Muốn đọc" />
            </MenuItem>
          )}

          {currentTab !== "COMPLETED" && (
            <MenuItem
              onClick={() => {
                handleMenuClose();
                onOpenMarkCompletedDialog(item);
              }}
            >
              <ListItemIcon>
                <CheckCircleIcon fontSize="small" color="success" />
              </ListItemIcon>
              <ListItemText primary="Đánh dấu Đã đọc" />
            </MenuItem>
          )}

          <Divider sx={{ my: 0.5 }} />

          <MenuItem
            onClick={() => {
              handleMenuClose();
              onRemove(item.id);
            }}
            sx={{ color: "error.main" }}
          >
            <ListItemIcon>
              <DeleteIcon fontSize="small" color="error" />
            </ListItemIcon>
            <ListItemText primary="Xoá khỏi kệ" />
          </MenuItem>
        </Menu>
      </Box>
    </Paper>
  );
};

export default ShelfItem;
