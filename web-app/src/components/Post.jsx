import React, { forwardRef, useEffect, useState } from "react";
import { Box, Avatar, Typography, IconButton, Button, TextField, Collapse, Chip, Paper, Dialog, DialogTitle, DialogContent, DialogActions, CircularProgress, Menu, MenuItem, ListItemIcon, ListItemText } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderIcon from "@mui/icons-material/FavoriteBorder";
import ChatBubbleOutlineOutlinedIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import SendIcon from "@mui/icons-material/Send";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import RepeatIcon from "@mui/icons-material/Repeat";
import PublicIcon from "@mui/icons-material/Public";
import PeopleIcon from "@mui/icons-material/People";
import LockIcon from "@mui/icons-material/Lock";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import FlagOutlinedIcon from "@mui/icons-material/FlagOutlined";
import { useNavigate } from "react-router-dom";
import { toggleLikePost, getPostComments, addPostComment, sharePost, getPostById, deletePost } from "../services/postService";
import { getPostErrorMessage } from "../services/postErrorMessages";
import { useToast } from "../context/ToastContext";
import { getCurrentUserId, isAdmin } from "../services/authenticationService";
import { ReportDialog } from "./report/ReportDialog";

const VISIBILITY_META = {
  PUBLIC: { icon: <PublicIcon sx={{ fontSize: 13 }} />, label: "Công khai" },
  FRIENDS: { icon: <PeopleIcon sx={{ fontSize: 13 }} />, label: "Bạn bè" },
  PRIVATE: { icon: <LockIcon sx={{ fontSize: 13 }} />, label: "Riêng tư" },
};

// Bài gốc được nhắc tới khi post này là 1 lượt share/repost (sharedFromPostId
// khác null) — PostResponse thật chỉ trả về ID, không nhúng sẵn nội dung bài
// gốc, nên phải tự gọi GET /post/{id} riêng để hiển thị.
const SharedOriginalPost = ({ postId }) => {
  const [original, setOriginal] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    getPostById(postId)
      .then((res) => {
        if (!cancelled) setOriginal(res?.data?.result || null);
      })
      .catch(() => {
        if (!cancelled) setNotFound(true);
      });
    return () => {
      cancelled = true;
    };
  }, [postId]);

  if (notFound) {
    return (
      <Paper elevation={0} sx={{ p: 1.5, borderRadius: 2, border: "1px dashed rgba(0,0,0,0.15)", bgcolor: "#fafafa" }}>
        <Typography variant="caption" color="text.secondary">
          Bài viết gốc không còn tồn tại hoặc bạn không có quyền xem.
        </Typography>
      </Paper>
    );
  }

  if (!original) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", p: 1.5 }}>
        <CircularProgress size={18} />
      </Box>
    );
  }

  return (
    <Paper
      elevation={0}
      onClick={() => original.bookId && navigate(`/books/${original.bookId}`)}
      sx={{ p: 1.5, borderRadius: 2, border: "1px solid rgba(0,0,0,0.1)", bgcolor: "#fafafa" }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}>
        <Avatar sx={{ width: 26, height: 26 }} />
        <Typography variant="caption" sx={{ fontWeight: 700 }}>
          {original.username}
        </Typography>
      </Box>
      <Typography variant="body2" sx={{ fontSize: "0.85rem", whiteSpace: "pre-line" }}>
        {original.content}
      </Typography>
    </Paper>
  );
};

