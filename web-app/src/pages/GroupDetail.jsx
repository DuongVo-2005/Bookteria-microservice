import React, { useState, useEffect, useCallback } from "react";
import { Box, Card, CardMedia, Avatar, Typography, Chip, Button, Tabs, Tab, Menu, MenuItem, ListItemIcon, ListItemText, List, ListItem, Paper, CircularProgress, Container } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import GroupsIcon from "@mui/icons-material/Groups";
import PublicIcon from "@mui/icons-material/Public";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import ForumIcon from "@mui/icons-material/Forum";
import PeopleIcon from "@mui/icons-material/People";
import InfoIcon from "@mui/icons-material/Info";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import LogoutIcon from "@mui/icons-material/Logout";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import VerifiedUserIcon from "@mui/icons-material/VerifiedUser";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import LockIcon from "@mui/icons-material/Lock";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import FlagOutlinedIcon from "@mui/icons-material/FlagOutlined";
import { useParams, useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAuthenticated } from "../services/authenticationService";
import { useGroup } from "../context/GroupContext";
import { getGroupErrorMessage } from "../services/groupErrorMessages";
import { GroupMemberItem } from "../components/group/GroupMemberItem";
import { CreatePostBox } from "../components/group/CreatePostBox";
import { GroupPostCard } from "../components/group/GroupPostCard";
import { PendingPostCard } from "../components/group/PendingPostCard";
import { LeaveGroupDialog } from "../components/group/LeaveGroupDialog";
import { DeleteGroupDialog } from "../components/group/DeleteGroupDialog";
import { ReportDialog } from "../components/report/ReportDialog";
import { EmptyState } from "../components/book/EmptyState";

const POST_PAGE_SIZE = 10;

