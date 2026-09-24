import React, { useState, useEffect } from "react";
import { Box, Card, Typography, Tabs, Tab, Button, TextField, InputAdornment, MenuItem, Container, CircularProgress } from "@mui/material";
import ExploreIcon from "@mui/icons-material/Explore";
import GroupsIcon from "@mui/icons-material/Groups";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import AddIcon from "@mui/icons-material/Add";
import SearchIcon from "@mui/icons-material/Search";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAuthenticated } from "../services/authenticationService";
import { useGroup } from "../context/GroupContext";
import { getPopularGroups } from "../services/groupService";
import { GroupCard } from "../components/group/GroupCard";
import { CreateGroupDialog } from "../components/group/CreateGroupDialog";
import { LeaveGroupDialog } from "../components/group/LeaveGroupDialog";
import { EmptyState } from "../components/book/EmptyState";

const CATEGORY_FILTERS = ["Tất cả thể loại", "Công nghệ & Lập trình", "Tâm lý & Kỹ năng sống", "Văn học & Tiểu thuyết", "Lịch sử & Triết học", "Kinh doanh & Khởi nghiệp", "Thiết kế & Nghệ thuật"];

export default function Groups() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
    }
  }, [navigate]);

  const {
    groups,
    myGroups,
    discoverPage,
    discoverTotalPages,
    discoverTotalElements,
    discoverLoading,
    myGroupsLoading,
    fetchDiscoverGroups,
    leaveGroup,
  } = useGroup();

  const [currentTab, setCurrentTab] = useState("DISCOVER");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Tất cả thể loại");
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [leaveTargetGroup, setLeaveTargetGroup] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);

  // Phase 6 (be-report.md, 2026-09-18): GET /groups/popular — trạng thái
  // riêng, không dùng chung state phân trang DISCOVER (nguồn dữ liệu/sort
  // khác hẳn: DISCOVER là search+filter thường, POPULAR sort theo điểm
  // popularity, chỉ nhận category chứ không nhận search query).
  const [popularGroups, setPopularGroups] = useState([]);
  const [popularPage, setPopularPage] = useState(0);
  const [popularTotalPages, setPopularTotalPages] = useState(0);
  const [popularLoading, setPopularLoading] = useState(false);
  const [popularLoadingMore, setPopularLoadingMore] = useState(false);

  const handleTabChange = (_, newValue) => setCurrentTab(newValue);

  const handleOpenLeaveDialog = (group) => setLeaveTargetGroup(group);

  const handleConfirmLeave = (groupId) => leaveGroup(groupId);

  // Tìm kiếm/lọc thể loại thật đều thực hiện phía server (search-service kiểu
  // fuzzy, category là exact-match) — debounce 400ms tránh gọi API mỗi phím gõ.
  useEffect(() => {
    const category = selectedCategory === "Tất cả thể loại" ? undefined : selectedCategory;
    const timer = setTimeout(() => {
      fetchDiscoverGroups(searchQuery.trim() || undefined, category, 0);
    }, 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, selectedCategory]);

  const handleLoadMore = () => {
    if (loadingMore || discoverPage + 1 >= discoverTotalPages) return;
    setLoadingMore(true);
    const category = selectedCategory === "Tất cả thể loại" ? undefined : selectedCategory;
    fetchDiscoverGroups(searchQuery.trim() || undefined, category, discoverPage + 1).finally(() => setLoadingMore(false));
  };

  const loadPopularGroups = (pageToLoad) => {
    const category = selectedCategory === "Tất cả thể loại" ? undefined : selectedCategory;
    if (pageToLoad === 0) setPopularLoading(true);
    else setPopularLoadingMore(true);
    getPopularGroups(category, pageToLoad, 12)
      .then((res) => {
        const result = res.data.result;
        setPopularGroups((prev) => (pageToLoad === 0 ? result.data : [...prev, ...result.data]));
        setPopularPage(pageToLoad);
        setPopularTotalPages(result.totalPages);
      })
      .catch(() => {})
      .finally(() => {
        setPopularLoading(false);
        setPopularLoadingMore(false);
      });
  };

  useEffect(() => {
    if (currentTab !== "POPULAR") return;
    loadPopularGroups(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab, selectedCategory]);

  const handleLoadMorePopular = () => {
    if (popularLoadingMore || popularPage + 1 >= popularTotalPages) return;
    loadPopularGroups(popularPage + 1);
  };

  const displayedGroups = currentTab === "DISCOVER" ? groups : currentTab === "POPULAR" ? popularGroups : myGroups;
  const isLoading = currentTab === "DISCOVER" ? discoverLoading && discoverPage === 0 : currentTab === "POPULAR" ? popularLoading : myGroupsLoading;

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
      <Box sx={{ width: "100%", pb: 4, mt: "5px" }}>
        <Box sx={{ mb: 3, display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: { xs: "flex-start", sm: "center" }, justifyContent: "space-between", gap: 2 }}>
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ fontSize: "17px" }}>
              Tham gia cộng đồng yêu sách, trao đổi quan điểm theo từng thể loại và xây dựng thói quen đọc cùng nhau
            </Typography>
          </Box>

          <Button variant="contained" color="primary" size="medium" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)} sx={{ fontWeight: 700, px: 2.5, py: 1.1, borderRadius: 2.5, boxShadow: "0 4px 12px rgba(25, 118, 210, 0.25)", flexShrink: 0 }}>
            + Tạo nhóm mới
          </Button>
        </Box>

        <Card sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3 }}>
          <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
            <Tabs value={currentTab} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" sx={{ "& .MuiTab-root": { textTransform: "none", fontWeight: 600, fontSize: "0.95rem", minHeight: 48 } }}>
              <Tab icon={<ExploreIcon fontSize="small" />} iconPosition="start" label={`Khám phá nhóm (${discoverTotalElements})`} value="DISCOVER" />
              <Tab icon={<TrendingUpIcon fontSize="small" />} iconPosition="start" label="Nổi bật" value="POPULAR" />
              <Tab icon={<GroupsIcon fontSize="small" />} iconPosition="start" label={`Nhóm của tôi (${myGroups.length})`} value="MY_GROUPS" />
            </Tabs>
          </Box>

          {(currentTab === "DISCOVER" || currentTab === "POPULAR") && (
            <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, gap: 2, mb: 3.5 }}>
              {currentTab === "DISCOVER" && (
                <TextField
                  placeholder="Tìm kiếm nhóm theo tên hoặc mô tả…"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  fullWidth
                  size="small"
                  slotProps={{ input: { startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon color="action" />
                    </InputAdornment>
                  ), sx: { borderRadius: 2, bgcolor: "#f8fafc" } } }}
                />
              )}

              <TextField select value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)} size="small" sx={{ minWidth: { xs: "100%", sm: 220 } }} slotProps={{ input: { sx: { borderRadius: 2, bgcolor: "#f8fafc" } } }}>
                {CATEGORY_FILTERS.map((cat) => (
                  <MenuItem key={cat} value={cat}>
                    {cat}
                  </MenuItem>
                ))}
              </TextField>
            </Box>
          )}

          {isLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 5 }}>
              <CircularProgress size={28} />
            </Box>
          ) : displayedGroups.length > 0 ? (
            <>
              <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", md: "repeat(3, 1fr)" }, gap: 3 }}>
                {displayedGroups.map((group) => (
                  <GroupCard key={group.id} group={group} onOpenLeaveDialog={handleOpenLeaveDialog} />
                ))}
              </Box>

              {currentTab === "DISCOVER" && discoverPage + 1 < discoverTotalPages && (
                <Box sx={{ display: "flex", justifyContent: "center", pt: 3 }}>
                  <Button onClick={handleLoadMore} disabled={loadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                    {loadingMore ? <CircularProgress size={18} /> : "Tải thêm nhóm"}
                  </Button>
                </Box>
              )}
              {currentTab === "POPULAR" && popularPage + 1 < popularTotalPages && (
                <Box sx={{ display: "flex", justifyContent: "center", pt: 3 }}>
                  <Button onClick={handleLoadMorePopular} disabled={popularLoadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                    {popularLoadingMore ? <CircularProgress size={18} /> : "Tải thêm nhóm"}
                  </Button>
                </Box>
              )}
            </>
          ) : (
            <EmptyState
              title={currentTab === "MY_GROUPS" ? "Bạn chưa tham gia hội nhóm nào" : currentTab === "POPULAR" ? "Chưa có nhóm nổi bật" : "Không tìm thấy nhóm phù hợp"}
              description={
                currentTab === "MY_GROUPS"
                  ? "Hãy khám phá các câu lạc bộ đọc sách đa dạng chủ đề hoặc tự tạo một hội nhóm của riêng mình!"
                  : currentTab === "POPULAR"
                  ? "Chưa có đủ dữ liệu hoạt động để xếp hạng nhóm nổi bật. Hãy thử thể loại khác."
                  : "Thử thay đổi từ khoá tìm kiếm hoặc chọn thể loại khác để khám phá thêm các câu lạc bộ sách."
              }
              actionText={currentTab === "MY_GROUPS" ? "Khám phá hội nhóm ngay" : currentTab === "POPULAR" ? undefined : "+ Tạo nhóm mới"}
              onAction={currentTab === "MY_GROUPS" ? () => setCurrentTab("DISCOVER") : currentTab === "POPULAR" ? undefined : () => setCreateDialogOpen(true)}
            />
          )}
        </Card>

        <CreateGroupDialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} />

        <LeaveGroupDialog open={Boolean(leaveTargetGroup)} group={leaveTargetGroup} onClose={() => setLeaveTargetGroup(null)} onConfirm={handleConfirmLeave} />
      </Box>
      </Container>
    </Scene>
  );
}
