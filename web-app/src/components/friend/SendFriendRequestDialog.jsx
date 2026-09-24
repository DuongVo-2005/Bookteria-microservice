import React, { useState, useEffect, useCallback } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  TextField,
  InputAdornment,
  Box,
  Typography,
  Alert,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  CircularProgress,
  Tooltip,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import { useFriend } from "../../context/FriendContext";
import { search as searchUsers } from "../../services/userService";

// Nhãn hiển thị khi đã có quan hệ khác NONE — không cho gửi lời mời lại từ
// đây (đã có chỗ xử lý riêng: tab "Lời mời" cho PENDING_RECEIVED, trang cá
// nhân/FriendStatusButton cho huỷ lời mời/bỏ chặn...), tránh trùng hành vi.
const RELATIONSHIP_LABELS = {
  ACCEPTED: "Bạn bè",
  PENDING_SENT: "Đã gửi lời mời",
  PENDING_RECEIVED: "Đã gửi lời mời cho bạn",
  BLOCKED: "Không khả dụng",
  BLOCKED_BY: "Không khả dụng",
};

export const SendFriendRequestDialog = ({ open, onClose }) => {
  const { sendFriendRequest, getFriendshipStatus } = useFriend();
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [error, setError] = useState(null);
  const [sendingUsername, setSendingUsername] = useState(null);

  const handleClose = () => {
    setSearchQuery("");
    setSearchResults([]);
    setHasSearched(false);
    setError(null);
    onClose();
  };

  const handleSearch = useCallback(async (query) => {
    if (!query?.trim()) {
      setSearchResults([]);
      setHasSearched(false);
      return;
    }

    setLoading(true);
    setHasSearched(true);
    setError(null);

    try {
      const response = await searchUsers(query.trim());
      setSearchResults(response?.data?.result?.data || []);
    } catch (err) {
      console.error("Error searching users:", err);
      setError("Không tìm được người dùng. Vui lòng thử lại.");
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (searchQuery) {
        handleSearch(searchQuery);
      } else {
        setSearchResults([]);
        setHasSearched(false);
        setError(null);
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery, handleSearch]);

  const handleClearSearch = () => {
    setSearchQuery("");
    setSearchResults([]);
    setHasSearched(false);
    setError(null);
  };

  const handleSelectUser = (user) => {
    setSendingUsername(user.username);
    sendFriendRequest(user.username)
      .then(() => {
        handleClose();
      })
      .catch(() => {
        // Toast lỗi đã hiện qua FriendContext, giữ dialog mở để thử lại
      })
      .finally(() => setSendingUsername(null));
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      fullWidth
      maxWidth="xs"
      slotProps={{ paper: { sx: { borderRadius: 3, p: 1 } } }}
    >
      <DialogTitle sx={{ fontWeight: 700, display: "flex", justifyContent: "space-between", alignItems: "center", pb: 1 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <PersonAddIcon color="primary" />
          <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
            Gửi lời mời kết bạn
          </Typography>
        </Box>
        <IconButton onClick={handleClose} size="small" aria-label="Đóng">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ pt: 1, pb: 2 }}>
        <TextField
          autoFocus
          fullWidth
          placeholder="Nhập username để tìm..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" />
                </InputAdornment>
              ),
              endAdornment: searchQuery && (
                <InputAdornment position="end">
                  <IconButton size="small" onClick={handleClearSearch} aria-label="clear search">
                    <ClearIcon fontSize="small" />
                  </IconButton>
                </InputAdornment>
              ),
            },
          }}
          sx={{ mb: 2 }}
        />

        <Box sx={{ height: 300, overflow: "auto" }}>
          {loading && (
            <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
              <CircularProgress size={28} />
            </Box>
          )}

          {!loading && error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {!loading && !error && searchResults.length > 0 && (
            <List>
              {searchResults.map((user) => {
                const status = getFriendshipStatus(user.userId);
                const canAdd = status === "NONE";
                const isSending = sendingUsername === user.username;

                return (
                  <ListItem
                    key={user.userId}
                    sx={{
                      borderRadius: 1,
                      opacity: sendingUsername && !isSending ? 0.5 : 1,
                    }}
                  >
                    <ListItemAvatar>
                      <Avatar src={user.avatar || ""} alt={user.username} />
                    </ListItemAvatar>
                    <ListItemText
                      primary={user.username}
                      secondary={`${user.firstName || ""} ${user.lastName || ""}`.trim()}
                      primaryTypographyProps={{ fontWeight: "medium", variant: "body1" }}
                    />
                    {isSending ? (
                      <CircularProgress size={20} />
                    ) : canAdd ? (
                      <Tooltip title="Kết bạn">
                        <span>
                          <IconButton
                            color="primary"
                            size="small"
                            disabled={Boolean(sendingUsername)}
                            onClick={() => handleSelectUser(user)}
                            aria-label={`Kết bạn với ${user.username}`}
                          >
                            <PersonAddIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    ) : (
                      <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0, pl: 1 }}>
                        {RELATIONSHIP_LABELS[status] || ""}
                      </Typography>
                    )}
                  </ListItem>
                );
              })}
            </List>
          )}

          {!loading && !error && searchResults.length === 0 && hasSearched && (
            <Box sx={{ p: 2, textAlign: "center" }}>
              <Typography color="text.secondary">Không tìm thấy user nào khớp "{searchQuery}"</Typography>
            </Box>
          )}

          {!loading && !error && !hasSearched && (
            <Box sx={{ p: 2, textAlign: "center" }}>
              <Typography color="text.secondary">Nhập username để tìm người muốn kết bạn</Typography>
            </Box>
          )}
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default SendFriendRequestDialog;