export default function GroupDetail() {
  const { groupId } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
    }
  }, [navigate]);

  const { getGroupDetail, getGroupMembers, getGroupPosts, joinGroup, leaveGroup, updateMemberRole, removeMember, isAdmin, deleteGroupById, getPendingPosts } = useGroup();

  const [group, setGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(null);

  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(false);

  const [posts, setPosts] = useState([]);
  const [postsPage, setPostsPage] = useState(0);
  const [postsTotalPages, setPostsTotalPages] = useState(0);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postsLoadingMore, setPostsLoadingMore] = useState(false);

  const [currentTab, setCurrentTab] = useState("POSTS");
  const [anchorElAction, setAnchorElAction] = useState(null);
  const [leaveDialogOpen, setLeaveDialogOpen] = useState(false);
  const [deleteGroupDialogOpen, setDeleteGroupDialogOpen] = useState(false);
  const [reportGroupOpen, setReportGroupOpen] = useState(false);

  // OPS-01 (be-report.md, bổ sung 2026-09-19): hàng chờ duyệt bài, chỉ
  // OWNER/ADMIN của nhóm (vai trò trong nhóm, khác platform isAdmin) mới
  // thấy tab này.
  const [pendingPosts, setPendingPosts] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const canModerate = group?.currentUserRole === "OWNER" || group?.currentUserRole === "ADMIN";

  const loadGroup = useCallback(() => {
    setLoading(true);
    setNotFound(false);
    setError(null);
    getGroupDetail(groupId)
      .then((data) => setGroup(data))
      .catch((err) => {
        if (err?.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(getGroupErrorMessage(err));
        }
      })
      .finally(() => setLoading(false));
  }, [groupId, getGroupDetail]);

  useEffect(() => {
    loadGroup();
  }, [loadGroup]);

  // Nhóm PRIVATE mà chưa tham gia -> GET .../members và .../posts trả 403
  // thật (đúng theo hợp đồng BE) — không gọi 2 API này trong trường hợp đó,
  // hiện chỗ chờ "cần tham gia để xem" thay vì để lỗi hiện ra.
  const canViewContent = Boolean(group) && (group.visibility === "PUBLIC" || group.isJoined);

  const loadMembers = useCallback(() => {
    if (!canViewContent) return;
    setMembersLoading(true);
    getGroupMembers(groupId)
      .then(setMembers)
      .catch(() => setMembers([]))
      .finally(() => setMembersLoading(false));
  }, [groupId, canViewContent, getGroupMembers]);

  const loadPosts = useCallback(
    (pageToLoad = 0) => {
      if (!canViewContent) return;
      if (pageToLoad === 0) setPostsLoading(true);
      else setPostsLoadingMore(true);

      getGroupPosts(groupId, pageToLoad, POST_PAGE_SIZE)
        .then((result) => {
          setPosts((prev) => (pageToLoad === 0 ? result.data : [...prev, ...result.data]));
          setPostsPage(result.currentPage);
          setPostsTotalPages(result.totalPages);
        })
        .catch(() => {})
        .finally(() => {
          setPostsLoading(false);
          setPostsLoadingMore(false);
        });
    },
    [groupId, canViewContent, getGroupPosts]
  );

  useEffect(() => {
    if (!canViewContent) return;
    loadMembers();
    loadPosts(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canViewContent, groupId]);

  const loadPendingPosts = useCallback(() => {
    if (!groupId) return;
    setPendingLoading(true);
    getPendingPosts(groupId, 0, 20)
      .then((result) => setPendingPosts(result.data))
      .catch(() => {})
      .finally(() => setPendingLoading(false));
  }, [groupId, getPendingPosts]);

  useEffect(() => {
    if (currentTab === "PENDING" && canModerate) loadPendingPosts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab]);

  if (loading) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
            <CircularProgress />
          </Box>
        </Container>
      </Scene>
    );
  }

  if (notFound || !group) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ p: 4 }}>
            <EmptyState title="Không tìm thấy hội nhóm" description="Hội nhóm này có thể đã bị xoá hoặc liên kết không hợp lệ." actionText="Quay lại danh sách nhóm" onAction={() => navigate("/groups")} />
          </Box>
        </Container>
      </Scene>
    );
  }

  if (error) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ p: 4 }}>
            <EmptyState title="Không thể tải hội nhóm" description={error} actionText="Thử lại" onAction={loadGroup} />
          </Box>
        </Container>
      </Scene>
    );
  }

  const handleTabChange = (_, newValue) => setCurrentTab(newValue);
  const handleActionMenuOpen = (e) => setAnchorElAction(e.currentTarget);
  const handleActionMenuClose = () => setAnchorElAction(null);
  const handleJoin = () => joinGroup(group.id).then(loadGroup);
  const handleConfirmLeave = () => leaveGroup(group.id).then(loadGroup);
  const handleConfirmDeleteGroup = () => deleteGroupById(group.id).then(() => navigate("/groups"));
  const handleUpdateRole = (userId, newRole) => updateMemberRole(group.id, userId, newRole).then(loadMembers).catch(() => {});
  const handleRemoveMember = (userId) => removeMember(group.id, userId).then(loadMembers).catch(() => {});
  // OPS-01: bài PENDING_APPROVAL chưa publish — không chèn vào feed đang xem.
  const handlePostCreated = (newPost) => {
    if (newPost.status === "PENDING_APPROVAL") return;
    setPosts((prev) => [newPost, ...prev]);
  };

  const handlePostDecided = (postId) => setPendingPosts((prev) => prev.filter((p) => p.id !== postId));
  const handleLoadMorePosts = () => {
    if (postsLoadingMore || postsPage + 1 >= postsTotalPages) return;
    loadPosts(postsPage + 1);
  };

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
      <Box sx={{ width: "100%", pb: 5 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate("/groups")} sx={{ mb: 2, fontWeight: 600, color: "text.secondary" }}>
          Tất cả hội nhóm
        </Button>

        {/* BA v2 Phase 2 §2.3 (be-report.md, bổ sung 2026-09-19): Group.hidden
            (boolean, chỉ dùng cho HIDE_GROUP report action) đã được BE hợp
            nhất thành Group.status (ACTIVE/FROZEN/SUSPENDED) — SUSPENDED giữ
            nguyên đúng hành vi cũ của hidden=true (ẩn khỏi discovery/popular
            công khai, owner/member/ADMIN vẫn xem được trang này bình thường). */}
        {group.status === "SUSPENDED" && (
          <Paper elevation={0} sx={{ p: 2, mb: 2, borderRadius: 2.5, bgcolor: "rgba(211,47,47,0.06)", border: "1px solid rgba(211,47,47,0.2)" }}>
            <Typography variant="body2" sx={{ fontWeight: 700, color: "error.main" }}>
              ⚠️ Nhóm này đã bị quản trị viên ẩn do vi phạm quy định.
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Nhóm không còn hiển thị ở mục Khám phá/Nổi bật, nhưng bạn vẫn xem được vì là thành viên.
            </Typography>
          </Paper>
        )}

        {/* FROZEN — khái niệm mới (be-report.md, BA v2 Phase 2): vẫn hiện
            công khai bình thường, chỉ chặn đăng bài MỚI (ADMIN vẫn bypass
            được ở tầng BE, nhưng banner vẫn hiện cho tất cả để rõ trạng thái). */}
        {group.status === "FROZEN" && (
          <Paper elevation={0} sx={{ p: 2, mb: 2, borderRadius: 2.5, bgcolor: "rgba(245,124,0,0.08)", border: "1px solid rgba(245,124,0,0.25)" }}>
            <Typography variant="body2" sx={{ fontWeight: 700, color: "warning.dark" }}>
              🔒 Nhóm tạm khoá đăng bài.
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Quản trị viên đã tạm khoá tính năng đăng bài mới cho nhóm này. Bài đăng cũ và các tính năng khác vẫn hoạt động bình thường.
            </Typography>
          </Paper>
        )}

        <Card sx={{ borderRadius: 3, overflow: "hidden", mb: 3 }}>
          <Box sx={{ position: "relative", height: { xs: 140, sm: 220 }, bgcolor: "#ccc" }}>
            <CardMedia component="img" height="100%" image={group.coverImage} alt={group.name} sx={{ objectFit: "cover", height: "100%" }} />
          </Box>

          <Box sx={{ p: { xs: 2.5, sm: 3.5 }, pt: 0 }}>
            <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: { xs: "flex-start", sm: "flex-end" }, justifyContent: "space-between", gap: 2, mt: { xs: -5, sm: -6 }, mb: 2 }}>
              <Box sx={{ display: "flex", alignItems: "flex-end", gap: 2.5 }}>
                <Avatar src={group.avatar} alt={group.name} variant="rounded" sx={{ width: { xs: 80, sm: 104 }, height: { xs: 80, sm: 104 }, borderRadius: 3, border: "4px solid #ffffff", boxShadow: "0 4px 14px rgba(0,0,0,0.18)", bgcolor: "primary.main" }} />
                <Box sx={{ mb: 0.5 }}>
                  <Typography variant="h5" component="h1" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
                    {group.name}
                  </Typography>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mt: 0.8, flexWrap: "wrap" }}>
                    <Chip
                      icon={group.visibility === "PUBLIC" ? <PublicIcon sx={{ fontSize: "13px !important" }} /> : <LockOutlinedIcon sx={{ fontSize: "13px !important" }} />}
                      label={group.visibility === "PUBLIC" ? "Nhóm Công khai" : "Nhóm Riêng tư"}
                      size="small"
                      color="default"
                      variant="outlined"
                      sx={{ height: 22, fontSize: "0.75rem", fontWeight: 600 }}
                    />
                    <Chip label={group.category} size="small" sx={{ height: 22, fontSize: "0.75rem", fontWeight: 600, bgcolor: "rgba(25, 118, 210, 0.1)", color: "primary.dark" }} />
                    <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                      <GroupsIcon sx={{ fontSize: 16, color: "text.secondary" }} />
                      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                        {group.memberCount} thành viên
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              </Box>

              <Box sx={{ flexShrink: 0, alignSelf: { xs: "stretch", sm: "flex-end" }, display: "flex", gap: 1, alignItems: "center" }}>
                {!group.isJoined ? (
                  <>
                    <Button variant="contained" color="primary" size="large" fullWidth onClick={handleJoin} sx={{ fontWeight: 700, px: 3.5, py: 1.2, borderRadius: 2.5, boxShadow: "0 4px 12px rgba(25, 118, 210, 0.3)" }}>
                      + Tham gia nhóm
                    </Button>
                    {/* Phase 4: platform ADMIN xoá được nhóm dù không phải member. */}
                    {isAdmin && (
                      <Button variant="outlined" color="error" size="small" startIcon={<DeleteOutlineIcon />} onClick={() => setDeleteGroupDialogOpen(true)} sx={{ borderRadius: 2, textTransform: "none", fontWeight: 600, flexShrink: 0 }}>
                        Xoá nhóm
                      </Button>
                    )}
                  </>
                ) : (
                  <Box sx={{ display: "flex", gap: 1.5, alignItems: "center" }}>
                    <Button
                      variant="outlined"
                      color="primary"
                      size="large"
                      endIcon={<ArrowDropDownIcon />}
                      onClick={handleActionMenuOpen}
                      sx={{ fontWeight: 700, px: 2.5, py: 1.1, borderRadius: 2.5, borderWidth: 2, "&:hover": { borderWidth: 2 } }}
                    >
                      {group.currentUserRole === "OWNER" ? "Trưởng nhóm ▾" : group.currentUserRole === "ADMIN" ? "Quản trị viên ▾" : "Đang là thành viên ▾"}
                    </Button>

                    <Menu anchorEl={anchorElAction} open={Boolean(anchorElAction)} onClose={handleActionMenuClose} slotProps={{ paper: { sx: { minWidth: 180, borderRadius: 2, boxShadow: 3 } } }}>
                      <MenuItem disabled>
                        <ListItemIcon>
                          <CheckCircleIcon fontSize="small" color="success" />
                        </ListItemIcon>
                        <ListItemText primary="Trạng thái" secondary={group.currentUserRole === "OWNER" ? "Trưởng nhóm" : group.currentUserRole === "ADMIN" ? "Quản trị viên" : "Thành viên"} />
                      </MenuItem>
                      {group.currentUserRole !== "OWNER" && (
                        <MenuItem
                          onClick={() => {
                            handleActionMenuClose();
                            setLeaveDialogOpen(true);
                          }}
                          sx={{ color: "error.main" }}
                        >
                          <ListItemIcon>
                            <LogoutIcon fontSize="small" color="error" />
                          </ListItemIcon>
                          <ListItemText primary="Rời nhóm" />
                        </MenuItem>
                      )}
                      {group.currentUserRole !== "OWNER" && (
                        <MenuItem
                          onClick={() => {
                            handleActionMenuClose();
                            setReportGroupOpen(true);
                          }}
                        >
                          <ListItemIcon>
                            <FlagOutlinedIcon fontSize="small" />
                          </ListItemIcon>
                          <ListItemText primary="Báo cáo nhóm" />
                        </MenuItem>
                      )}
                      {/* Phase 4: OWNER hoặc platform ADMIN mới xoá được nhóm. */}
                      {(group.currentUserRole === "OWNER" || isAdmin) && (
                        <MenuItem
                          onClick={() => {
                            handleActionMenuClose();
                            setDeleteGroupDialogOpen(true);
                          }}
                          sx={{ color: "error.main" }}
                        >
                          <ListItemIcon>
                            <DeleteOutlineIcon fontSize="small" color="error" />
                          </ListItemIcon>
                          <ListItemText primary="Xoá nhóm" />
                        </MenuItem>
                      )}
                    </Menu>
                  </Box>
                )}
              </Box>
            </Box>

            <Typography variant="body1" color="text.secondary" sx={{ mt: 1, fontSize: "0.95rem" }}>
              {group.description}
            </Typography>
          </Box>

          <Box sx={{ borderTop: 1, borderColor: "divider", px: { xs: 2, sm: 3 } }}>
            <Tabs value={currentTab} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" sx={{ "& .MuiTab-root": { textTransform: "none", fontWeight: 600, fontSize: "0.95rem", minHeight: 52 } }}>
              <Tab icon={<ForumIcon fontSize="small" />} iconPosition="start" label="Bài đăng" value="POSTS" />
              {canModerate && (
                <Tab icon={<CheckCircleIcon fontSize="small" />} iconPosition="start" label={`Chờ duyệt${pendingPosts.length ? ` (${pendingPosts.length})` : ""}`} value="PENDING" />
              )}
              <Tab icon={<PeopleIcon fontSize="small" />} iconPosition="start" label={`Thành viên (${group.memberCount})`} value="MEMBERS" />
              <Tab icon={<InfoIcon fontSize="small" />} iconPosition="start" label="Giới thiệu & Quy định" value="ABOUT" />
            </Tabs>
          </Box>
        </Card>

        {!canViewContent && currentTab !== "ABOUT" ? (
          <Card sx={{ p: 5, borderRadius: 3, textAlign: "center" }}>
            <LockIcon sx={{ fontSize: 40, color: "text.disabled", mb: 1.5 }} />
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>
              Nhóm riêng tư
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Bạn cần tham gia nhóm này để xem bài đăng và danh sách thành viên.
            </Typography>
            <Button variant="contained" color="primary" onClick={handleJoin} sx={{ borderRadius: 2 }}>
              Tham gia ngay
            </Button>
          </Card>
        ) : (
          <>
            {currentTab === "POSTS" && (
              <Box sx={{ maxWidth: 800, mx: "auto" }}>
                {group.isJoined ? (
                  group.status === "FROZEN" && !isAdmin ? (
                    <Paper elevation={0} sx={{ p: 2.5, mb: 3, borderRadius: 3, bgcolor: "rgba(245,124,0,0.06)", border: "1px dashed rgba(245,124,0,0.3)", textAlign: "center" }}>
                      <Typography variant="body2" color="text.secondary">
                        Nhóm đang tạm khoá đăng bài — không thể tạo bài viết mới lúc này.
                      </Typography>
                    </Paper>
                  ) : (
                    <CreatePostBox groupId={group.id} onCreated={handlePostCreated} />
                  )
                ) : (
                  <Paper elevation={0} sx={{ p: 3, mb: 3, borderRadius: 3, border: "1px dashed rgba(25, 118, 210, 0.4)", bgcolor: "rgba(25, 118, 210, 0.03)", textAlign: "center" }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>
                      Tham gia nhóm để cùng chia sẻ bài viết
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      Gia nhập hội nhóm để trao đổi cảm nhận về sách và thảo luận cùng các thành viên.
                    </Typography>
                    <Button variant="contained" color="primary" onClick={handleJoin} sx={{ borderRadius: 2 }}>
                      Tham gia ngay
                    </Button>
                  </Paper>
                )}

                {postsLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", py: 5 }}>
                    <CircularProgress size={26} />
                  </Box>
                ) : posts.length > 0 ? (
                  <>
                    <Box>
                      {posts.map((post) => (
                        <GroupPostCard key={post.id} post={post} onDeleted={(postId) => setPosts((prev) => prev.filter((p) => p.id !== postId))} />
                      ))}
                    </Box>
                    {postsPage + 1 < postsTotalPages && (
                      <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                        <Button onClick={handleLoadMorePosts} disabled={postsLoadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                          {postsLoadingMore ? <CircularProgress size={18} /> : "Tải thêm bài đăng"}
                        </Button>
                      </Box>
                    )}
                  </>
                ) : (
                  <EmptyState title="Chưa có bài viết nào trong nhóm" description="Hãy là người đầu tiên chia sẻ cảm nhận về một cuốn sách hoặc đặt câu hỏi mở đầu thảo luận!" />
                )}
              </Box>
            )}

            {currentTab === "PENDING" && canModerate && (
              <Box sx={{ maxWidth: 800, mx: "auto" }}>
                {pendingLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", py: 5 }}>
                    <CircularProgress size={26} />
                  </Box>
                ) : pendingPosts.length > 0 ? (
                  pendingPosts.map((post) => <PendingPostCard key={post.id} post={post} onDecided={handlePostDecided} />)
                ) : (
                  <EmptyState title="Không có bài viết nào chờ duyệt" description="Bài viết mới của thành viên (khi nhóm bật chế độ duyệt bài) sẽ hiện ở đây." />
                )}
              </Box>
            )}

            {currentTab === "MEMBERS" && (
              <Card sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3 }}>
                <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2.5 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Danh sách thành viên ({members.length})
                  </Typography>
                </Box>

                {membersLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                    <CircularProgress size={26} />
                  </Box>
                ) : members.length > 0 ? (
                  <Box>
                    {members.map((member) => (
                      <GroupMemberItem key={member.id} member={member} currentUserRole={group.currentUserRole} onUpdateRole={handleUpdateRole} onRemoveMember={handleRemoveMember} />
                    ))}
                  </Box>
                ) : (
                  <EmptyState title="Chưa có thành viên" description="Nhóm này chưa có thành viên nào tham gia." />
                )}
              </Card>
            )}
          </>
        )}

        {currentTab === "ABOUT" && (
          <Card sx={{ p: { xs: 2.5, sm: 3.5 }, borderRadius: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.5 }}>
              Về hội nhóm này
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.7, mb: 3 }}>
              {group.description}
            </Typography>

            <Box sx={{ display: "flex", gap: 3, mb: 3, flexWrap: "wrap" }}>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <CalendarTodayIcon sx={{ fontSize: 18, color: "text.secondary" }} />
                <Typography variant="body2" color="text.secondary">
                  Thành lập: <strong>{new Date(group.createdAt).toLocaleDateString("vi-VN")}</strong>
                </Typography>
              </Box>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <GroupsIcon sx={{ fontSize: 18, color: "text.secondary" }} />
                <Typography variant="body2" color="text.secondary">
                  Quy mô: <strong>{group.memberCount} thành viên</strong>
                </Typography>
              </Box>
            </Box>

            <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.5 }}>
              Quy định hội nhóm
            </Typography>
            <List disablePadding>
              {(group.rules && group.rules.length > 0
                ? group.rules
                : ["Tôn trọng và lịch sự trong mọi bình luận thảo luận.", "Không chia sẻ các nội dung vi phạm bản quyền hoặc spam quảng cáo.", "Khi review sách có tình tiết quan trọng cần gắn cảnh báo Spoiler."]
              ).map((rule, idx) => (
                <ListItem key={idx} disableGutters sx={{ display: "flex", alignItems: "flex-start", gap: 1.5, py: 1 }}>
                  <VerifiedUserIcon sx={{ color: "primary.main", fontSize: 20, mt: 0.2 }} />
                  <Typography variant="body2" sx={{ lineHeight: 1.5 }}>
                    {rule}
                  </Typography>
                </ListItem>
              ))}
            </List>
          </Card>
        )}

        <LeaveGroupDialog open={leaveDialogOpen} group={group} onClose={() => setLeaveDialogOpen(false)} onConfirm={handleConfirmLeave} />
        <DeleteGroupDialog open={deleteGroupDialogOpen} group={group} onClose={() => setDeleteGroupDialogOpen(false)} onConfirm={handleConfirmDeleteGroup} />
        <ReportDialog open={reportGroupOpen} onClose={() => setReportGroupOpen(false)} targetType="GROUP" targetId={group.id} targetLabel={`Nhóm ${group.name}`} />
      </Box>
      </Container>
    </Scene>
  );
}
