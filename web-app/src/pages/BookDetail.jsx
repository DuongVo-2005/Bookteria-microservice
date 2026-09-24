import React, { useState, useEffect, useCallback } from "react";
import DOMPurify from "dompurify";
import { Box, Card, Typography, Chip, Rating, Button, Divider, Skeleton, Paper, Container, LinearProgress, List } from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import BusinessIcon from "@mui/icons-material/Business";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import LanguageIcon from "@mui/icons-material/Language";
import ClassIcon from "@mui/icons-material/Class";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import PlayCircleFilledIcon from "@mui/icons-material/PlayCircleFilled";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import VisibilityIcon from "@mui/icons-material/Visibility";
import Scene from "./Scene";
import { getBookById } from "../services/bookService";
import { getMyReadingList } from "../services/readingListService";
import { getBookErrorMessage } from "../services/errorMessages";
import { PlaceholderCover } from "../components/book/PlaceholderCover";
import { ShelfActionMenu } from "../components/book/ShelfActionMenu";
import { ReviewList } from "../components/book/ReviewList";
import { EmptyState } from "../components/book/EmptyState";
import { ErrorBanner } from "../components/book/ErrorBanner";
import { useReading } from "../context/ReadingContext";
import { getReadingAccess, resolveGoogleBooksAccessCase, GOOGLE_BOOKS_ACCESS_CASE } from "../services/googleBooksService";

// Mô tả sách (đặc biệt sách nhập từ Google Books) là HTML thô (<p>/<b>/<br>...),
// trước đây hiện nguyên thẻ dạng chữ vì render bằng Typography thường. Sanitize
// bằng DOMPurify (whitelist thẻ định dạng văn bản cơ bản, không cho phép link/
// ảnh/script) trước khi dùng dangerouslySetInnerHTML, tránh XSS.
const DESCRIPTION_ALLOWED_TAGS = ["p", "b", "strong", "i", "em", "br", "ul", "ol", "li", "u"];
const sanitizeDescriptionHtml = (html) => DOMPurify.sanitize(html || "", { ALLOWED_TAGS: DESCRIPTION_ALLOWED_TAGS, ALLOWED_ATTR: [] });
const stripHtmlToText = (html) => DOMPurify.sanitize(html || "", { ALLOWED_TAGS: [], ALLOWED_ATTR: [] });

