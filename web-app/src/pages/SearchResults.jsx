import React, { useState, useEffect, useCallback, useRef } from "react";
import { Box, CircularProgress, Typography, Card, Container, TextField, MenuItem, Tabs, Tab, Chip, Button } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import LocalFireDepartmentIcon from "@mui/icons-material/LocalFireDepartment";
import { useSearchParams } from "react-router-dom";
import Scene from "./Scene";
import { searchBooks, searchPosts, searchGroups, getTrendingHashtags } from "../services/searchService";
import { getBookErrorMessage } from "../services/errorMessages";
import { SearchResultCard } from "../components/book/SearchResultCard";
import { BookCardSkeleton } from "../components/book/BookCardSkeleton";
import { PostSearchResultCard } from "../components/book/PostSearchResultCard";
import { GroupSearchResultCard } from "../components/group/GroupSearchResultCard";
import { EmptyState } from "../components/book/EmptyState";
import { ErrorBanner } from "../components/book/ErrorBanner";

const PAGE_SIZE = 8;

export default function SearchResults() {
  const [searchParams] = useSearchParams();
  const queryParam = searchParams.get("q") || "";
  const categoryParam = searchParams.get("categoryId") || undefined;
  const authorParam = searchParams.get("authorId") || undefined;
  const hashtagParam = searchParams.get("hashtag") || "";

  // §22.1 (be-report.md, bổ sung vào Phase 6, 2026-09-18): tìm kiếm mở rộng
  // sang Post/Group, tab riêng — không gộp chung kết quả với Sách vì 3 loại
  // dữ liệu/shape hoàn toàn khác nhau. Vào thẳng bằng link "?hashtag=..."
  // (VD bấm hashtag trong 1 bài đăng ở Bảng tin) mở đúng tab Bài viết luôn.
  const [currentTab, setCurrentTab] = useState(hashtagParam ? "POSTS" : "BOOKS");
  const [hashtagFilter, setHashtagFilter] = useState(hashtagParam);
  const [trendingHashtags, setTrendingHashtags] = useState([]);

  const [posts, setPosts] = useState([]);
  const [postsTotalElements, setPostsTotalElements] = useState(0);
  const [postsPage, setPostsPage] = useState(0);
  const [postsTotalPages, setPostsTotalPages] = useState(0);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postsLoadingMore, setPostsLoadingMore] = useState(false);
  const [postsError, setPostsError] = useState(null);

  const [groupResults, setGroupResults] = useState([]);
  const [groupsTotalElements, setGroupsTotalElements] = useState(0);
  const [groupsPage, setGroupsPage] = useState(0);
  const [groupsTotalPages, setGroupsTotalPages] = useState(0);
  const [groupsLoading, setGroupsLoading] = useState(false);
  const [groupsLoadingMore, setGroupsLoadingMore] = useState(false);
  const [groupsError, setGroupsError] = useState(null);

  const [books, setBooks] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const [sort, setSort] = useState("");

  const loadResults = useCallback(() => {
    const cleanQuery = queryParam.trim();
    if (!cleanQuery) {
      setBooks([]);
      setTotalElements(0);
      setTotalPages(0);
      setPage(0);
      setLoading(false);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);

    searchBooks(cleanQuery, categoryParam, authorParam, 0, PAGE_SIZE, sort || undefined)
      .then((response) => {
        const result = response.data.result;
        setBooks(result.data);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);
        setPage(0);
      })
      .catch((err) => {
        setError(getBookErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [queryParam, categoryParam, authorParam, sort]);

  useEffect(() => {
    loadResults();
  }, [loadResults]);

  const hasMore = page < totalPages - 1;

  // Cùng cơ chế IntersectionObserver + ref-guard đã dùng ở Books.jsx, tránh
  // bug tải dồn dập nhiều trang trong 1 cú lướt (scroll-listener thô sơ).
  const hasMoreRef = useRef(hasMore);
  const pageRef = useRef(page);
  const loadingMoreRef = useRef(false);
  const queryRef = useRef(queryParam);
  const filtersRef = useRef({ categoryParam, authorParam, sort });
  useEffect(() => {
    hasMoreRef.current = hasMore;
  }, [hasMore]);
  useEffect(() => {
    pageRef.current = page;
  }, [page]);
  useEffect(() => {
    queryRef.current = queryParam;
    filtersRef.current = { categoryParam, authorParam, sort };
  }, [queryParam, categoryParam, authorParam, sort]);

  const loadMore = useCallback(() => {
    if (loadingMoreRef.current || !hasMoreRef.current) return;
    const cleanQuery = queryRef.current.trim();
    if (!cleanQuery) return;

    loadingMoreRef.current = true;
    setLoadingMore(true);
    const nextPage = pageRef.current + 1;
    searchBooks(
      cleanQuery,
      filtersRef.current.categoryParam,
      filtersRef.current.authorParam,
      nextPage,
      PAGE_SIZE,
      filtersRef.current.sort || undefined
    )
      .then((response) => {
        const result = response.data.result;
        setBooks((prev) => [...prev, ...result.data]);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);
        setPage(nextPage);
      })
      .catch((err) => {
        setError(getBookErrorMessage(err));
      })
      .finally(() => {
        loadingMoreRef.current = false;
        setLoadingMore(false);
      });
  }, []);

  const sentinelRef = useRef(null);
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return undefined;

    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      { rootMargin: "300px" }
    );
    io.observe(el);
    return () => io.disconnect();
  }, [loadMore, loading, hasMore]);

  // Trending hashtags — tải 1 lần lúc vào trang, không phụ thuộc query/tab
  // (gợi ý khám phá, không phải kết quả tìm kiếm).
  useEffect(() => {
    getTrendingHashtags(10)
      .then((res) => setTrendingHashtags(res?.data?.result || []))
      .catch(() => setTrendingHashtags([]));
  }, []);

  const loadPosts = useCallback(
    (pageToLoad) => {
      const cleanQuery = queryParam.trim();
      if (!cleanQuery && !hashtagFilter) {
        setPosts([]);
        setPostsTotalElements(0);
        setPostsTotalPages(0);
        setPostsPage(0);
        setPostsLoading(false);
        setPostsError(null);
        return;
      }
      if (pageToLoad === 0) setPostsLoading(true);
      else setPostsLoadingMore(true);
      setPostsError(null);

      // Contract BE: chỉ nhận 1 trong 2 (q HOẶC hashtag) — ưu tiên hashtag
      // khi người dùng vừa bấm 1 thẻ hashtag.
      searchPosts(hashtagFilter ? undefined : cleanQuery, hashtagFilter || undefined, pageToLoad, PAGE_SIZE)
        .then((response) => {
          const result = response.data.result;
          setPosts((prev) => (pageToLoad === 0 ? result.data : [...prev, ...result.data]));
          setPostsTotalElements(result.totalElements);
          setPostsTotalPages(result.totalPages);
          setPostsPage(pageToLoad);
        })
        .catch((err) => setPostsError(getBookErrorMessage(err)))
        .finally(() => {
          setPostsLoading(false);
          setPostsLoadingMore(false);
        });
    },
    [queryParam, hashtagFilter]
  );

  useEffect(() => {
    if (currentTab === "POSTS") loadPosts(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab, loadPosts]);

  const loadGroupResults = useCallback(
    (pageToLoad) => {
      const cleanQuery = queryParam.trim();
      if (!cleanQuery) {
        setGroupResults([]);
        setGroupsTotalElements(0);
        setGroupsTotalPages(0);
        setGroupsPage(0);
        setGroupsLoading(false);
        setGroupsError(null);
        return;
      }
      if (pageToLoad === 0) setGroupsLoading(true);
      else setGroupsLoadingMore(true);
      setGroupsError(null);

      searchGroups(cleanQuery, undefined, pageToLoad, PAGE_SIZE)
        .then((response) => {
          const result = response.data.result;
          setGroupResults((prev) => (pageToLoad === 0 ? result.data : [...prev, ...result.data]));
          setGroupsTotalElements(result.totalElements);
          setGroupsTotalPages(result.totalPages);
          setGroupsPage(pageToLoad);
        })
        .catch((err) => setGroupsError(getBookErrorMessage(err)))
        .finally(() => {
          setGroupsLoading(false);
          setGroupsLoadingMore(false);
        });
    },
    [queryParam]
  );

  useEffect(() => {
    if (currentTab === "GROUPS") loadGroupResults(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab, loadGroupResults]);

  const handleHashtagClick = (tag) => {
    setHashtagFilter(tag);
    setCurrentTab("POSTS");
  };

  // Gõ từ khoá mới trên thanh điều hướng (đổi queryParam) huỷ bộ lọc hashtag
  // đang chọn — tránh 2 tín hiệu lọc mâu thuẫn nhau (contract BE chỉ nhận 1).
  // Bỏ qua lần render đầu tiên — nếu vào thẳng bằng link "?hashtag=...",
  // không được tự xoá filter vừa khởi tạo từ URL.
  const isFirstQueryRender = useRef(true);
  useEffect(() => {
    if (isFirstQueryRender.current) {
      isFirstQueryRender.current = false;
      return;
    }
    setHashtagFilter("");
  }, [queryParam]);

  const hasQuery = Boolean(queryParam.trim());

  return (
    <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ mb: 3, display: "flex", flexDirection: { xs: "column", sm: "row" }, justifyContent: "space-between", alignItems: { xs: "flex-start", sm: "flex-end" }, gap: 2 }}>
            <Box>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1 }}>
                <SearchIcon color="primary" sx={{ fontSize: 28 }} />
                <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: "text.primary" }}>
                  Kết quả tìm kiếm
                </Typography>
              </Box>
              {hasQuery || hashtagFilter ? (
                <Typography variant="body1" color="text.secondary">
                  {hashtagFilter ? (
                    <>Kết quả cho hashtag: "<strong>{hashtagFilter}</strong>"</>
                  ) : (
                    <>Tìm thấy <strong>{currentTab === "BOOKS" ? totalElements : currentTab === "POSTS" ? postsTotalElements : groupsTotalElements}</strong> kết quả cho từ khoá: "<strong>{queryParam}</strong>"</>
                  )}
                </Typography>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Vui lòng nhập từ khoá tìm kiếm trên thanh điều hướng để bắt đầu tìm kiếm.
                </Typography>
              )}
            </Box>

            {hasQuery && currentTab === "BOOKS" && (
              <TextField
                select
                size="small"
                label="Sắp xếp"
                value={sort}
                onChange={(e) => setSort(e.target.value)}
                sx={{ minWidth: 180 }}
              >
                <MenuItem value="">Liên quan nhất</MenuItem>
                <MenuItem value="newest">Mới nhất</MenuItem>
                <MenuItem value="oldest">Cũ nhất</MenuItem>
                <MenuItem value="rating">Đánh giá cao nhất</MenuItem>
              </TextField>
            )}
          </Box>

          <Tabs value={currentTab} onChange={(_, v) => setCurrentTab(v)} sx={{ mb: 2, minHeight: 40, "& .MuiTab-root": { minHeight: 40, textTransform: "none", fontWeight: 700 } }}>
            <Tab label="Sách" value="BOOKS" />
            <Tab label="Bài viết" value="POSTS" />
            <Tab label="Nhóm" value="GROUPS" />
          </Tabs>

          {trendingHashtags.length > 0 && (
            <Box sx={{ display: "flex", alignItems: "center", flexWrap: "wrap", gap: 0.8, mb: 2.5 }}>
              <LocalFireDepartmentIcon sx={{ fontSize: 18, color: "error.main" }} />
              <Typography variant="caption" sx={{ fontWeight: 700, mr: 0.5 }}>
                Thịnh hành:
              </Typography>
              {trendingHashtags.map((tag) => (
                <Chip key={tag} label={tag} size="small" clickable variant={hashtagFilter === tag ? "filled" : "outlined"} color={hashtagFilter === tag ? "primary" : "default"} onClick={() => handleHashtagClick(tag)} sx={{ height: 22, fontSize: "0.7rem" }} />
              ))}
            </Box>
          )}

          {currentTab === "BOOKS" && (
            <>
              {error && <ErrorBanner message={error} onRetry={loadResults} />}

              {loading ? (
                <Box
                  sx={{
                    display: "grid",
                    gridTemplateColumns: {
                      xs: "repeat(2, 1fr)",
                      sm: "repeat(3, 1fr)",
                      md: "repeat(4, 1fr)",
                      lg: "repeat(5, 1fr)",
                    },
                    gap: { xs: 1.5, sm: 2, md: 2.5 },
                  }}
                >
                  {Array.from(new Array(8)).map((_, idx) => (
                    <BookCardSkeleton key={idx} />
                  ))}
                </Box>
              ) : !hasQuery ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState
                    title="Nhập từ khoá để tìm kiếm"
                    description="Tìm kiếm theo tiêu đề sách, tên tác giả, thể loại hoặc nhà xuất bản."
                  />
                </Card>
              ) : books.length === 0 ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState
                    title={`Không tìm thấy kết quả cho "${queryParam}"`}
                    description="Hãy thử kiểm tra lại chính tả hoặc sử dụng các từ khoá ngắn gọn, phổ biến hơn."
                  />
                </Card>
              ) : (
                <>
                  <Box
                    sx={{
                      display: "grid",
                      gridTemplateColumns: {
                        xs: "repeat(2, 1fr)",
                        sm: "repeat(3, 1fr)",
                        md: "repeat(4, 1fr)",
                        lg: "repeat(5, 1fr)",
                      },
                      gap: { xs: 1.5, sm: 2, md: 2.5 },
                    }}
                  >
                    {books.map((book) => (
                      <SearchResultCard key={book.id} book={book} />
                    ))}
                  </Box>

                  {hasMore && <Box ref={sentinelRef} sx={{ height: 1 }} />}

                  {loadingMore && (
                    <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 4, gap: 1.5 }}>
                      <CircularProgress size={24} />
                      <Typography variant="body2" color="text.secondary">
                        Đang tải thêm kết quả...
                      </Typography>
                    </Box>
                  )}

                  {!hasMore && books.length > 0 && (
                    <Box sx={{ textAlign: "center", py: 4 }}>
                      <Typography variant="caption" color="text.secondary">
                        Đã hiển thị tất cả {books.length} kết quả
                      </Typography>
                    </Box>
                  )}
                </>
              )}
            </>
          )}

          {currentTab === "POSTS" && (
            <Box sx={{ maxWidth: 700, mx: "auto" }}>
              {postsError && <ErrorBanner message={postsError} onRetry={() => loadPosts(0)} />}

              {postsLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 5 }}>
                  <CircularProgress />
                </Box>
              ) : !hasQuery && !hashtagFilter ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState title="Nhập từ khoá hoặc bấm 1 hashtag để tìm bài viết" description="Chỉ bài viết Công khai mới xuất hiện trong kết quả tìm kiếm." />
                </Card>
              ) : posts.length === 0 ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState title="Không tìm thấy bài viết nào" description="Hãy thử từ khoá hoặc hashtag khác." />
                </Card>
              ) : (
                <>
                  {posts.map((post) => (
                    <PostSearchResultCard key={post.id} post={post} onHashtagClick={handleHashtagClick} />
                  ))}
                  {postsPage + 1 < postsTotalPages && (
                    <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                      <Button onClick={() => loadPosts(postsPage + 1)} disabled={postsLoadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                        {postsLoadingMore ? <CircularProgress size={18} /> : "Tải thêm bài viết"}
                      </Button>
                    </Box>
                  )}
                </>
              )}
            </Box>
          )}

          {currentTab === "GROUPS" && (
            <Box sx={{ maxWidth: 700, mx: "auto" }}>
              {groupsError && <ErrorBanner message={groupsError} onRetry={() => loadGroupResults(0)} />}

              {groupsLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 5 }}>
                  <CircularProgress />
                </Box>
              ) : !hasQuery ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState title="Nhập từ khoá để tìm nhóm" description="Chỉ nhóm Công khai mới xuất hiện trong kết quả tìm kiếm." />
                </Card>
              ) : groupResults.length === 0 ? (
                <Card sx={{ p: 5, textAlign: "center", borderRadius: 2 }}>
                  <EmptyState title={`Không tìm thấy nhóm nào cho "${queryParam}"`} description="Hãy thử từ khoá khác." />
                </Card>
              ) : (
                <>
                  {groupResults.map((group) => (
                    <GroupSearchResultCard key={group.id} group={group} />
                  ))}
                  {groupsPage + 1 < groupsTotalPages && (
                    <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
                      <Button onClick={() => loadGroupResults(groupsPage + 1)} disabled={groupsLoadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                        {groupsLoadingMore ? <CircularProgress size={18} /> : "Tải thêm nhóm"}
                      </Button>
                    </Box>
                  )}
                </>
              )}
            </Box>
          )}
        </Container>
    </Scene>
  );
}
