import React, { useState } from "react";
import { Card, CardContent, Avatar, Typography, Box, IconButton, Menu, MenuItem, ListItemIcon, ListItemText, Button } from "@mui/material";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import PersonRemoveIcon from "@mui/icons-material/PersonRemove";
import BlockIcon from "@mui/icons-material/Block";
import PersonIcon from "@mui/icons-material/Person";
import { useNavigate } from "react-router-dom";

export const FriendCard = ({ friend, onOpenUnfriend, onOpenBlock }) => {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleMenuOpen = (e) => {
    e.stopPropagation();
    setAnchorEl(e.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleUnfriendClick = () => {
    handleMenuClose();
    onOpenUnfriend(friend);
  };

  const handleBlockClick = () => {
    handleMenuClose();
    onOpenBlock?.({ userId: friend.userId, username: friend.username, avatar: friend.avatar });
  };

  return (
    <Card
      sx={{
        borderRadius: 3,
        border: "1px solid rgba(0, 0, 0, 0.08)",
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.04)",
        transition: "transform 0.2s, box-shadow 0.2s, border-color 0.2s",
        "&:hover": {
          borderColor: "primary.main",
          boxShadow: "0 6px 18px rgba(25, 118, 210, 0.12)",
          transform: "translateY(-2px)",
        },
      }}
    >
      <CardContent sx={{ p: 2.5, display: "flex", alignItems: "center", justifyContent: "space-between", gap: 2 }}>
        <Box
          onClick={() => navigate(`/users/${friend.userId}`)}
          sx={{ display: "flex", alignItems: "center", gap: 2, minWidth: 0, cursor: "pointer", "&:hover .friend-username": { color: "primary.main" } }}
        >
          <Avatar
            src={friend.avatar || undefined}
            alt={friend.username}
            sx={{ width: 52, height: 52, boxShadow: "0 2px 6px rgba(0,0,0,0.1)", bgcolor: "primary.light", transition: "transform 0.15s", "&:hover": { transform: "scale(1.05)" } }}
          >
            <PersonIcon />
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="subtitle1"
              className="friend-username"
              sx={{ fontWeight: 700, lineHeight: 1.3, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", transition: "color 0.15s" }}
            >
              {friend.username}
            </Typography>
          </Box>
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexShrink: 0 }}>
          <Button
            size="small"
            variant="outlined"
            color="error"
            startIcon={<PersonRemoveIcon fontSize="small" />}
            onClick={() => onOpenUnfriend(friend)}
            sx={{ borderRadius: 2, textTransform: "none", fontWeight: 600, fontSize: "0.8rem", display: { xs: "none", sm: "inline-flex" } }}
          >
            Xoá kết bạn
          </Button>

          <IconButton size="small" onClick={handleMenuOpen} aria-label="Tùy chọn">
            <MoreVertIcon fontSize="small" />
          </IconButton>

          <Menu
            anchorEl={anchorEl}
            open={open}
            onClose={handleMenuClose}
            slotProps={{ paper: { sx: { minWidth: 170, borderRadius: 2, boxShadow: 3 } } }}
          >
            <MenuItem onClick={handleUnfriendClick} sx={{ color: "error.main", display: { xs: "flex", sm: "none" } }}>
              <ListItemIcon>
                <PersonRemoveIcon fontSize="small" color="error" />
              </ListItemIcon>
              <ListItemText primary="Xoá kết bạn" />
            </MenuItem>
            <MenuItem onClick={handleBlockClick} sx={{ color: "text.primary" }}>
              <ListItemIcon>
                <BlockIcon fontSize="small" color="error" />
              </ListItemIcon>
              <ListItemText primary="Chặn người này" />
            </MenuItem>
          </Menu>
        </Box>
      </CardContent>
    </Card>
  );
};

export default FriendCard;