const Post = forwardRef((props, ref) => {
  const navigate = useNavigate();
  const { showError, showSuccess } = useToast();
  const post = props.post;
  const { id, avatar, username, created, content, visibility, bookId, bookTitle, bookCoverImage, sharedFromPostId, userId, hashtags } = post;

  const [isLiked, setIsLiked] = useState(post.liked || false);
  const [likesCount, setLikesCount] = useState(post.likesCount || 0);
  const [commentsCount, setCommentsCount] = useState(post.commentsCount || 0);
  const [commentOpen, setCommentOpen] = useState(false);
  const [commentsLoaded, setCommentsLoaded] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentInput, setCommentInput] = useState("");
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [shareContent, setShareContent] = useState("");
  const [sharing, setSharing] = useState(false);
  const [menuAnchorEl, setMenuAnchorEl] = useState(null);
  const [deleting, setDeleting] = useState(false);
  // { targetType: "POST"|"COMMENT", targetId, targetLabel } | null — dùng
  // chung 1 ReportDialog cho cả báo cáo bài viết lẫn báo cáo bình luận.
  const [reportTarget, setReportTarget] = useState(null);
  const [deleted, setDeleted] = useState(false);

  const visMeta = VISIBILITY_META[visibility] || VISIBILITY_META.PUBLIC;
  // Phase 4 (be-report.md, 2026-09-18): tác giả HOẶC platform ADMIN mới xoá
  // được post — không phải tác giả thì hiện "Báo cáo" thay vì "Xoá".
  const isOwnPost = userId && userId === getCurrentUserId();
  const canDelete = isOwnPost || isAdmin();

  const handleDeletePost = () => {
    setMenuAnchorEl(null);
    setDeleting(true);
    deletePost(id)
      .then(() => {
        showSuccess("Đã xoá bài viết.");
        setDeleted(true);
        props.onDeleted?.(id);
      })
      .catch((err) => showError(getPostErrorMessage(err)))
      .finally(() => setDeleting(false));
  };

  if (deleted) return null;

  const handleLike = () => {
    const nextLiked = !isLiked;
    setIsLiked(nextLiked);
    setLikesCount((c) => Math.max(0, nextLiked ? c + 1 : c - 1));
    toggleLikePost(id).catch((err) => {
      setIsLiked(!nextLiked);
      setLikesCount((c) => Math.max(0, nextLiked ? c - 1 : c + 1));
      showError(getPostErrorMessage(err));
    });
  };

  const handleToggleComments = () => {
    const opening = !commentOpen;
    setCommentOpen(opening);
    if (opening && !commentsLoaded) {
      getPostComments(id, 0, 20)
        .then((res) => {
          setComments(res?.data?.result?.data || []);
          setCommentsLoaded(true);
        })
        .catch((err) => showError(getPostErrorMessage(err)));
    }
  };

  const handleCommentSubmit = (e) => {
    e.preventDefault();
    const text = commentInput.trim();
    if (!text) return;
    setCommentInput("");
    addPostComment(id, text)
      .then((res) => {
        const newComment = res?.data?.result;
        if (newComment) setComments((prev) => [...prev, newComment]);
        setCommentsCount((c) => c + 1);
      })
      .catch((err) => showError(getPostErrorMessage(err)));
  };

  const handleShareSubmit = () => {
    setSharing(true);
    sharePost(id, shareContent)
      .then(() => {
        showSuccess("Đã chia sẻ bài viết lên Bảng tin của bạn.");
        setShareDialogOpen(false);
        setShareContent("");
      })
      .catch((err) => showError(getPostErrorMessage(err)))
      .finally(() => setSharing(false));
  };

  return (
    <Box ref={ref} sx={{ width: "100%", py: 1.5, borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
      <Box sx={{ display: "flex", flexDirection: "row", alignItems: "start", mb: 1.2 }}>
        <Avatar src={avatar} sx={{ marginRight: 2 }} />
        <Box sx={{ minWidth: 0, flex: 1 }}>
          <Box sx={{ display: "flex", flexDirection: "row", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
            <Typography sx={{ fontSize: 14, fontWeight: 600 }}>{username}</Typography>
            <Typography sx={{ fontSize: 13, color: "text.secondary" }}>{created}</Typography>
            <Chip icon={visMeta.icon} label={visMeta.label} size="small" variant="outlined" sx={{ height: 20, fontSize: "0.68rem", "& .MuiChip-icon": { ml: "6px" } }} />
          </Box>
          <Typography sx={{ fontSize: 14, mt: 0.5, whiteSpace: "pre-line" }}>{content}</Typography>
          {hashtags?.length > 0 && (
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.6, mt: 0.8 }}>
              {hashtags.map((tag) => (
                <Chip key={tag} label={tag} size="small" clickable onClick={() => navigate(`/search?hashtag=${encodeURIComponent(tag)}`)} sx={{ height: 22, fontSize: "0.7rem" }} />
              ))}
            </Box>
          )}
        </Box>
        <IconButton size="small" onClick={(e) => setMenuAnchorEl(e.currentTarget)} disabled={deleting} aria-label="Tuỳ chọn khác">
          {deleting ? <CircularProgress size={16} /> : <MoreVertIcon fontSize="small" />}
        </IconButton>
        <Menu anchorEl={menuAnchorEl} open={Boolean(menuAnchorEl)} onClose={() => setMenuAnchorEl(null)}>
          {canDelete && (
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
                setReportTarget({ targetType: "POST", targetId: id, targetLabel: `Bài viết của ${username}` });
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

      {sharedFromPostId && (
        <Box sx={{ mb: 1.5, pl: { xs: 0, sm: 7 } }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.6, mb: 0.5 }}>
            <RepeatIcon sx={{ fontSize: 14, color: "text.secondary" }} />
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
              Đã chia sẻ lại
            </Typography>
          </Box>
          <SharedOriginalPost postId={sharedFromPostId} />
        </Box>
      )}

      {bookId && (
        <Paper
          elevation={0}
          onClick={() => navigate(`/books/${bookId}`)}
          sx={{
            p: 1.2,
            mb: 1.5,
            ml: { xs: 0, sm: 7 },
            borderRadius: 2,
            bgcolor: "rgba(25, 118, 210, 0.04)",
            border: "1px solid rgba(25, 118, 210, 0.15)",
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            cursor: "pointer",
            "&:hover": { bgcolor: "rgba(25, 118, 210, 0.08)" },
          }}
        >
          {bookCoverImage ? (
            <Box component="img" src={bookCoverImage} alt={bookTitle} sx={{ width: 38, height: 55, objectFit: "cover", borderRadius: 1, boxShadow: 1 }} />
          ) : (
            <Box sx={{ width: 38, height: 55, bgcolor: "primary.light", borderRadius: 1, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <AutoStoriesIcon sx={{ color: "#fff", fontSize: 18 }} />
            </Box>
          )}
          <Typography variant="body2" sx={{ fontWeight: 700 }} noWrap>
            {bookTitle}
          </Typography>
        </Paper>
      )}

      <Box sx={{ display: "flex", alignItems: "center", gap: 2.5, ml: { xs: 0, sm: 7 } }}>
        <Button size="small" startIcon={isLiked ? <FavoriteIcon sx={{ color: "error.main" }} /> : <FavoriteBorderIcon sx={{ color: "text.secondary" }} />} onClick={handleLike} sx={{ fontWeight: 600, color: isLiked ? "error.main" : "text.secondary", textTransform: "none" }}>
          {likesCount > 0 ? `${likesCount} Thích` : "Thích"}
        </Button>
        <Button size="small" startIcon={<ChatBubbleOutlineOutlinedIcon sx={{ color: "text.secondary" }} />} onClick={handleToggleComments} sx={{ fontWeight: 600, color: "text.secondary", textTransform: "none" }}>
          {commentsCount > 0 ? `${commentsCount} Bình luận` : "Bình luận"}
        </Button>
        <Button size="small" startIcon={<RepeatIcon sx={{ color: "text.secondary" }} />} onClick={() => setShareDialogOpen(true)} sx={{ fontWeight: 600, color: "text.secondary", textTransform: "none" }}>
          Chia sẻ
        </Button>
      </Box>

      <Collapse in={commentOpen}>
        <Box sx={{ mt: 1.5, ml: { xs: 0, sm: 7 } }}>
          {comments.length > 0 && (
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.2, mb: 1.5 }}>
              {comments.map((comm) => (
                // CommentResponse chưa được be-report.md liệt kê chi tiết field
                // tác giả — dùng fallback cả 2 kiểu (phẳng như PostResponse,
                // hoặc lồng "author" như group-service) để không vỡ UI nếu
                // đoán sai, đã ghi vào fe-report.md để BE xác nhận lại.
                <Box key={comm.id} sx={{ display: "flex", gap: 1.2, alignItems: "flex-start" }}>
                  <Avatar src={comm.avatar || comm.author?.avatar} sx={{ width: 28, height: 28 }} />
                  <Box sx={{ bgcolor: "#f4f6f8", p: 1, px: 1.6, borderRadius: 2.5, flexGrow: 1 }}>
                    <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                      <Typography variant="caption" sx={{ fontWeight: 700 }}>
                        {comm.username || comm.author?.username}
                      </Typography>
                      <Typography variant="caption" color="text.disabled" sx={{ fontSize: "0.7rem" }}>
                        {comm.created || comm.createdAt}
                      </Typography>
                    </Box>
                    <Typography variant="body2" sx={{ fontSize: "0.85rem", mt: 0.3 }}>
                      {comm.content}
                    </Typography>
                    <Button
                      size="small"
                      onClick={() =>
                        setReportTarget({
                          targetType: "COMMENT",
                          targetId: comm.id,
                          targetLabel: `Bình luận của ${comm.username || comm.author?.username || "người dùng"}`,
                        })
                      }
                      sx={{ mt: 0.3, p: 0, minWidth: 0, fontSize: "0.68rem", fontWeight: 600, color: "text.disabled", textTransform: "none", "&:hover": { bgcolor: "transparent", color: "text.secondary" } }}
                    >
                      Báo cáo
                    </Button>
                  </Box>
                </Box>
              ))}
            </Box>
          )}
          <Box component="form" onSubmit={handleCommentSubmit} sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
            <TextField
              size="small"
              fullWidth
              placeholder="Viết bình luận…"
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

      <Dialog open={shareDialogOpen} onClose={() => !sharing && setShareDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 700 }}>Chia sẻ bài viết</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            minRows={2}
            maxRows={5}
            placeholder="Thêm lời bình (không bắt buộc)…"
            value={shareContent}
            onChange={(e) => setShareContent(e.target.value)}
            variant="outlined"
            autoFocus
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setShareDialogOpen(false)} disabled={sharing} sx={{ textTransform: "none" }}>
            Huỷ
          </Button>
          <Button variant="contained" onClick={handleShareSubmit} disabled={sharing} sx={{ textTransform: "none", fontWeight: 700 }}>
            {sharing ? <CircularProgress size={18} color="inherit" /> : "Chia sẻ"}
          </Button>
        </DialogActions>
      </Dialog>

      <ReportDialog
        open={Boolean(reportTarget)}
        onClose={() => setReportTarget(null)}
        targetType={reportTarget?.targetType}
        targetId={reportTarget?.targetId}
        targetLabel={reportTarget?.targetLabel}
      />
    </Box>
  );
});

export default Post;
