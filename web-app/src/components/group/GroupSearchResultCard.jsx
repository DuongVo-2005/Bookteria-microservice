import React from "react";
import { Paper, Box, Avatar, Typography, Chip } from "@mui/material";
import GroupsIcon from "@mui/icons-material/Groups";
import { useNavigate } from "react-router-dom";

// GroupSearchResponse (be-report.md §22.1) chỉ có { id, name, description,
// category, createdAt } — thiếu avatar/memberCount/visibility/isJoined mà
// GroupCard.jsx (dùng ở trang Nhóm, gắn liền useGroup() context) cần, nên
// dùng card riêng, thuần điều hướng tới /groups/:id (trang đó tự tải đủ
// thông tin, không phụ thuộc dữ liệu preview ở đây).
export const GroupSearchResultCard = ({ group }) => {
  const navigate = useNavigate();

  return (
    <Paper
      elevation={0}
      onClick={() => navigate(`/groups/${group.id}`)}
      sx={{ p: 2, mb: 2, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.08)", cursor: "pointer", display: "flex", gap: 2, alignItems: "center", transition: "box-shadow 0.2s", "&:hover": { boxShadow: "0 4px 12px rgba(0,0,0,0.06)" } }}
    >
      <Avatar variant="rounded" sx={{ width: 52, height: 52, borderRadius: 2, bgcolor: "primary.light" }}>
        <GroupsIcon />
      </Avatar>
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 700 }} noWrap>
          {group.name}
        </Typography>
        {group.description && (
          <Typography variant="body2" color="text.secondary" sx={{ display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden", fontSize: "0.85rem" }}>
            {group.description}
          </Typography>
        )}
        {group.category && <Chip label={group.category} size="small" sx={{ mt: 0.7, height: 20, fontSize: "0.68rem", bgcolor: "rgba(25, 118, 210, 0.1)", color: "primary.dark" }} />}
      </Box>
    </Paper>
  );
};

export default GroupSearchResultCard;
