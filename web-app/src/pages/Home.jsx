import { useEffect, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CircularProgress,
  Typography,
  Fab,
  Popover,
  TextField,
  Button,
  Snackbar,
  Alert,
  Paper,
  LinearProgress,
  Chip,
  Tabs,
  Tab,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  ToggleButtonGroup,
  ToggleButton,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import BookmarksIcon from "@mui/icons-material/Bookmarks";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import PublicIcon from "@mui/icons-material/Public";
import PeopleIcon from "@mui/icons-material/People";
import LockIcon from "@mui/icons-material/Lock";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import CloseIcon from "@mui/icons-material/Close";
import { isAuthenticated } from "../services/authenticationService";
import Scene from "./Scene";
import Post from "../components/Post";
import { getMyPosts, getFeed, getFriendsFeed, getPersonalizedFeed, createPost } from "../services/postService";
import { getPostErrorMessage } from "../services/postErrorMessages";
import { getBooks, getBookById, getTrendingBooks, getMyRecommendations } from "../services/bookService";
import { getMyReadingList } from "../services/readingListService";
import { getOnboardingPreferences } from "../services/onboardingService";
import { BookCard } from "../components/book/BookCard";
import { OnboardingWizardDialog } from "../components/book/OnboardingWizardDialog";
import AutoFixHighIcon from "@mui/icons-material/AutoFixHigh";

const FEED_TABS = [
  { key: "global", label: "Bảng tin" },
  { key: "personalized", label: "Dành cho bạn" },
  { key: "friends", label: "Bạn bè" },
  { key: "mine", label: "Của tôi" },
];

const VISIBILITY_OPTIONS = [
  { value: "PUBLIC", label: "Công khai", icon: <PublicIcon sx={{ fontSize: 16 }} /> },
  { value: "FRIENDS", label: "Bạn bè", icon: <PeopleIcon sx={{ fontSize: 16 }} /> },
  { value: "PRIVATE", label: "Riêng tư", icon: <LockIcon sx={{ fontSize: 16 }} /> },
];

export default function Home() {
  const [posts, setPosts] = useState([]);
  // post-service's GET /my-posts đổi từ 1-indexed sang 0-indexed (2026-09-18,
  // breaking change) — page đầu tiên giờ là 0, giống mọi service khác.
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(false);
  const observer = useRef();
  const lastPostElementRef = useRef();
  // Phase 3 (be-report.md, 2026-09-18): post-service giờ có Global
  // Feed/Friends Feed/My posts riêng biệt — 3 tab độc lập, đổi tab reset
  // page/posts về đầu (không gộp state phân trang giữa các tab).
  const [feedTab, setFeedTab] = useState("global");
  const [anchorEl, setAnchorEl] = useState(null);
  const [newPostContent, setNewPostContent] = useState("");
  const [postVisibility, setPostVisibility] = useState("PUBLIC");
  const [selectedBook, setSelectedBook] = useState(null);
  const [anchorElBooks, setAnchorElBooks] = useState(null);
  const [bookOptions, setBookOptions] = useState([]);
  const [loadingBooks, setLoadingBooks] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarSeverity, setSnackbarSeverity] = useState("success");

  // Khu vực "dashboard sách" bổ sung trên Bảng tin — hoàn toàn tách biệt
  // khỏi state/luồng dữ liệu của feed bài đăng phía dưới (posts/page/...),
  // dùng lại đúng API thật đã có (book-service, reading-list), không thêm
  // backend/context mới, không đụng logic feed hiện có.
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [continueItem, setContinueItem] = useState(null);
  const [shelfCounts, setShelfCounts] = useState({ READING: 0, WANT_TO_READ: 0, COMPLETED: 0 });
  const [featuredBooks, setFeaturedBooks] = useState([]);
  // Phase 6: tải riêng, KHÔNG gộp vào Promise.all ở trên — BE tự cảnh báo
  // "/me/recommendations" có thể chậm ở lần gọi đầu của 1 user mới (cache
  // miss, tự tính ngay lúc request) — tách riêng để không làm chậm cả
  // dashboard (shelf counts/Đọc tiếp/Sách nổi bật vẫn hiện ngay).
  const [recommendedBooks, setRecommendedBooks] = useState([]);
  const [recommendedLoading, setRecommendedLoading] = useState(true);

  // FEAT-01 (be-report.md, bổ sung 2026-09-19): chỉ hiện banner mời làm
  // onboarding nếu BE xác nhận chưa hoàn thành — đóng banner chỉ lưu tạm
  // trên trình duyệt (localStorage), không có API "bỏ qua" riêng nên không
  // đồng bộ giữa các thiết bị, chấp nhận được cho 1 lời mời không bắt buộc.
  const [onboardingNeeded, setOnboardingNeeded] = useState(false);
  const [onboardingDialogOpen, setOnboardingDialogOpen] = useState(false);

  const navigate = useNavigate();

  // Handle opening the popover
  const handleCreatePostClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  // Handle closing the popover
  const handleClosePopover = () => {
    setAnchorEl(null);
    setNewPostContent("");
    setPostVisibility("PUBLIC");
    setSelectedBook(null);
  };

  const handleOpenBookMenu = (e) => {
    setAnchorElBooks(e.currentTarget);
    if (bookOptions.length === 0) {
      setLoadingBooks(true);
      getBooks(0, 20)
        .then((response) => setBookOptions(response?.data?.result?.data || []))
        .catch(() => setBookOptions([]))
        .finally(() => setLoadingBooks(false));
    }
  };
  const handleCloseBookMenu = () => setAnchorElBooks(null);
  const handleSelectBook = (book) => {
    setSelectedBook(book);
    handleCloseBookMenu();
  };

  // Handle Snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === "clickaway") {
      return;
    }
    setSnackbarOpen(false);
  };

  // Handle posting new content
  const handlePostContent = () => {
    const content = newPostContent;
    const visibility = postVisibility;
    const bookId = selectedBook?.id;
    handleClosePopover();

    createPost(content, { visibility, bookId })
      .then((response) => {
        // Chỉ chèn ngay vào feed đang xem nếu bài mới chắc chắn thuộc feed đó
        // (PUBLIC luôn hiện ở "Bảng tin"/"Của tôi"; tab "Của tôi" hiện mọi
        // visibility; tab "Bạn bè" không tự hiện bài của chính mình).
        if (feedTab === "mine" || (feedTab === "global" && visibility === "PUBLIC")) {
          setPosts((prevPosts) => [response.data.result, ...prevPosts]);
        }
        setSnackbarMessage("Đăng bài thành công!");
        setSnackbarSeverity("success");
        setSnackbarOpen(true);
      })
      .catch((error) => {
        setSnackbarMessage(getPostErrorMessage(error));
        setSnackbarSeverity("error");
        setSnackbarOpen(true);
      });
  };

  const open = Boolean(anchorEl);
  const popoverId = open ? "post-popover" : undefined;

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
    } else {
      loadPosts(feedTab, page);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [navigate, page, feedTab]);

  // Đổi tab: reset toàn bộ state phân trang, effect ở trên sẽ tự tải lại
  // trang 0 của feed mới.
  const handleFeedTabChange = (_, newTab) => {
    if (!newTab || newTab === feedTab) return;
    setPosts([]);
    setTotalPages(0);
    setHasMore(false);
    setFeedTab(newTab);
    setPage(0);
  };

  // Tải dữ liệu cho khu vực dashboard sách — chạy đúng 1 lần, tách biệt
  // hoàn toàn với effect tải feed ở trên (không dùng chung state `page`).
  useEffect(() => {
    if (!isAuthenticated()) return;

    Promise.all([
      getMyReadingList(0, 1, "READING"),
      getMyReadingList(0, 1, "WANT_TO_READ"),
      getMyReadingList(0, 1, "COMPLETED"),
      // Phase 6 (be-report.md, 2026-09-18): "Sách nổi bật" giờ dùng đúng tín
      // hiệu trending thật (readCount/reviewCount/ratingAverage) thay vì
      // getBooks(0, 4) (trước đây chỉ lấy 4 sách đầu danh sách, không có ý
      // nghĩa "nổi bật" thật sự).
      getTrendingBooks(0, 4),
    ])
      .then(([readingRes, wantRes, completedRes, featuredRes]) => {
        setShelfCounts({
          READING: readingRes.data.result.totalElements || 0,
          WANT_TO_READ: wantRes.data.result.totalElements || 0,
          COMPLETED: completedRes.data.result.totalElements || 0,
        });
        setFeaturedBooks(featuredRes.data.result.data || []);

        const firstReading = readingRes.data.result.data?.[0];
        if (firstReading) {
          return getBookById(firstReading.bookId).then((bookRes) => {
            setContinueItem({ ...firstReading, book: bookRes.data.result });
          });
        }
      })
      .catch(() => {
        // Khu vực dashboard chỉ là bổ sung — lỗi ở đây không được chặn/ảnh
        // hưởng tới Bảng tin bên dưới.
      })
      .finally(() => setDashboardLoading(false));

    getMyRecommendations()
      .then((res) => setRecommendedBooks(res?.data?.result || []))
      .catch(() => setRecommendedBooks([]))
      .finally(() => setRecommendedLoading(false));

    if (localStorage.getItem("onboardingBannerDismissed") !== "1") {
      getOnboardingPreferences()
        .then((res) => setOnboardingNeeded(res?.data?.result?.completed === false))
        .catch(() => {});
    }
  }, []);

  const loadPosts = (tab, page) => {
    setLoading(true);
    const fetcher = tab === "friends" ? getFriendsFeed : tab === "mine" ? getMyPosts : tab === "personalized" ? getPersonalizedFeed : getFeed;
    fetcher(page)
      .then((response) => {
        setTotalPages(response.data.result.totalPages);
        setPosts((prevPosts) => [...prevPosts, ...response.data.result.data]);
        setHasMore(response.data.result.data.length > 0);
      })
      .catch((error) => {
        // 401 handled globally by httpClient's response interceptor
        if (error?.response?.status !== 401) {
          setSnackbarMessage(getPostErrorMessage(error));
          setSnackbarSeverity("error");
          setSnackbarOpen(true);
        }
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    if (!hasMore) return;

    if (observer.current) observer.current.disconnect();
    observer.current = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        // 0-indexed: trang cuối cùng hợp lệ là totalPages - 1.
        if (page + 1 < totalPages) {
          setPage((prevPage) => prevPage + 1);
        }
      }
    });
    if (lastPostElementRef.current) {
      observer.current.observe(lastPostElementRef.current);
    }

    setHasMore(false);
  }, [hasMore]);

  return (
    <Scene>
      {" "}
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={3000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
        sx={{ marginTop: "64px" }} // Position below the header
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbarSeverity}
          sx={{ width: "100%" }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
      <Box sx={{ width: "100%", maxWidth: 900, display: "flex", flexDirection: "column", alignItems: "center", gap: 3, mt: "20px" }}>
        {!dashboardLoading && (
          <Box sx={{ width: "100%" }}>
            {/* FEAT-01: mời hoàn thiện onboarding nếu BE xác nhận chưa xong */}
            {onboardingNeeded && (
              <Paper
                elevation={0}
                sx={{ p: 2, mb: 3, borderRadius: 3, border: "1px solid rgba(255, 193, 7, 0.4)", bgcolor: "rgba(255, 193, 7, 0.08)", display: "flex", alignItems: "center", gap: 2, flexWrap: "wrap" }}
              >
                <AutoFixHighIcon sx={{ color: "#f59e0b" }} />
                <Box sx={{ flexGrow: 1, minWidth: 200 }}>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    Hoàn thiện hồ sơ đọc sách để nhận gợi ý phù hợp hơn
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Chọn thể loại/tác giả yêu thích chỉ mất 1 phút.
                  </Typography>
                </Box>
                <Button size="small" variant="contained" onClick={() => setOnboardingDialogOpen(true)} sx={{ textTransform: "none", fontWeight: 700 }}>
                  Bắt đầu
                </Button>
                <Button
                  size="small"
                  color="inherit"
                  onClick={() => {
                    localStorage.setItem("onboardingBannerDismissed", "1");
                    setOnboardingNeeded(false);
                  }}
                  sx={{ textTransform: "none" }}
                >
                  Để sau
                </Button>
              </Paper>
            )}

            {/* Banner chào mừng */}
            <Paper
              elevation={0}
              sx={{
                p: { xs: 2.5, sm: 3.5 },
                mb: 3,
                borderRadius: 3,
                background: "linear-gradient(135deg, #1976d2 0%, #1565c0 100%)",
                color: "#ffffff",
                boxShadow: "0 4px 20px rgba(25, 118, 210, 0.25)",
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
                <AutoAwesomeIcon sx={{ color: "#ffca28" }} />
                <Typography variant="overline" sx={{ letterSpacing: 1.2, fontWeight: 700 }}>
                  Bookteria
                </Typography>
              </Box>
              <Typography variant="h5" component="h1" sx={{ fontWeight: 800, mb: 1.5 }}>
                Chào mừng bạn quay lại
              </Typography>
              <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
                <Button
                  variant="contained"
                  onClick={() => navigate("/books")}
                  startIcon={<MenuBookIcon />}
                  sx={{ bgcolor: "#ffffff", color: "primary.main", fontWeight: 700, "&:hover": { bgcolor: "#f0f0f0" } }}
                >
                  Khám phá sách
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => navigate("/me/reading-list")}
                  startIcon={<BookmarksIcon />}
                  sx={{ borderColor: "#ffffff", color: "#ffffff", fontWeight: 600, "&:hover": { borderColor: "#ffffff", bgcolor: "rgba(255,255,255,0.1)" } }}
                >
                  Kệ sách của tôi ({shelfCounts.READING + shelfCounts.WANT_TO_READ + shelfCounts.COMPLETED})
                </Button>
              </Box>
            </Paper>

            {/* Đọc tiếp — chỉ hiện khi có sách đang ở trạng thái "Đang đọc" */}
            {continueItem && (
              <Paper
                elevation={0}
                sx={{
                  p: { xs: 2, sm: 3 },
                  mb: 3,
                  borderRadius: 3,
                  border: "1px solid rgba(25, 118, 210, 0.2)",
                  background: "linear-gradient(135deg, #f0f7ff 0%, #e0f2fe 100%)",
                  display: "flex",
                  flexDirection: { xs: "column", sm: "row" },
                  alignItems: { xs: "flex-start", sm: "center" },
                  justifyContent: "space-between",
                  gap: 2.5,
                }}
              >
                <Box sx={{ display: "flex", alignItems: "center", gap: 2, minWidth: 0 }}>
                  <Box
                    component="img"
                    src={continueItem.book.metadata?.coverImage}
                    alt={continueItem.book.title}
                    sx={{ width: { xs: 60, sm: 75 }, height: { xs: 90, sm: 112 }, borderRadius: 2, objectFit: "cover", boxShadow: "0 4px 12px rgba(0,0,0,0.15)", flexShrink: 0, bgcolor: "#e0e0e0" }}
                  />
                  <Box sx={{ minWidth: 0 }}>
                    <Chip icon={<TrendingUpIcon />} label="Đang đọc dở" size="small" color="primary" sx={{ fontWeight: 700, height: 22, fontSize: "0.72rem", mb: 0.7 }} />
                    <Typography variant="subtitle1" sx={{ fontWeight: 800, lineHeight: 1.3 }}>
                      {continueItem.book.title}
                    </Typography>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1.2, mt: 1, maxWidth: 320 }}>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, Math.round(((continueItem.currentPage || 0) / (continueItem.book.metadata?.pageCount || 1)) * 100))}
                        sx={{ flex: 1, height: 6, borderRadius: 3 }}
                      />
                      <Typography variant="caption" color="text.secondary">
                        {continueItem.currentPage || 0}/{continueItem.book.metadata?.pageCount || "?"} trang
                      </Typography>
                    </Box>
                  </Box>
                </Box>
                <Button
                  variant="contained"
                  startIcon={<PlayArrowIcon />}
                  onClick={() => navigate(`/reader/${continueItem.book.id}`)}
                  sx={{ fontWeight: 700, px: 3, py: 1.1, borderRadius: 2.5, textTransform: "none", flexShrink: 0, alignSelf: { xs: "stretch", sm: "center" } }}
                >
                  Đọc tiếp
                </Button>
              </Paper>
            )}

            {/* Số liệu kệ sách — 3 con số thật từ reading-list, không suy diễn thêm số liệu nào khác */}
            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 1.5, mb: 3 }}>
              <Paper elevation={0} sx={{ p: 1.8, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.06)", textAlign: "center" }}>
                <MenuBookIcon color="primary" sx={{ mb: 0.5 }} />
                <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
                  {shelfCounts.READING}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Đang đọc
                </Typography>
              </Paper>
              <Paper elevation={0} sx={{ p: 1.8, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.06)", textAlign: "center" }}>
                <CheckCircleIcon color="success" sx={{ mb: 0.5 }} />
                <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
                  {shelfCounts.COMPLETED}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Đã đọc xong
                </Typography>
              </Paper>
              <Paper elevation={0} sx={{ p: 1.8, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.06)", textAlign: "center" }}>
                <BookmarkBorderIcon color="action" sx={{ mb: 0.5 }} />
                <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
                  {shelfCounts.WANT_TO_READ}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Muốn đọc
                </Typography>
              </Paper>
            </Box>

            {/* Sách nổi bật */}
            {featuredBooks.length > 0 && (
              <Box>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Sách nổi bật
                  </Typography>
                  <Button size="small" color="primary" onClick={() => navigate("/books")}>
                    Xem tất cả
                  </Button>
                </Box>
                <Box sx={{ display: "grid", gridTemplateColumns: { xs: "repeat(2, 1fr)", sm: "repeat(4, 1fr)" }, gap: { xs: 1.5, sm: 2 } }}>
                  {featuredBooks.map((book) => (
                    <BookCard key={book.id} book={book} />
                  ))}
                </Box>
              </Box>
            )}

            {/* Gợi ý cho bạn — /me/recommendations (Phase 6), tải riêng nên
                có thể hiện muộn hơn phần còn lại của dashboard. */}
            {!recommendedLoading && recommendedBooks.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Gợi ý cho bạn
                  </Typography>
                </Box>
                <Box sx={{ display: "grid", gridTemplateColumns: { xs: "repeat(2, 1fr)", sm: "repeat(4, 1fr)" }, gap: { xs: 1.5, sm: 2 } }}>
                  {recommendedBooks.slice(0, 4).map((book) => (
                    <BookCard key={book.id} book={book} />
                  ))}
                </Box>
              </Box>
            )}
          </Box>
        )}

        <Card
          sx={{
            minWidth: { xs: "100%", sm: 500 },
            maxWidth: 600,
            boxShadow: 3,
            borderRadius: 2,
            padding: "20px",
          }}
        >
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            alignItems: "flex-start",
            width: "100%",
            gap: "10px",
          }}
        >
          <Tabs
            value={feedTab}
            onChange={handleFeedTabChange}
            sx={{ width: "100%", minHeight: 36, mb: 0.5, "& .MuiTab-root": { minHeight: 36, py: 0.5, fontSize: "0.85rem", fontWeight: 700, textTransform: "none" } }}
          >
            {FEED_TABS.map((t) => (
              <Tab key={t.key} value={t.key} label={t.label} />
            ))}
          </Tabs>
          {!loading && posts.length === 0 && (
            <Box sx={{ width: "100%", py: 4, textAlign: "center" }}>
              <Typography variant="body2" color="text.secondary">
                {feedTab === "global" && "Chưa có bài đăng công khai nào."}
                {feedTab === "personalized" && "Chưa có gợi ý bài đăng nào — hãy kết bạn và thêm sách vào kệ để nhận gợi ý phù hợp hơn."}
                {feedTab === "friends" && "Bạn bè của bạn chưa đăng bài nào."}
                {feedTab === "mine" && "Bạn chưa đăng bài nào."}
              </Typography>
            </Box>
          )}
          {posts.map((post, index) => {
            const handleDeleted = (postId) => setPosts((prev) => prev.filter((p) => p.id !== postId));
            if (posts.length === index + 1) {
              return (
                <Post ref={lastPostElementRef} key={post.id} post={post} onDeleted={handleDeleted} />
              );
            } else {
              return <Post key={post.id} post={post} onDeleted={handleDeleted} />;
            }
          })}
          {loading && (
            <Box
              sx={{ display: "flex", justifyContent: "center", width: "100%" }}
            >
              <CircularProgress size="24px" />
            </Box>
          )}
        </Box>
        </Card>
      </Box>
      {/* Floating Action Button for creating new posts */}
      <Fab
        color="primary"
        aria-label="add"
        onClick={handleCreatePostClick}
        sx={{
          position: "fixed",
          bottom: 30,
          right: 30,
        }}
      >
        <AddIcon />
      </Fab>
      {/* Popover for creating new post */}{" "}
      <Popover
        id={popoverId}
        open={open}
        anchorEl={anchorEl}
        onClose={handleClosePopover}
        anchorOrigin={{
          vertical: "top",
          horizontal: "center",
        }}
        transformOrigin={{
          vertical: "bottom",
          horizontal: "center",
        }}
        slotProps={{
          paper: {
            sx: {
              borderRadius: 5,
              p: 3,
              width: 500,
            },
          },
        }}
      >
        <Typography variant="h6" sx={{ mb: 2 }}>
          Tạo bài đăng mới
        </Typography>
        <TextField
          fullWidth
          multiline
          rows={4}
          placeholder="Bạn đang nghĩ gì?"
          value={newPostContent}
          onChange={(e) => setNewPostContent(e.target.value)}
          variant="outlined"
          sx={{ mb: 1.5 }}
        />

        {selectedBook && (
          <Box sx={{ display: "inline-flex", alignItems: "center", gap: 1, mb: 1.5, p: 0.8, px: 1.5, borderRadius: 2, bgcolor: "rgba(25, 118, 210, 0.08)", border: "1px solid rgba(25, 118, 210, 0.2)" }}>
            <AutoStoriesIcon sx={{ fontSize: 16, color: "primary.main" }} />
            <Typography variant="caption" sx={{ fontWeight: 600, color: "primary.dark" }}>
              Đang nhắc đến: {selectedBook.title}
            </Typography>
            <Chip label="Bỏ" size="small" onDelete={() => setSelectedBook(null)} deleteIcon={<CloseIcon sx={{ fontSize: "14px !important" }} />} sx={{ height: 20, fontSize: "0.7rem", ml: 0.5 }} />
          </Box>
        )}

        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2, gap: 1, flexWrap: "wrap" }}>
          <ToggleButtonGroup size="small" exclusive value={postVisibility} onChange={(_, v) => v && setPostVisibility(v)}>
            {VISIBILITY_OPTIONS.map((opt) => (
              <ToggleButton key={opt.value} value={opt.value} sx={{ textTransform: "none", fontSize: "0.75rem", fontWeight: 600, px: 1.2, gap: 0.5 }}>
                {opt.icon}
                {opt.label}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>

          <Button size="small" variant="outlined" color="inherit" startIcon={<AutoStoriesIcon sx={{ color: "primary.main" }} />} onClick={handleOpenBookMenu} sx={{ borderRadius: 2, fontWeight: 600, fontSize: "0.75rem", textTransform: "none" }}>
            Gắn thẻ sách
          </Button>
          <Menu anchorEl={anchorElBooks} open={Boolean(anchorElBooks)} onClose={handleCloseBookMenu} slotProps={{ paper: { sx: { maxHeight: 300, width: 320, borderRadius: 2, boxShadow: 4 } } }}>
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 700 }}>
                CHỌN SÁCH
              </Typography>
            </Box>
            {loadingBooks && (
              <Box sx={{ display: "flex", justifyContent: "center", p: 2 }}>
                <CircularProgress size={20} />
              </Box>
            )}
            {!loadingBooks && bookOptions.length === 0 && (
              <Box sx={{ px: 2, py: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Không có sách nào.
                </Typography>
              </Box>
            )}
            {!loadingBooks &&
              bookOptions.map((book) => (
                <MenuItem key={book.id} onClick={() => handleSelectBook(book)}>
                  <ListItemIcon>
                    <AutoStoriesIcon fontSize="small" color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Typography variant="body2" sx={{ fontSize: "0.85rem", fontWeight: 600 }} noWrap>
                        {book.title}
                      </Typography>
                    }
                  />
                </MenuItem>
              ))}
          </Menu>
        </Box>

        <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
          <Button
            variant="contained"
            color="primary"
            onClick={handlePostContent}
            disabled={!newPostContent.trim()}
          >
            Đăng bài
          </Button>
        </Box>
      </Popover>
      <OnboardingWizardDialog
        open={onboardingDialogOpen}
        onClose={() => setOnboardingDialogOpen(false)}
        onCompleted={() => setOnboardingNeeded(false)}
      />
    </Scene>
  );
}
