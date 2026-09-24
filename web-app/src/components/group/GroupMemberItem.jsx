import React, { useState } from "react";
import { Paper, Box, Avatar, Typography, Chip, IconButton, Menu, MenuItem, ListItemIcon, ListItemText, Divider } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import PersonIcon from "@mui/icons-material/Person";
import PersonRemoveIcon from "@mui/icons-material/PersonRemove";
import { useNavigate } from "react-router-dom";
import { useGroup } from "../../context/GroupContext";

export const GroupMemberItem = ({ member, currentUserRole, onUpdateRole, onRemoveMember }) => {
  const navigate = useNavigate();
  const { myUserId } = useGroup();
  const [anchorEl, setAnchorEl] = useState(null);
  const openMenu = Boolean(anchorEl);

  const isSelf = member.userId === myUserId;
  const isOwner = member.role === "OWNER";
  const canManage = (currentUserRole === "OWNER" || (currentUserRole === "ADMIN" && member.role === "MEMBER")) && !isSelf && !isOwner;

  const handleMenuOpen = (e) => {
    e.stopPropagation();
    setAnchorEl(e.currentTarget);
  };

  const handleMenuClose = () => setAnchorEl(null);

  const getRoleChip = () => {
    switch (member.role) {
      case "OWNER":
        return <Chip label="Trưởng nhóm" size="small" color="secondary" sx={{ fontWeight: 700, fontSize: "0.75rem", height: 24 }} />;
      case "ADMIN":
        return <Chip label="Quản trị viên" size="small" color="primary" sx={{ fontWeight: 600, fontSize: "0.75rem", height: 24 }} />;
      case "MEMBER":
      default:
        return <Chip label="Thành viên" size="small" variant="outlined" sx={{ fontSize: "0.75rem", height: 24 }} />;
    }
  };

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2,
        mb: 1.5,
        borderRadius: 2.5,
        border: "1px solid rgba(0,0,0,0.07)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        transition: "background-color 0.2s, border-color 0.2s",
        "&:hover": { bgcolor: "rgba(0,0,0,0.015)", borderColor: "rgba(0,0,0,0.15)" },
      }}
    >
      <Box
        onClick={() => navigate(`/users/${member.userId}`)}
        sx={{ display: "flex", alignItems: "center", gap: 2, minWidth: 0, cursor: "pointer", "&:hover .member-name": { color: "primary.main" } }}
      >
        <Avatar
          src={member.user.avatar || undefined}
          alt={member.user.username}
          sx={{ width: 48, height: 48, boxShadow: "0 2px 6px rgba(0,0,0,0.1)", transition: "transform 0.15s", "&:hover": { transform: "scale(1.05)" } }}
        />
        <Box sx={{ minWidth: 0 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
            <Typography variant="subtitle2" className="member-name" sx={{ fontWeight: 700, transition: "color 0.15s" }}>
              {member.user.username}
            </Typography>
            {isSelf && <Chip label="Bạn" size="small" variant="outlined" sx={{ height: 18, fontSize: "0.65rem", fontWeight: 600 }} />}
          </Box>
          <Typography variant="caption" color="text.disabled">
            Tham gia: {member.joinedAt}
          </Typography>
        </Box>
      </Box>

      <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexShrink: 0 }}>
        {getRoleChip()}

        {canManage && (
          <>
            <IconButton size="small" onClick={handleMenuOpen} aria-label="Quản lý thành viên">
              <MoreVertIcon fontSize="small" />
            </IconButton>

            <Menu anchorEl={anchorEl} open={openMenu} onClose={handleMenuClose} slotProps={{ paper: { sx: { minWidth: 190, borderRadius: 2, boxShadow: 3 } } }}>
              {member.role === "MEMBER" && (
                <MenuItem
                  onClick={() => {
                    handleMenuClose();
                    onUpdateRole?.(member.userId, "ADMIN");
                  }}
                >
                  <ListItemIcon>
                    <AdminPanelSettingsIcon fontSize="small" color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Đặt làm Quản trị viên" />
                </MenuItem>
              )}

              {member.role === "ADMIN" && (
                <MenuItem
                  onClick={() => {
                    handleMenuClose();
                    onUpdateRole?.(member.userId, "MEMBER");
                  }}
                >
                  <ListItemIcon>
                    <PersonIcon fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Chuyển thành Thành viên" />
                </MenuItem>
              )}

              <Divider sx={{ my: 0.5 }} />

              <MenuItem
                onClick={() => {
                  handleMenuClose();
                  onRemoveMember?.(member.userId);
                }}
                sx={{ color: "error.main" }}
              >
                <ListItemIcon>
                  <PersonRemoveIcon fontSize="small" color="error" />
                </ListItemIcon>
                <ListItemText primary="Xoá khỏi nhóm" />
              </MenuItem>
            </Menu>
          </>
        )}
      </Box>
    </Paper>
  );
};

export default GroupMemberItem;
