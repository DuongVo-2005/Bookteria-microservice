import React, { useState } from "react";
import { Paper, Box, Avatar, Typography, IconButton, Button, TextField, Collapse, Menu, MenuItem, ListItemIcon, ListItemText, CircularProgress } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderIcon from "@mui/icons-material/FavoriteBorder";
import ChatBubbleOutlineOutlinedIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import SendIcon from "@mui/icons-material/Send";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import FlagOutlinedIcon from "@mui/icons-material/FlagOutlined";
import { useNavigate } from "react-router-dom";
import { useGroup } from "../../context/GroupContext";
import { ReportDialog } from "../report/ReportDialog";

export const GroupPostCard = ({ post, onDeleted }) => {
  const navigate = useNavigate();
  const { toggleLikePost, addComment, deletePost, deleteComment, myUserId, isAdmin } = useGroup();

  const [commentOpen, setCommentOpen] = useState(false);
  const [commentInput, setCommentInput] = useState("");
  const [menuAnchorEl, setMenuAnchorEl] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleted, setDeleted] = useState(false);
  // { targetType: "POST"|"COMMENT", targetId, targetLabel } | null
  const [reportTarget, setReportTarget] = useState(null);

  const isOwnPost = post.author?.userId === myUserId;
  const canDeletePost = isOwnPost || isAdmin;

  const handleDeletePost = () => {
    setMenuAnchorEl(null);
    setDeleting(true);
    deletePost(post.groupId, post.id)
      .then(() => {
        setDeleted(true);
        onDeleted?.(post.id);
      })
      .catch(() => {})
      .finally(() => setDeleting(false));
  };

  // Toggle like/thêm comment cập nhật lạc quan ở state cục bộ của post này
  // (khớp ngay trên UI), rồi mới gọi API thật — rollback nếu API lỗi.
  const [isLiked, setIsLiked] = useState(post.isLiked);
  const [likesCount, setLikesCount] = useState(post.likesCount);
  const [comments, setComments] = useState(post.comments || []);
  const [commentsCount, setCommentsCount] = useState(post.commentsCount);

  const handleLike = () => {
    const nextLiked = !isLiked;
    setIsLiked(nextLiked);
    setLikesCount((c) => Math.max(0, nextLiked ? c + 1 : c - 1));
    toggleLikePost(post.groupId, post.id).catch(() => {
      setIsLiked(!nextLiked);
      setLikesCount((c) => Math.max(0, nextLiked ? c - 1 : c + 1));
    });
  };

  const handleBookClick = () => {
    // BA Backlog GAP-02 (be-report.md, bổ sung 2026-09-18): sách đã bị Admin
    // xoá khỏi catalog thì bookRef.deleted=true — không còn trang /books/:id
    // thật để điều hướng tới.
    if (post.bookRef && !post.bookRef.deleted) navigate(`/books/${post.bookRef.bookId}`);
  };

  const handleCommentSubmit = (e) => {
    e.preventDefault();
    if (!commentInput.trim()) return;
    const text = commentInput.trim();
    setCommentInput("");
    addComment(post.groupId, post.id, text)
      .then((newComment) => {
        if (!newComment) return;
        setComments((prev) => [...prev, newComment]);
        setCommentsCount((c) => c + 1);
      })
      .catch(() => {});
  };

  const handleDeleteComment = (commentId) => {
    deleteComment(post.groupId, post.id, commentId)
      .then(() => {
        setComments((prev) => prev.filter((c) => c.id !== commentId));
        setCommentsCount((c) => Math.max(0, c - 1));
      })
      .catch(() => {});
  };

  if (deleted) return null;

  return (
    <Paper elevation={0} sx={{ p: 2.5, mb: 2.5, borderRadius: 3, border: "1px solid rgba(0, 0, 0, 0.08)", boxShadow: "0 2px 8px rgba(0, 0, 0, 0.03)" }}>
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.8, mb: 2 }}>
        <Avatar src={post.author.avatar || undefined} alt={post.author.username} sx={{ width: 46, height: 46, boxShadow: "0 2px 6px rgba(0,0,0,0.1)" }} />
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            {post.author.username}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {post.createdAt}
          </Typography>
        </Box>
        <IconButton size="small" onClick={(e) => setMenuAnchorEl(e.currentTarget)} disabled={deleting} aria-label="Tuỳ chọn khác">
          {deleting ? <CircularProgress size={16} /> : <MoreVertIcon fontSize="small" />}
        </IconButton>
        <Menu anchorEl={menuAnchorEl} open={Boolean(menuAnchorEl)} onClose={() => setMenuAnchorEl(null)}>
          {canDeletePost && (
            <MenuItem onClick={handleDeletePost} sx={{ color: "error.main" }}>
              <ListItemIcon>
                <DeleteOutlineIcon fontSize="small" color="error" />
              </ListItemIcon>
              <ListItemText primary="Xoá bài viết" />
            </MenuItem>
          )}
          {!isOwnPost && (
            <MenuItem
              onClick={() => {
                setMenuAnchorEl(null);
                setReportTarget({ targetType: "POST", targetId: post.id, targetLabel: `Bài viết của ${post.author.username}` });
              }}
            >
              <ListItemIcon>
                <FlagOutlinedIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Báo cáo bài viết" />
            </MenuItem>
          )}
        </Menu>
      </Box>

      <Typography variant="body1" sx={{ fontSize: "0.95rem", lineHeight: 1.6, color: "text.primary", whiteSpace: "pre-line", mb: 2 }}>
        {post.content}
      </Typography>

      {post.bookRef && (
        <Paper
          elevation={0}
          onClick={handleBookClick}
          sx={{
            p: 1.5,
            mb: 2,
            borderRadius: 2.5,
            bgcolor: post.bookRef.deleted ? "rgba(0,0,0,0.03)" : "rgba(25, 118, 210, 0.04)",
            border: post.bookRef.deleted ? "1px dashed rgba(0,0,0,0.15)" : "1px solid rgba(25, 118, 210, 0.15)",
            display: "flex",
            alignItems: "center",
            gap: 2,
            cursor: post.bookRef.deleted ? "default" : "pointer",
            transition: "all 0.2s",
            ...(!post.bookRef.deleted && { "&:hover": { bgcolor: "rgba(25, 118, 210, 0.08)", borderColor: "primary.main", boxShadow: "0 2px 8px rgba(25, 118, 210, 0.1)" } }),
          }}
        >
          {post.bookRef.deleted ? (
            <Box sx={{ width: 45, height: 65, bgcolor: "action.disabledBackground", borderRadius: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <AutoStoriesIcon sx={{ color: "text.disabled", fontSize: 20 }} />
            </Box>
          ) : post.bookRef.coverImage ? (
            <Box component="img" src={post.bookRef.coverImage} alt={post.bookRef.bookTitle} sx={{ width: 45, height: 65, objectFit: "cover", borderRadius: 1, boxShadow: 1 }} />
          ) : (
            <Box sx={{ width: 45, height: 65, bgcolor: "primary.light", borderRadius: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <AutoStoriesIcon sx={{ color: "#fff", fontSize: 20 }} />
            </Box>
          )}

          <Box sx={{ minWidth: 0 }}>
            {post.bookRef.deleted ? (
              <Typography variant="body2" color="text.disabled" sx={{ fontStyle: "italic" }}>
                Sách không còn tồn tại
              </Typography>
            ) : (
              <>
                <Typography variant="caption" color="primary.main" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
                  SÁCH ĐƯỢC NHẮC ĐẾN
                </Typography>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
                  {post.bookRef.bookTitle}
                </Typography>
                {post.bookRef.authorName && (
                  <Typography variant="caption" color="text.secondary">
                    {post.bookRef.authorName}
                  </Typography>
                )}
              </>
            )}
          </Box>
        </Paper>
      )}

      <Box sx={{ display: "flex", alignItems: "center", gap: 3, pt: 1.5, borderTop: "1px solid rgba(0, 0, 0, 0.06)" }}>
        <Button
          size="small"
          startIcon={isLiked ? <FavoriteIcon sx={{ color: "error.main" }} /> : <FavoriteBorderIcon sx={{ color: "text.secondary" }} />}
          onClick={handleLike}
          sx={{ fontWeight: 600, color: isLiked ? "error.main" : "text.secondary", textTransform: "none" }}
        >
          {likesCount > 0 ? `${likesCount} Thích` : "Thích"}
        </Button>

        <Button size="small" startIcon={<ChatBubbleOutlineOutlinedIcon sx={{ color: "text.secondary" }} />} onClick={() => setCommentOpen(!commentOpen)} sx={{ fontWeight: 600, color: "text.secondary", textTransform: "none" }}>
          {commentsCount > 0 ? `${commentsCount} Bình luận` : "Bình luận"}
        </Button>
      </Box>

      <Collapse in={commentOpen || comments.length > 0}>
        <Box sx={{ mt: 2, pt: 2, borderTop: "1px dashed rgba(0,0,0,0.08)" }}>
          {comments.length > 0 && (
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5, mb: 2 }}>
              {comments.map((comm) => {
                const isOwnComment = comm.author?.userId === myUserId;
                return (
                  <Box key={comm.id} sx={{ display: "flex", gap: 1.5, alignItems: "flex-start" }}>
                    <Avatar src={comm.author.avatar || undefined} alt={comm.author.username} sx={{ width: 32, height: 32 }} />
                    <Box sx={{ bgcolor: "#f4f6f8", p: 1.2, px: 1.8, borderRadius: 2.5, flexGrow: 1 }}>
                      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                        <Typography variant="caption" sx={{ fontWeight: 700 }}>
                          {comm.author.username}
                        </Typography>
                        <Typography variant="caption" color="text.disabled" sx={{ fontSize: "0.7rem" }}>
                          {comm.createdAt}
                        </Typography>
                      </Box>
                      <Typography variant="body2" sx={{ fontSize: "0.85rem", mt: 0.3 }}>
                        {comm.content}
                      </Typography>
                      <Box sx={{ display: "flex", gap: 1.5, mt: 0.3 }}>
                        {(isOwnComment || isAdmin) && (
                          <Button
                            size="small"
                            onClick={() => handleDeleteComment(comm.id)}
                            sx={{ p: 0, minWidth: 0, fontSize: "0.68rem", fontWeight: 600, color: "text.disabled", textTransform: "none", "&:hover": { bgcolor: "transparent", color: "error.main" } }}
                          >
                            Xoá
                          </Button>
                        )}
                        {!isOwnComment && (
                          <Button
                            size="small"
                            onClick={() => setReportTarget({ targetType: "COMMENT", targetId: comm.id, targetLabel: `Bình luận của ${comm.author.username}` })}
                            sx={{ p: 0, minWidth: 0, fontSize: "0.68rem", fontWeight: 600, color: "text.disabled", textTransform: "none", "&:hover": { bgcolor: "transparent", color: "text.secondary" } }}
                          >
                            Báo cáo
                          </Button>
                        )}
                      </Box>
                    </Box>
                  </Box>
                );
              })}
            </Box>
          )}

          <Box component="form" onSubmit={handleCommentSubmit} sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
            <TextField
              size="small"
              fullWidth
              placeholder="Viết câu trả lời hoặc bình luận…"
              value={commentInput}
              onChange={(e) => setCommentInput(e.target.value)}
              slotProps={{ input: { sx: { borderRadius: 2.5, bgcolor: "#fafafa", fontSize: "0.85rem" } } }}
            />
            <IconButton type="submit" color="primary" disabled={!commentInput.trim()} size="small" aria-label="Gửi">
              <SendIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
      </Collapse>

      <ReportDialog
        open={Boolean(reportTarget)}
        onClose={() => setReportTarget(null)}
        targetType={reportTarget?.targetType}
        targetId={reportTarget?.targetId}
        targetLabel={reportTarget?.targetLabel}
      />
    </Paper>
  );
};

export default GroupPostCard;
