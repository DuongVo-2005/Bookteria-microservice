import React from "react";
import { Paper, Box, Typography, Chip } from "@mui/material";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import TagIcon from "@mui/icons-material/Tag";
import { useNavigate } from "react-router-dom";

// PostSearchResponse (be-report.md §22.1) chỉ có { id, content, authorId,
// hashtags, bookId, createdDate } — KHÔNG có username/likesCount/visibility
// như PostResponse đầy đủ, và chưa có trang xem 1 post riêng lẻ (đã ghi ở
// Known Issues Phase 3/4) — nên đây là card xem trước thuần (không Thích/
// Bình luận/Chia sẻ như Post.jsx), chỉ điều hướng được tới sách gắn kèm và
// bấm hashtag để lọc lại kết quả.
export const PostSearchResultCard = ({ post, onHashtagClick }) => {
  const navigate = useNavigate();

  return (
    <Paper elevation={0} sx={{ p: 2, mb: 2, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.08)" }}>
      <Typography variant="body2" sx={{ whiteSpace: "pre-line", mb: post.hashtags?.length || post.bookId ? 1.2 : 0 }}>
        {post.content}
      </Typography>

      {post.hashtags?.length > 0 && (
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.7, mb: post.bookId ? 1.2 : 0 }}>
          {post.hashtags.map((tag) => (
            <Chip key={tag} icon={<TagIcon sx={{ fontSize: "14px !important" }} />} label={tag} size="small" clickable onClick={() => onHashtagClick?.(tag)} sx={{ height: 24, fontSize: "0.72rem" }} />
          ))}
        </Box>
      )}

      {post.bookId && (
        <Chip
          icon={<AutoStoriesIcon sx={{ fontSize: "14px !important" }} />}
          label="Xem sách được nhắc đến"
          size="small"
          clickable
          color="primary"
          variant="outlined"
          onClick={() => navigate(`/books/${post.bookId}`)}
          sx={{ height: 24, fontSize: "0.72rem" }}
        />
      )}

      {post.createdDate && (
        <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 1 }}>
          {post.createdDate}
        </Typography>
      )}
    </Paper>
  );
};

export default PostSearchResultCard;
