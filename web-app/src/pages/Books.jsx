import React, { useState, useEffect, useCallback, useRef } from "react";
import { Box, Card, Container, CircularProgress, Typography, Chip } from "@mui/material";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import { useSearchParams } from "react-router-dom";
import Scene from "./Scene";
import { getBooks, getCategories, getAuthors, getTrendingBooks } from "../services/bookService";
import { getBookErrorMessage } from "../services/errorMessages";
import { BookCard } from "../components/book/BookCard";
import { BookCardSkeleton } from "../components/book/BookCardSkeleton";
import { BookFilter } from "../components/book/BookFilter";
import { EmptyState } from "../components/book/EmptyState";
import { ErrorBanner } from "../components/book/ErrorBanner";

const PAGE_SIZE = 8;
const GRID_COLUMNS = { xs: "repeat(2, 1fr)", sm: "repeat(3, 1fr)", md: "repeat(4, 1fr)", lg: "repeat(5, 1fr)" };
const GRID_GAP = { xs: 1.5, sm: 2, md: 2.5 };

export default function Books() {
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryParam = searchParams.get("category") || "";

  const [categories, setCategories] = useState([]);
  const [authors, setAuthors] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(categoryParam);
  const [selectedAuthor, setSelectedAuthor] = useState("");
  // Phase 6 (be-report.md, 2026-09-18): GET /books/trending — không nhận
  // category/author, nên bật chế độ này sẽ tạm ẩn bộ lọc thay vì âm thầm bỏ
  // qua (tránh gây hiểu nhầm là bộ lọc vẫn đang áp dụng).
  const [trendingMode, setTrendingMode] = useState(false);

  const [books, setBooks] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const loadingMoreRef = useRef(false);

  // Đổ dropdown Category/Author 1 lần
  useEffect(() => {
    getCategories()
      .then((response) => {
        const list = response.data.result?.data || response.data.result || [];
        setCategories(list);
      })
      .catch(() => setCategories([]));

    getAuthors()
      .then((response) => {
        const list = response.data.result?.data || response.data.result || [];
        setAuthors(list);
      })
      .catch(() => setAuthors([]));
  }, []);

  useEffect(() => {
    if (categoryParam) {
      setSelectedCategory(categoryParam);
    }
  }, [categoryParam]);

  const fetchPage = useCallback(
    (pageToLoad) => {
      if (trendingMode) return getTrendingBooks(pageToLoad, PAGE_SIZE);
      return getBooks(pageToLoad, PAGE_SIZE, selectedCategory || undefined, selectedAuthor || undefined);
    },
    [trendingMode, selectedCategory, selectedAuthor]
  );

  // Tải trang đầu (0), thay thế toàn bộ danh sách — dùng lúc vào trang hoặc đổi filter.
  const loadFirstPage = useCallback(() => {
    setLoading(true);
    setError(null);

    fetchPage(0)
      .then((response) => {
        const result = response.data.result;
        setBooks(result.data);
        setTotalPages(result.totalPages);
        setPage(0);
      })
      .catch((err) => {
        setError(getBookErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [fetchPage]);

  // Quay về trang 0 mỗi khi filter/search đổi
  useEffect(() => {
    loadFirstPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchPage]);

  const hasMore = page + 1 < totalPages;

  // Infinite scroll thật (dữ liệu thật từ book-service, phân trang server-side)
  // — cuộn gần cuối trang thì tự tải trang kế tiếp và nối vào danh sách hiện
  // có, không tải hết mọi trang cùng lúc.
  useEffect(() => {
    const handleScroll = () => {
      if (loading || loadingMoreRef.current || !hasMore) return;

      const scrollHeight = document.documentElement.scrollHeight;
      const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
      const clientHeight = document.documentElement.clientHeight;

      if (scrollTop + clientHeight >= scrollHeight - 200) {
        loadingMoreRef.current = true;
        setLoadingMore(true);
        const nextPage = page + 1;

        fetchPage(nextPage)
          .then((response) => {
            const result = response.data.result;
            setBooks((prev) => [...prev, ...result.data]);
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
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [loading, hasMore, page, fetchPage]);

  const handleResetFilter = () => {
    setSelectedCategory("");
    setSelectedAuthor("");
    setSearchParams({});
  };

  const activeFilterCount = (selectedCategory ? 1 : 0) + (selectedAuthor ? 1 : 0);

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
        <Box sx={{ mb: 2 }}>
          <Chip
            icon={<TrendingUpIcon sx={{ fontSize: "16px !important" }} />}
            label="Sách thịnh hành"
            clickable
            color={trendingMode ? "primary" : "default"}
            variant={trendingMode ? "filled" : "outlined"}
            onClick={() => setTrendingMode((v) => !v)}
            sx={{ fontWeight: 600 }}
          />
        </Box>

        {!trendingMode && (
          <BookFilter
            categories={categories}
            authors={authors}
            selectedCategory={selectedCategory}
            selectedAuthor={selectedAuthor}
            onCategoryChange={(val) => setSelectedCategory(val)}
            onAuthorChange={(val) => setSelectedAuthor(val)}
            onResetFilter={handleResetFilter}
            activeFilterCount={activeFilterCount}
          />
        )}

        {error && <ErrorBanner message={error} onRetry={loadFirstPage} />}

        {loading ? (
          <Box sx={{ display: "grid", gridTemplateColumns: GRID_COLUMNS, gap: GRID_GAP }}>
            {Array.from(new Array(PAGE_SIZE)).map((_, idx) => (
              <BookCardSkeleton key={idx} />
            ))}
          </Box>
        ) : books.length === 0 ? (
          <Card sx={{ p: 4, textAlign: "center" }}>
            <EmptyState
              title="Không tìm thấy sách nào phù hợp"
              description="Hãy thử kiểm tra lại từ khoá tìm kiếm hoặc thay đổi bộ lọc thể loại/tác giả."
              actionText="Xoá tất cả bộ lọc"
              onAction={handleResetFilter}
            />
          </Card>
        ) : (
          <>
            <Box sx={{ display: "grid", gridTemplateColumns: GRID_COLUMNS, gap: GRID_GAP }}>
              {books.map((book) => (
                <BookCard key={book.id} book={book} />
              ))}
            </Box>

            {loadingMore && (
              <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 4, gap: 1.5 }}>
                <CircularProgress size={24} />
                <Typography variant="body2" color="text.secondary">
                  Đang tải thêm sách...
                </Typography>
              </Box>
            )}

            {!hasMore && books.length > 0 && (
              <Box sx={{ textAlign: "center", py: 4 }}>
                <Typography variant="caption" color="text.secondary">
                  Đã hiển thị tất cả {books.length} sách
                </Typography>
              </Box>
            )}
          </>
        )}
      </Container>
    </Scene>
  );
}