export default function BookDetail() {
  const { bookId } = useParams();
  const navigate = useNavigate();
  const { getProgress, getChapters } = useReading();

  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState(null);
  const [isExpandedDescription, setIsExpandedDescription] = useState(false);
  const [imgError, setImgError] = useState(false);
  const [shelfEntry, setShelfEntry] = useState(null);
  const [progress, setProgress] = useState(null);
  const [chapters, setChapters] = useState([]);
  const [googleAccess, setGoogleAccess] = useState(null);
  const [googleAccessLoading, setGoogleAccessLoading] = useState(false);

  const loadBook = useCallback(() => {
    setLoading(true);
    setNotFound(false);
    setError(null);

    getBookById(bookId)
      .then((response) => {
        setBook(response.data.result);
      })
      .catch((err) => {
        if (err.response?.status === 404) {
          setNotFound(true);
        } else {
          setError(getBookErrorMessage(err));
        }
      })
      .finally(() => {
        setLoading(false);
      });
  }, [bookId]);

  useEffect(() => {
    loadBook();
  }, [loadBook]);

  // Biết sách này đã có trong kệ của user chưa (và trạng thái gì) để ShelfActionMenu hiện đúng nút
  useEffect(() => {
    if (!book) return;
    getMyReadingList(0, 100)
      .then((response) => {
        const found = response.data.result.data.find((item) => item.bookId === book.id);
        setShelfEntry(found || null);
      })
      .catch(() => setShelfEntry(null));
  }, [book]);

  // Tiến độ đọc + mục lục chương thật từ reading-service — sách chưa được
  // seed chương sẽ trả danh sách rỗng (không phải lỗi).
  useEffect(() => {
    if (!book) return;
    getProgress(bookId)
      .then((p) => setProgress(p ? { ...p, lastReadAt: p.lastReadAt ? new Date(p.lastReadAt).toLocaleString("vi-VN") : "" } : null))
      .catch(() => setProgress(null));
    getChapters(bookId)
      .then(setChapters)
      .catch(() => setChapters([]));
  }, [book, bookId, getProgress, getChapters]);

  // Đọc qua Google Books — nút chỉ hiện khi sách có googleBookId (sách nhập
  // từ Google Books qua Admin, xem AdminBooks.jsx). Nếu lỗi/không có quyền
  // đọc thì chỉ đơn giản không hiện khối này, không chặn phần còn lại của trang.
  useEffect(() => {
    if (!book?.googleBookId) {
      setGoogleAccess(null);
      return;
    }
    setGoogleAccessLoading(true);
    getReadingAccess(bookId)
      .then((response) => setGoogleAccess(response?.data?.result || null))
      .catch(() => setGoogleAccess(null))
      .finally(() => setGoogleAccessLoading(false));
  }, [book, bookId]);

  const getAuthorRoleLabel = (role) => {
    switch (role) {
      case "MAIN_AUTHOR":
        return "Tác giả chính";
      case "CONTRIBUTOR":
        return "Đóng góp";
      case "TRANSLATOR":
        return "Dịch giả";
      default:
        return "Tác giả";
    }
  };

  const getFormatLabel = (format) => {
    switch (format) {
      case "HARDCOVER":
        return "Bìa cứng";
      case "PAPERBACK":
        return "Bìa mềm";
      case "EBOOK":
        return "Sách điện tử";
      case "AUDIOBOOK":
        return "Sách nói";
      default:
        return format;
    }
  };

  if (notFound) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Card sx={{ p: 4, my: 4 }}>
            <EmptyState
              title="Không tìm thấy cuốn sách này"
              description={`Mã sách "${bookId}" không tồn tại hoặc đã bị gỡ bỏ khỏi hệ thống Book Service.`}
              actionText="Về trang Khám phá sách"
              onAction={() => navigate("/books")}
            />
          </Card>
        </Container>
      </Scene>
    );
  }

  if (error) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ my: 4 }}>
            <ErrorBanner message={error} onRetry={loadBook} />
          </Box>
        </Container>
      </Scene>
    );
  }

  if (loading || !book) {
    return (
      <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Card sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
            <Box sx={{ display: "flex", flexDirection: { xs: "column", md: "row" }, gap: 4 }}>
              <Box sx={{ width: { xs: "100%", md: "30%" } }}>
                <Box sx={{ width: "100%", paddingTop: "150%", position: "relative", mb: 2 }}>
                  <Skeleton
                    variant="rectangular"
                    animation="wave"
                    sx={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", borderRadius: 2 }}
                  />
                </Box>
                <Skeleton variant="rectangular" height={48} animation="wave" sx={{ borderRadius: 1 }} />
              </Box>
              <Box sx={{ width: { xs: "100%", md: "70%" }, flexGrow: 1 }}>
                <Skeleton variant="text" width="80%" height={40} animation="wave" />
                <Skeleton variant="text" width="50%" height={24} animation="wave" sx={{ mb: 2 }} />
                <Skeleton variant="text" width="40%" height={20} animation="wave" sx={{ mb: 2 }} />
                <Box sx={{ display: "flex", gap: 1, mb: 3 }}>
                  <Skeleton variant="rectangular" width={80} height={28} sx={{ borderRadius: 4 }} />
                  <Skeleton variant="rectangular" width={100} height={28} sx={{ borderRadius: 4 }} />
                </Box>
                <Skeleton variant="rectangular" height={60} animation="wave" sx={{ mb: 3, borderRadius: 1 }} />
                <Skeleton variant="text" width="100%" height={20} />
                <Skeleton variant="text" width="100%" height={20} />
                <Skeleton variant="text" width="90%" height={20} />
                <Skeleton variant="text" width="70%" height={20} />
              </Box>
            </Box>
          </Card>
        </Container>
      </Scene>
    );
  }

  const publishYear = book.metadata?.publishedDate ? book.metadata.publishedDate.split("-")[0] : "Chưa cập nhật";

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
        <Box sx={{ width: "100%", pb: 4 }}>
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate("/books")} sx={{ mb: 2, color: "text.secondary" }}>
            Quay lại Khám phá sách
          </Button>

          <Card sx={{ p: { xs: 2.5, sm: 3.5, md: 4.5 }, mb: 3 }}>
              <Box sx={{ display: "flex", flexDirection: { xs: "column", md: "row" }, gap: { xs: 3, md: 4.5 } }}>
                <Box sx={{ width: { xs: "100%", md: "30%" }, minWidth: { md: 240 }, maxWidth: { md: 320 } }}>
                  <Box
                    sx={{
                      width: "100%",
                      paddingTop: "150%",
                      position: "relative",
                      borderRadius: 2,
                      overflow: "hidden",
                      boxShadow: "0 8px 24px rgba(0, 0, 0, 0.14)",
                      mb: 2.5,
                      bgcolor: "#f5f5f5",
                    }}
                  >
                    {book.metadata?.coverImage && !imgError ? (
                      <Box
                        component="img"
                        src={book.metadata.coverImage}
                        alt={book.title}
                        onError={() => setImgError(true)}
                        sx={{
                          position: "absolute",
                          top: 0,
                          left: 0,
                          width: "100%",
                          height: "100%",
                          objectFit: "cover",
                        }}
                      />
                    ) : (
                      <Box sx={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%" }}>
                        <PlaceholderCover title={book.title} />
                      </Box>
                    )}
                  </Box>

                  <Button
                    variant="contained"
                    color="primary"
                    size="large"
                    fullWidth
                    startIcon={<AutoStoriesIcon />}
                    onClick={() => navigate(`/reader/${book.id}`)}
                    sx={{
                      fontWeight: 700,
                      py: 1.3,
                      borderRadius: 2.5,
                      mb: 1.5,
                      boxShadow: "0 4px 14px rgba(25, 118, 210, 0.35)",
                      textTransform: "none",
                      fontSize: "1rem",
                    }}
                  >
                    {progress && progress.progressPercent > 0 ? `Tiếp tục đọc (${progress.progressPercent}%)` : "Đọc sách ngay"}
                  </Button>

                  <ShelfActionMenu
                    book={book}
                    shelfEntry={shelfEntry}
                    onShelfChange={setShelfEntry}
                    fullWidth={true}
                    size="large"
                  />

                  {progress && progress.progressPercent > 0 && (
                    <Paper
                      elevation={0}
                      sx={{
                        p: 2,
                        mt: 2,
                        borderRadius: 2,
                        bgcolor: "#f0fdf4",
                        border: "1px solid #bbf7d0",
                      }}
                    >
                      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 0.8 }}>
                        <Typography variant="caption" sx={{ fontWeight: 700, color: "success.dark" }}>
                          Tiến độ đọc của bạn
                        </Typography>
                        <Typography variant="caption" sx={{ fontWeight: 800, color: "success.dark" }}>
                          {progress.progressPercent}%
                        </Typography>
                      </Box>
                      <LinearProgress variant="determinate" value={progress.progressPercent} color="success" sx={{ height: 6, borderRadius: 3 }} />
                      <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1, fontSize: "0.75rem" }}>
                        Lần đọc cuối: {progress.lastReadAt}
                      </Typography>
                    </Paper>
                  )}

                  {/* Đọc qua Google Books — chỉ hiện khi sách có googleBookId
                      (đã import từ Google Books). */}
                  {book.googleBookId && !googleAccessLoading && googleAccess && (
                    <>
                      {(() => {
                        const accessCase = resolveGoogleBooksAccessCase(googleAccess);
                        if (accessCase === GOOGLE_BOOKS_ACCESS_CASE.FULL_EMBED) {
                          return (
                            <Button
                              variant="outlined"
                              color="primary"
                              fullWidth
                              size="large"
                              startIcon={<span role="img" aria-label="">📖</span>}
                              onClick={() => navigate(`/read/books/${book.id}`)}
                              sx={{ mt: 1.5, fontWeight: 700, borderRadius: 2.5, textTransform: "none" }}
                            >
                              Đọc sách
                            </Button>
                          );
                        }
                        if (accessCase === GOOGLE_BOOKS_ACCESS_CASE.PARTIAL_EMBED) {
                          return (
                            <Button
                              variant="outlined"
                              color="primary"
                              fullWidth
                              size="large"
                              startIcon={<VisibilityIcon />}
                              onClick={() => navigate(`/read/books/${book.id}`)}
                              sx={{ mt: 1.5, fontWeight: 700, borderRadius: 2.5, textTransform: "none" }}
                            >
                              Đọc bản xem trước
                            </Button>
                          );
                        }
                        if (accessCase === GOOGLE_BOOKS_ACCESS_CASE.EXTERNAL_ONLY) {
                          return (
                            <Button
                              variant="outlined"
                              color="primary"
                              fullWidth
                              size="large"
                              startIcon={<OpenInNewIcon />}
                              onClick={() => window.open(googleAccess.webReaderLink, "_blank", "noopener,noreferrer")}
                              sx={{ mt: 1.5, fontWeight: 700, borderRadius: 2.5, textTransform: "none" }}
                            >
                              Đọc trên Google Books
                            </Button>
                          );
                        }
                        if (accessCase === GOOGLE_BOOKS_ACCESS_CASE.NO_ACCESS) {
                          return (
                            <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1.5, textAlign: "center" }}>
                              Cuốn sách này hiện không có nội dung đọc trực tuyến.
                              {googleAccess.webReaderLink && (
                                <>
                                  {" "}
                                  <Box
                                    component="span"
                                    onClick={() => window.open(googleAccess.webReaderLink, "_blank", "noopener,noreferrer")}
                                    sx={{ color: "primary.main", cursor: "pointer", fontWeight: 600 }}
                                  >
                                    Xem trên Google Books
                                  </Box>
                                </>
                              )}
                            </Typography>
                          );
                        }
                        // UNKNOWN — không giả định user có quyền đọc, không hiện nút hành động.
                        return null;
                      })()}
                    </>
                  )}
                </Box>

                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography variant="h4" component="h1" sx={{ fontWeight: 700, lineHeight: 1.25, mb: 0.8 }}>
                    {book.title}
                  </Typography>

                  {book.subtitle && (
                    <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 2, lineHeight: 1.4 }}>
                      {book.subtitle}
                    </Typography>
                  )}

                  <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 1, mb: 2 }}>
                    <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
                      Tác giả:
                    </Typography>
                    {book.authors.map((author, index) => (
                      <Typography key={author.authorId} variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                        {author.name}{" "}
                        <Typography component="span" variant="caption" color="text.secondary">
                          ({getAuthorRoleLabel(author.role)})
                        </Typography>
                        {index < book.authors.length - 1 && ", "}
                      </Typography>
                    ))}
                  </Box>

                  <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1, mb: 2.5 }}>
                    {book.categories.map((cat) => (
                      <Chip
                        key={cat.categoryId}
                        label={cat.name}
                        onClick={() => navigate(`/books?category=${cat.categoryId}`)}
                        color="primary"
                        variant="outlined"
                        size="small"
                        clickable
                        sx={{ borderRadius: 4, fontWeight: 500 }}
                      />
                    ))}
                  </Box>

                  <Paper
                    elevation={0}
                    sx={{
                      p: 2,
                      mb: 3,
                      borderRadius: 2,
                      bgcolor: "#f8fafc",
                      border: "1px solid rgba(0,0,0,0.06)",
                      display: "flex",
                      flexWrap: "wrap",
                      alignItems: "center",
                      gap: 2,
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography variant="h5" sx={{ fontWeight: 700, color: "primary.main" }}>
                        {book.stats?.ratingAverage || 0}
                      </Typography>
                      <Rating value={book.stats?.ratingAverage || 0} precision={0.5} readOnly size="medium" />
                    </Box>
                    <Divider orientation="vertical" flexItem sx={{ display: { xs: "none", sm: "block" } }} />
                    <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
                      {book.stats?.ratingAverage || 0} ★ · {book.stats?.reviewCount || 0} đánh giá · {book.stats?.readCount || 0} người đang đọc ·{" "}
                      {book.stats?.wantToReadCount || 0} người muốn đọc
                    </Typography>
                  </Paper>

                  <Box
                    sx={{
                      display: "grid",
                      gridTemplateColumns: { xs: "repeat(2, 1fr)", sm: "repeat(3, 1fr)" },
                      gap: 2,
                      mb: 3,
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <BusinessIcon fontSize="small" color="action" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Nhà xuất bản
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {book.publisher.name}
                        </Typography>
                      </Box>
                    </Box>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <CalendarMonthIcon fontSize="small" color="action" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Năm xuất bản
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {publishYear}
                        </Typography>
                      </Box>
                    </Box>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <MenuBookIcon fontSize="small" color="action" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Số trang
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {book.metadata?.pageCount ? `${book.metadata.pageCount} trang` : "Chưa cập nhật"}
                        </Typography>
                      </Box>
                    </Box>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <LanguageIcon fontSize="small" color="action" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Ngôn ngữ
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {book.metadata?.language || "Chưa cập nhật"}
                        </Typography>
                      </Box>
                    </Box>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <ClassIcon fontSize="small" color="action" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Định dạng
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {book.metadata?.format ? getFormatLabel(book.metadata.format) : "Chưa cập nhật"}
                        </Typography>
                      </Box>
                    </Box>
                  </Box>

                  <Divider sx={{ my: 2.5 }} />

                  <Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>
                      Giới thiệu sách
                    </Typography>
                    <Typography
                      variant="body2"
                      component="div"
                      color="text.secondary"
                      sx={{
                        lineHeight: 1.8,
                        whiteSpace: "pre-line",
                        "& p": { m: 0, mb: 1, "&:last-child": { mb: 0 } },
                        ...(!isExpandedDescription && {
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          display: "-webkit-box",
                          WebkitLineClamp: 5,
                          WebkitBoxOrient: "vertical",
                        }),
                      }}
                      dangerouslySetInnerHTML={{ __html: sanitizeDescriptionHtml(book.description) }}
                    />
                    {stripHtmlToText(book.description).length > 250 && (
                      <Button
                        size="small"
                        onClick={() => setIsExpandedDescription(!isExpandedDescription)}
                        sx={{ mt: 1, p: 0, minWidth: "auto", fontWeight: 600 }}
                      >
                        {isExpandedDescription ? "Thu gọn" : "Xem thêm"}
                      </Button>
                    )}
                  </Box>
                </Box>
              </Box>

            {chapters.length > 0 && (
              <Box sx={{ mt: 4, pt: 3, borderTop: "1px solid rgba(0,0,0,0.08)" }}>
                <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2 }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <MenuBookIcon color="primary" />
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                      Mục lục các chương ({chapters.length} chương)
                    </Typography>
                  </Box>
                  <Button
                    variant="outlined"
                    color="primary"
                    size="small"
                    startIcon={<PlayCircleFilledIcon />}
                    onClick={() => navigate(`/reader/${book.id}`)}
                    sx={{ textTransform: "none", fontWeight: 600, borderRadius: 2 }}
                  >
                    Đọc toàn bộ sách
                  </Button>
                </Box>

                <List sx={{ p: 0 }}>
                  {chapters.map((chap, idx) => (
                    <Paper
                      key={chap.id}
                      elevation={0}
                      onClick={() => navigate(`/reader/${book.id}`)}
                      sx={{
                        p: 1.8,
                        mb: 1.2,
                        borderRadius: 2,
                        border: "1px solid rgba(0,0,0,0.06)",
                        bgcolor: "#fafafa",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        cursor: "pointer",
                        transition: "all 0.2s",
                        "&:hover": {
                          bgcolor: "#f0f7ff",
                          borderColor: "primary.light",
                          transform: "translateX(4px)",
                        },
                      }}
                    >
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, minWidth: 0 }}>
                        <Chip label={idx + 1} size="small" color="primary" variant="outlined" sx={{ fontWeight: 700, minWidth: 28, height: 24 }} />
                        <Box sx={{ minWidth: 0 }}>
                          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                            {chap.title}
                          </Typography>
                          {chap.subtitle && (
                            <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                              {chap.subtitle}
                            </Typography>
                          )}
                        </Box>
                      </Box>

                      <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, color: "text.secondary", flexShrink: 0 }}>
                        <AccessTimeIcon sx={{ fontSize: 13 }} />
                        <Typography variant="caption">{chap.readTimeMinutes} phút đọc</Typography>
                      </Box>
                    </Paper>
                  ))}
                </List>
              </Box>
            )}

            <Divider sx={{ my: 4 }} />

            <ReviewList book={book} />
          </Card>
        </Box>
      </Container>
    </Scene>
  );
}
