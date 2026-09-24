import React, { useState, useEffect, useCallback } from "react";
import { Button, ButtonGroup, Menu, MenuItem, ListItemIcon, ListItemText, Box, CircularProgress } from "@mui/material";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import HowToRegIcon from "@mui/icons-material/HowToReg";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import PersonRemoveIcon from "@mui/icons-material/PersonRemove";
import BlockIcon from "@mui/icons-material/Block";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import { useFriend } from "../../context/FriendContext";
import { UnfriendConfirmDialog } from "./UnfriendConfirmDialog";
import { BlockConfirmDialog } from "./BlockConfirmDialog";

// Nút/khối hành động theo đúng 6 trạng thái quan hệ (mục 2 friend-system-ui-spec.md):
// NONE -> "Kết bạn" | PENDING_SENT -> "Huỷ lời mời" | PENDING_RECEIVED -> "Chấp nhận"/"Từ chối"
// ACCEPTED -> "Bạn bè" (menu "Xoá kết bạn"/"Chặn") | BLOCKED -> "Bỏ chặn" | BLOCKED_BY -> ẩn hết
//
// Dùng fetchFriendshipStatus() (gọi thật GET /friends/status/{id}) thay vì
// getFriendshipStatus() suy từ state local — vì "BLOCKED_BY" không thể suy ra
// được từ dữ liệu đã tải sẵn (không có API liệt kê "ai đang chặn tôi", xem
// mục 3.1 friend-system-fe-tasks.md). Component này dùng cho trang cá nhân
// người khác nên luôn cần trạng thái đúng nhất tại thời điểm hiển thị.
export const FriendStatusButton = ({ otherUserId, otherUsername, otherAvatar, size = "small" }) => {
  const {
    fetchFriendshipStatus,
    sendFriendRequest,
    acceptFriendRequest,
    rejectFriendRequest,
    cancelFriendRequest,
    unfriend,
    blockUser,
    unblockUser,
    incomingRequests,
    outgoingRequests,
  } = useFriend();

  const [anchorEl, setAnchorEl] = useState(null);
  const [unfriendConfirmOpen, setUnfriendConfirmOpen] = useState(false);
  const [blockConfirmOpen, setBlockConfirmOpen] = useState(false);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadStatus = useCallback(() => {
    if (!otherUserId) return;
    setLoading(true);
    fetchFriendshipStatus(otherUserId)
      .then((s) => setStatus(s || "NONE"))
      .catch(() => setStatus("NONE"))
      .finally(() => setLoading(false));
  }, [otherUserId, fetchFriendshipStatus]);

  useEffect(() => {
    loadStatus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [otherUserId]);

  const handleMenuOpen = (e) => {
    setAnchorEl(e.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleUnfriendConfirm = (userId) => {
    unfriend(userId)
      .then(loadStatus)
      .catch(() => {});
    setUnfriendConfirmOpen(false);
  };

  const handleBlockConfirm = (userId) => {
    blockUser(userId)
      .then(loadStatus)
      .catch(() => {});
    setBlockConfirmOpen(false);
  };

  if (loading) {
    return <CircularProgress size={24} />;
  }

  // Trạng thái: BLOCKED_BY -> Ẩn toàn bộ nút hành động kết bạn
  if (status === "BLOCKED_BY") {
    return null;
  }

  // Trạng thái: BLOCKED -> Nút "Bỏ chặn"
  if (status === "BLOCKED") {
    return (
      <Button
        variant="outlined"
        color="primary"
        size={size}
        startIcon={<LockOpenIcon />}
        onClick={() => unblockUser(otherUserId).then(loadStatus).catch(() => {})}
        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none" }}
      >
        Bỏ chặn
      </Button>
    );
  }

  if (status === "NONE") {
    return (
      <Button
        variant="contained"
        color="primary"
        size={size}
        startIcon={<PersonAddIcon />}
        onClick={() => sendFriendRequest(otherUsername).then(loadStatus).catch(() => {})}
        sx={{ fontWeight: 700, borderRadius: 2, textTransform: "none" }}
      >
        Kết bạn
      </Button>
    );
  }

  if (status === "PENDING_SENT") {
    const request = outgoingRequests.find((r) => r.receiverId === otherUserId);
    return (
      <Button
        variant="outlined"
        color="warning"
        size={size}
        startIcon={<DeleteOutlineOutlinedIcon />}
        onClick={() => request && cancelFriendRequest(request.id).then(loadStatus).catch(() => {})}
        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none" }}
      >
        Huỷ lời mời
      </Button>
    );
  }

  if (status === "PENDING_RECEIVED") {
    const request = incomingRequests.find((r) => r.senderId === otherUserId);
    return (
      <ButtonGroup size={size} variant="contained" sx={{ borderRadius: 2 }}>
        <Button
          color="primary"
          startIcon={<CheckIcon />}
          onClick={() => request && acceptFriendRequest(request.id).then(loadStatus).catch(() => {})}
          sx={{ fontWeight: 700, textTransform: "none" }}
        >
          Chấp nhận
        </Button>
        <Button
          color="inherit"
          startIcon={<CloseIcon />}
          onClick={() => request && rejectFriendRequest(request.id).then(loadStatus).catch(() => {})}
          sx={{ fontWeight: 600, textTransform: "none", bgcolor: "action.hover", "&:hover": { bgcolor: "error.light", color: "#fff" } }}
        >
          Từ chối
        </Button>
      </ButtonGroup>
    );
  }

  const friendObj = {
    userId: otherUserId,
    username: otherUsername || otherUserId,
    avatar: otherAvatar,
  };

  const blockObj = {
    userId: otherUserId,
    username: otherUsername || otherUserId,
    avatar: otherAvatar,
  };

  return (
    <Box sx={{ display: "inline-block" }}>
      <Button
        variant="outlined"
        color="success"
        size={size}
        startIcon={<HowToRegIcon />}
        endIcon={<ArrowDropDownIcon />}
        onClick={handleMenuOpen}
        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none" }}
      >
        Bạn bè
      </Button>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose} slotProps={{ paper: { sx: { minWidth: 160, borderRadius: 2, boxShadow: 3 } } }}>
        <MenuItem
          onClick={() => {
            handleMenuClose();
            setUnfriendConfirmOpen(true);
          }}
          sx={{ color: "error.main" }}
        >
          <ListItemIcon>
            <PersonRemoveIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText primary="Xoá kết bạn" />
        </MenuItem>
        <MenuItem
          onClick={() => {
            handleMenuClose();
            setBlockConfirmOpen(true);
          }}
          sx={{ color: "text.primary" }}
        >
          <ListItemIcon>
            <BlockIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText primary="Chặn người này" />
        </MenuItem>
      </Menu>

      <UnfriendConfirmDialog open={unfriendConfirmOpen} friend={friendObj} onClose={() => setUnfriendConfirmOpen(false)} onConfirm={handleUnfriendConfirm} />

      <BlockConfirmDialog open={blockConfirmOpen} user={blockObj} onClose={() => setBlockConfirmOpen(false)} onConfirm={handleBlockConfirm} />
    </Box>
  );
};

export default FriendStatusButton;
