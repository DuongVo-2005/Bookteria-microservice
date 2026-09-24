import React, { useState } from "react";
import { Card, CardMedia, CardContent, Avatar, Typography, Box, Button, Chip, IconButton, Menu, MenuItem, ListItemIcon, ListItemText } from "@mui/material";
import GroupsIcon from "@mui/icons-material/Groups";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PublicIcon from "@mui/icons-material/Public";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import LogoutIcon from "@mui/icons-material/Logout";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import { useNavigate } from "react-router-dom";
import { useGroup } from "../../context/GroupContext";

export const GroupCard = ({ group, onOpenLeaveDialog }) => {
  const navigate = useNavigate();
  const { joinGroup } = useGroup();
  const [anchorEl, setAnchorEl] = useState(null);
  const openMenu = Boolean(anchorEl);

  const handleCardClick = () => navigate(`/groups/${group.id}`);

  const handleMenuOpen = (e) => {
    e.stopPropagation();
    setAnchorEl(e.currentTarget);
  };

  const handleMenuClose = () => setAnchorEl(null);

  const handleJoinClick = (e) => {
    e.stopPropagation();
    joinGroup(group.id);
  };

  const handleLeaveClick = () => {
    handleMenuClose();
    onOpenLeaveDialog?.(group);
  };

  return (
    <Card
      onClick={handleCardClick}
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        borderRadius: 3,
        border: "1px solid rgba(0, 0, 0, 0.08)",
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.04)",
        cursor: "pointer",
        transition: "transform 0.2s, box-shadow 0.2s, border-color 0.2s",
        overflow: "hidden",
        "&:hover": { borderColor: "primary.main", boxShadow: "0 8px 24px rgba(25, 118, 210, 0.15)", transform: "translateY(-3px)" },
      }}
    >
      <Box sx={{ position: "relative", height: 110, bgcolor: "#e0e0e0" }}>
        <CardMedia component="img" height="110" image={group.coverImage} alt={group.name} sx={{ objectFit: "cover" }} />
        <Chip
          icon={group.visibility === "PUBLIC" ? <PublicIcon sx={{ fontSize: "14px !important", color: "#fff !important" }} /> : <LockOutlinedIcon sx={{ fontSize: "14px !important", color: "#fff !important" }} />}
          label={group.visibility === "PUBLIC" ? "Công khai" : "Riêng tư"}
          size="small"
          sx={{ position: "absolute", top: 10, right: 10, bgcolor: "rgba(0, 0, 0, 0.65)", color: "#ffffff", fontWeight: 600, fontSize: "0.7rem", backdropFilter: "blur(4px)" }}
        />
      </Box>

      <CardContent sx={{ p: 2.5, pt: 0, display: "flex", flexDirection: "column", flexGrow: 1 }}>
        <Box sx={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", mt: -3.5, mb: 1.5 }}>
          <Avatar src={group.avatar} alt={group.name} variant="rounded" sx={{ width: 64, height: 64, borderRadius: 2.5, border: "3px solid #ffffff", boxShadow: "0 3px 10px rgba(0,0,0,0.15)", bgcolor: "primary.main" }} />

          {group.isJoined && (
            <IconButton size="small" onClick={handleMenuOpen} aria-label="Tùy chọn nhóm">
              <MoreVertIcon fontSize="small" />
            </IconButton>
          )}

          <Menu anchorEl={anchorEl} open={openMenu} onClose={handleMenuClose} slotProps={{ paper: { sx: { minWidth: 160, borderRadius: 2, boxShadow: 3 } } }}>
            <MenuItem onClick={handleCardClick}>
              <ListItemIcon>
                <ArrowForwardIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Vào nhóm" />
            </MenuItem>
            {group.currentUserRole !== "OWNER" && (
              <MenuItem onClick={handleLeaveClick} sx={{ color: "error.main" }}>
                <ListItemIcon>
                  <LogoutIcon fontSize="small" color="error" />
                </ListItemIcon>
                <ListItemText primary="Rời nhóm" />
              </MenuItem>
            )}
          </Menu>
        </Box>

        <Typography
          variant="subtitle1"
          sx={{ fontWeight: 700, lineHeight: 1.3, mb: 0.5, overflow: "hidden", textOverflow: "ellipsis", display: "-webkit-box", WebkitLineClamp: 1, WebkitBoxOrient: "vertical", "&:hover": { color: "primary.main" } }}
        >
          {group.name}
        </Typography>

        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1.5, flexWrap: "wrap" }}>
          <Chip label={group.category} size="small" variant="outlined" sx={{ fontSize: "0.7rem", height: 22, borderColor: "primary.light", color: "primary.dark", fontWeight: 600 }} />
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <GroupsIcon sx={{ fontSize: 16, color: "text.secondary" }} />
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
              {group.memberCount} thành viên
            </Typography>
          </Box>
        </Box>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ fontSize: "0.85rem", lineHeight: 1.4, mb: 2, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden", minHeight: 38 }}
        >
          {group.description}
        </Typography>

        <Box sx={{ mt: "auto", pt: 1.5, borderTop: "1px solid rgba(0,0,0,0.06)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          {group.currentUserRole ? (
            <Chip
              label={group.currentUserRole === "OWNER" ? "Trưởng nhóm" : group.currentUserRole === "ADMIN" ? "Quản trị viên" : "Thành viên"}
              size="small"
              color={group.currentUserRole === "OWNER" ? "secondary" : "default"}
              sx={{ fontWeight: 600, fontSize: "0.75rem", height: 24 }}
            />
          ) : (
            <Typography variant="caption" color="text.disabled">
              {group.createdAt}
            </Typography>
          )}

          {!group.isJoined ? (
            <Button variant="contained" color="primary" size="small" onClick={handleJoinClick} sx={{ borderRadius: 2, px: 2, py: 0.6, fontWeight: 700, fontSize: "0.8rem" }}>
              Tham gia
            </Button>
          ) : (
            <Button variant="outlined" color="primary" size="small" startIcon={<CheckCircleIcon sx={{ fontSize: "15px !important" }} />} onClick={handleCardClick} sx={{ borderRadius: 2, px: 1.5, py: 0.6, fontWeight: 600, fontSize: "0.8rem", borderWidth: 1.5 }}>
              Đã tham gia
            </Button>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default GroupCard;
