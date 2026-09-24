import React, { useState } from "react";
import { Popover, Box, Typography, Tabs, Tab, Button, List, ListItem, ListItemAvatar, ListItemText, Avatar, IconButton, Divider, Badge } from "@mui/material";
import { alpha } from "@mui/material/styles";
import { useNavigate } from "react-router-dom";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import HowToRegIcon from "@mui/icons-material/HowToReg";
import PersonRemoveIcon from "@mui/icons-material/PersonRemove";
import BlockIcon from "@mui/icons-material/Block";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import GroupsIcon from "@mui/icons-material/Groups";
import MailIcon from "@mui/icons-material/Mail";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import CloseIcon from "@mui/icons-material/Close";
import { useNotification } from "../../context/NotificationContext";

export const NotificationPopover = ({ anchorEl, open, onClose }) => {
  const navigate = useNavigate();
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotification();
  const [currentTab, setCurrentTab] = useState("all");

  const filteredNotifications = notifications.filter((n) => (currentTab === "unread" ? !n.isRead : true));

  const handleItemClick = (notification) => {
    markAsRead(notification.id);
    onClose();
    if (notification.targetUrl) {
      navigate(notification.targetUrl);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case "FRIEND_REQUEST_RECEIVED":
        return <PersonAddIcon sx={{ fontSize: 16, color: "#1976d2" }} />;
      case "FRIEND_REQUEST_ACCEPTED":
        return <HowToRegIcon sx={{ fontSize: 16, color: "#2e7d32" }} />;
      case "FRIEND_REQUEST_REJECTED":
      case "FRIEND_REMOVED":
        return <PersonRemoveIcon sx={{ fontSize: 16, color: "#d32f2f" }} />;
      case "FRIEND_BLOCKED":
        return <BlockIcon sx={{ fontSize: 16, color: "#d32f2f" }} />;
      case "FRIEND_UNBLOCKED":
        return <LockOpenIcon sx={{ fontSize: 16, color: "#0288d1" }} />;
      case "GROUP_INVITE":
      case "GROUP_POST":
        return <GroupsIcon sx={{ fontSize: 16, color: "#7b1fa2" }} />;
      case "MESSAGE_REQUEST_RECEIVED":
      case "MESSAGE_REQUEST_ACCEPTED":
      case "MESSAGE_REQUEST_REJECTED":
        return <ForumOutlinedIcon sx={{ fontSize: 16, color: "#ed6c02" }} />;
      default:
        return <MailIcon sx={{ fontSize: 16, color: "#1976d2" }} />;
    }
  };

  return (
    <Popover
      open={open}
      anchorEl={anchorEl}
      onClose={onClose}
      anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      transformOrigin={{ vertical: "top", horizontal: "right" }}
      slotProps={{ paper: { sx: { width: { xs: 340, sm: 400 }, maxHeight: 520, borderRadius: 3, boxShadow: "0 8px 32px rgba(0, 0, 0, 0.15)", display: "flex", flexDirection: "column", overflow: "hidden" } } }}
    >
      <Box sx={{ p: 2, pb: 1, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 800 }}>
            Thông báo
          </Typography>
          {unreadCount > 0 && (
            <Box sx={{ bgcolor: "error.main", color: "#fff", borderRadius: "12px", px: 1, py: 0.1, fontSize: "0.75rem", fontWeight: 700 }}>{unreadCount} mới</Box>
          )}
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
          {unreadCount > 0 && (
            <Button size="small" startIcon={<DoneAllIcon sx={{ fontSize: 16 }} />} onClick={markAllAsRead} sx={{ fontSize: "0.75rem", fontWeight: 600, textTransform: "none", py: 0.2, px: 1, borderRadius: 1.5 }}>
              Đã đọc tất cả
            </Button>
          )}
          <IconButton size="small" onClick={onClose}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>
      </Box>

      <Box sx={{ px: 2, borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
        <Tabs
          value={currentTab}
          onChange={(_, v) => setCurrentTab(v)}
          sx={{ minHeight: 36, "& .MuiTab-root": { minHeight: 36, py: 0.5, px: 1.5, fontSize: "0.8rem", fontWeight: 700, textTransform: "none" } }}
        >
          <Tab value="all" label={`Tất cả (${notifications.length})`} />
          <Tab
            value="unread"
            label={
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.6 }}>
                <span>Chưa đọc</span>
                {unreadCount > 0 && <Badge badgeContent={unreadCount} color="error" sx={{ "& .MuiBadge-badge": { fontSize: "0.65rem", height: 16, minWidth: 16 } }} />}
              </Box>
            }
          />
        </Tabs>
      </Box>

      <Box sx={{ overflowY: "auto", flex: 1, maxHeight: 380 }}>
        {filteredNotifications.length === 0 ? (
          <Box sx={{ py: 6, textAlign: "center", px: 3 }}>
            <NotificationsNoneIcon sx={{ fontSize: 44, color: "text.disabled", mb: 1 }} />
            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
              {currentTab === "unread" ? "Không có thông báo chưa đọc nào" : "Bạn chưa có thông báo nào"}
            </Typography>
          </Box>
        ) : (
          <List disablePadding>
            {filteredNotifications.map((notification, idx) => {
              const isUnread = !notification.isRead;
              return (
                <React.Fragment key={notification.id}>
                  <ListItem
                    onClick={() => handleItemClick(notification)}
                    sx={{ px: 2, py: 1.5, cursor: "pointer", bgcolor: isUnread ? alpha("#1976d2", 0.05) : "transparent", transition: "background-color 0.15s", "&:hover": { bgcolor: isUnread ? alpha("#1976d2", 0.1) : "action.hover" }, position: "relative" }}
                  >
                    <ListItemAvatar sx={{ minWidth: 46 }}>
                      <Badge
                        overlap="circular"
                        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
                        badgeContent={
                          <Box sx={{ width: 20, height: 20, borderRadius: "50%", bgcolor: "#ffffff", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 1px 4px rgba(0,0,0,0.2)" }}>
                            {getNotificationIcon(notification.type)}
                          </Box>
                        }
                      >
                        <Avatar sx={{ width: 40, height: 40 }} />
                      </Badge>
                    </ListItemAvatar>

                    <ListItemText
                      primary={
                        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 1 }}>
                          <Typography variant="subtitle2" sx={{ fontWeight: isUnread ? 700 : 600, fontSize: "0.85rem", lineHeight: 1.2, color: isUnread ? "text.primary" : "text.secondary" }}>
                            {notification.title}
                          </Typography>
                          {isUnread && <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "primary.main", flexShrink: 0 }} />}
                        </Box>
                      }
                      secondary={
                        <Box sx={{ mt: 0.3 }}>
                          <Typography variant="body2" sx={{ fontSize: "0.78rem", color: "text.secondary", lineHeight: 1.35, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                            {notification.content}
                          </Typography>
                          <Typography variant="caption" color="text.disabled" sx={{ fontSize: "0.7rem", mt: 0.3, display: "block" }}>
                            {notification.createdAt}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                  {idx < filteredNotifications.length - 1 && <Divider />}
                </React.Fragment>
              );
            })}
          </List>
        )}
      </Box>

      <Divider />
      <Box sx={{ p: 1, textAlign: "center", bgcolor: "background.default" }}>
        <Typography variant="caption" color="text.secondary">
          Đã đồng bộ với lịch sử thông báo đã lưu
        </Typography>
      </Box>
    </Popover>
  );
};

export default NotificationPopover;
