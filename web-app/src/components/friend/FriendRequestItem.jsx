import React, { useState, useEffect } from "react";
import { Paper, Box, Avatar, Typography, Button } from "@mui/material";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import { getUserProfile } from "../../services/userService";

export const FriendRequestItem = ({ request, isIncoming = true, onAccept, onReject, onCancel }) => {
  const navigate = useNavigate();
  const targetId = isIncoming ? request.senderId : request.receiverId;
  const createdAtDisplay = dayjs(request.createdAt).isValid()
    ? dayjs(request.createdAt).format("DD/MM/YYYY HH:mm")
    : request.createdAt;

  // friend-service giờ đã trả kèm senderUsername/senderAvatar ngay trong
  // FriendRequestResponse (FriendService.toEnrichedResponse gọi profile-service
  // 1 lần duy nhất khi build response, cho cả REST lẫn payload realtime), nên
  // chiều "incoming" (đang xem lời mời NHẬN được, targetId = sender) dùng
  // thẳng field có sẵn, không cần tự gọi thêm request nào nữa.
  //
  // Chiều "outgoing" (xem lời mời ĐÃ GỬI, targetId = receiver) backend chưa
  // trả receiverUsername/receiverAvatar, nên vẫn giữ cách tra thêm qua
  // profile-service (GET /profile/users/{userId}) như cũ cho riêng trường hợp này.
  const [fetchedProfile, setFetchedProfile] = useState(null);
  const [profileError, setProfileError] = useState(false);

  useEffect(() => {
    if (isIncoming) return; // đã có sẵn senderUsername/senderAvatar trong request
    let cancelled = false;
    setFetchedProfile(null);
    setProfileError(false);
    if (!targetId) return;

    getUserProfile(targetId)
      .then((response) => {
        if (!cancelled) setFetchedProfile(response?.data?.result || response?.data);
      })
      .catch(() => {
        if (!cancelled) setProfileError(true);
      });

    return () => {
      cancelled = true;
    };
  }, [isIncoming, targetId]);

  // Không fallback về targetId (UUID thô) khi thiếu username — có thể xảy ra
  // với tài khoản từng bị mất username ở profile-service (xem
  // profile-username-data-loss-bug.md). Hiện nhãn trung tính thay vì lộ ID.
  const displayName = isIncoming
    ? request.senderUsername || "Người dùng ẩn danh"
    : fetchedProfile?.username || (profileError ? "Người dùng ẩn danh" : null);

  const avatarSrc = isIncoming ? request.senderAvatar : fetchedProfile?.avatar;

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2.2,
        mb: 2,
        borderRadius: 3,
        border: "1px solid rgba(0, 0, 0, 0.08)",
        display: "flex",
        flexDirection: { xs: "column", sm: "row" },
        alignItems: { xs: "flex-start", sm: "center" },
        justifyContent: "space-between",
        gap: 2,
        transition: "box-shadow 0.2s, border-color 0.2s",
        "&:hover": { borderColor: "primary.light", boxShadow: "0 4px 14px rgba(0, 0, 0, 0.06)" },
      }}
    >
      <Box
        onClick={() => navigate(`/users/${targetId}`)}
        sx={{ display: "flex", alignItems: "center", gap: 2, minWidth: 0, cursor: "pointer", "&:hover .target-user-name": { color: "primary.main" } }}
      >
        <Avatar
          src={avatarSrc || undefined}
          sx={{
            width: 48,
            height: 48,
            bgcolor: isIncoming ? "info.light" : "warning.light",
            color: "#ffffff",
            boxShadow: "0 2px 6px rgba(0,0,0,0.1)",
            transition: "transform 0.15s",
            "&:hover": { transform: "scale(1.05)" },
          }}
        >
          <PersonOutlinedIcon />
        </Avatar>
        <Box sx={{ minWidth: 0 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
            <Typography variant="subtitle1" className="target-user-name" sx={{ fontWeight: 700, lineHeight: 1.2, transition: "color 0.15s" }}>
              {displayName
                ? `${isIncoming ? "Người gửi" : "Người nhận"}: ${displayName}`
                : `Đang tải tên...`}
            </Typography>
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 0.5 }}>
            <AccessTimeIcon sx={{ fontSize: 13, color: "text.disabled" }} />
            <Typography variant="caption" color="text.disabled">
              {createdAtDisplay}
            </Typography>
          </Box>
        </Box>
      </Box>

      <Box sx={{ display: "flex", alignItems: "center", gap: 1.2, width: { xs: "100%", sm: "auto" }, justifyContent: "flex-end", flexShrink: 0 }}>
        {isIncoming ? (
          <>
            <Button
              variant="contained"
              color="primary"
              size="small"
              startIcon={<CheckIcon />}
              onClick={() => onAccept?.(request.id)}
              sx={{ fontWeight: 700, px: 2, py: 0.8, borderRadius: 2, textTransform: "none" }}
            >
              Chấp nhận
            </Button>
            <Button
              variant="outlined"
              color="inherit"
              size="small"
              startIcon={<CloseIcon />}
              onClick={() => onReject?.(request.id)}
              sx={{
                fontWeight: 600,
                px: 2,
                py: 0.8,
                borderRadius: 2,
                textTransform: "none",
                borderColor: "rgba(0,0,0,0.18)",
                "&:hover": { borderColor: "error.main", color: "error.main" },
              }}
            >
              Từ chối
            </Button>
          </>
        ) : (
          <Button
            variant="outlined"
            color="error"
            size="small"
            startIcon={<DeleteOutlineOutlinedIcon />}
            onClick={() => onCancel?.(request.id)}
            sx={{ fontWeight: 600, px: 2, py: 0.8, borderRadius: 2, textTransform: "none" }}
          >
            Huỷ lời mời
          </Button>
        )}
      </Box>
    </Paper>
  );
};

export default FriendRequestItem;
