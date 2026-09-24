import React from "react";
import { Card, CardContent, Avatar, Typography, Box, Button } from "@mui/material";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import PersonOffOutlinedIcon from "@mui/icons-material/PersonOffOutlined";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";

export const BlockedUserCard = ({ block, onUnblock }) => {
  const navigate = useNavigate();
  const blockedAtDisplay = dayjs(block.createdAt).isValid() ? dayjs(block.createdAt).format("DD/MM/YYYY HH:mm") : block.createdAt;

  return (
    <Card
      sx={{
        borderRadius: 3,
        border: "1px solid rgba(0, 0, 0, 0.08)",
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.04)",
        transition: "transform 0.2s, box-shadow 0.2s, border-color 0.2s",
        "&:hover": {
          borderColor: "warning.main",
          boxShadow: "0 4px 14px rgba(237, 108, 2, 0.1)",
          transform: "translateY(-2px)",
        },
      }}
    >
      <CardContent sx={{ p: 2.5, display: "flex", alignItems: "center", justifyContent: "space-between", gap: 2 }}>
        <Box
          onClick={() => navigate(`/users/${block.userId}`)}
          sx={{ display: "flex", alignItems: "center", gap: 2, minWidth: 0, cursor: "pointer", "&:hover .blocked-user-name": { color: "primary.main" } }}
        >
          <Avatar
            src={block.avatar || undefined}
            alt={block.username}
            sx={{ width: 52, height: 52, boxShadow: "0 2px 6px rgba(0,0,0,0.1)", bgcolor: "action.disabledBackground", color: "text.secondary", transition: "transform 0.15s", "&:hover": { transform: "scale(1.05)" } }}
          >
            <PersonOffOutlinedIcon />
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="subtitle1" className="blocked-user-name" sx={{ fontWeight: 700, lineHeight: 1.3, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", transition: "color 0.15s" }}>
              {block.username}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
              ID: {block.userId}
            </Typography>
            {block.createdAt && (
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 0.3 }}>
                <AccessTimeIcon sx={{ fontSize: 12, color: "text.disabled" }} />
                <Typography variant="caption" color="text.disabled">
                  Chặn lúc: {blockedAtDisplay}
                </Typography>
              </Box>
            )}
          </Box>
        </Box>

        <Box sx={{ flexShrink: 0 }}>
          <Button
            size="small"
            variant="outlined"
            color="primary"
            startIcon={<LockOpenIcon fontSize="small" />}
            onClick={() => onUnblock(block.userId)}
            sx={{ borderRadius: 2, textTransform: "none", fontWeight: 600, fontSize: "0.85rem", px: 2 }}
          >
            Bỏ chặn
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
};

export default BlockedUserCard;
