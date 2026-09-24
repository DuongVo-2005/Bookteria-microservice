import React, { useState, useEffect, useCallback } from "react";
import { Box, Card, Typography, Tabs, Tab, Container, CircularProgress, Paper, Avatar, LinearProgress } from "@mui/material";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import HistoryIcon from "@mui/icons-material/History";
import RemoveCircleOutlineIcon from "@mui/icons-material/RemoveCircleOutline";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { getMyReadingList, updateReadingProgress, removeFromShelf } from "../services/readingListService";
import { getBookById } from "../services/bookService";
import { getBookErrorMessage } from "../services/errorMessages";
import { useToast } from "../context/ToastContext";
import { useReading } from "../context/ReadingContext";
import { ShelfItem } from "../components/book/ShelfItem";
import { UnavailableShelfItem } from "../components/book/UnavailableShelfItem";
import { ReadingStatsPanel } from "../components/book/ReadingStatsPanel";
import { ReadingWrapUpCard } from "../components/book/ReadingWrapUpCard";
import { ProgressDialog } from "../components/book/ProgressDialog";
import { MarkCompletedDialog } from "../components/book/MarkCompletedDialog";
import { EmptyState } from "../components/book/EmptyState";
import { ErrorBanner } from "../components/book/ErrorBanner";

export default function ReadingList() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const { history, historyLoading, fetchHistory } = useReading();

  const [currentTab, setCurrentTab] = useState("READING");
  const [items, setItems] = useState([]);
  const [counts, setCounts] = useState({ WANT_TO_READ: 0, READING: 0, COMPLETED: 0, UNAVAILABLE: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [progressDialogOpen, setProgressDialogOpen] = useState(false);
  const [selectedProgressItem, setSelectedProgressItem] = useState(null);

  const [completedDialogOpen, setCompletedDialogOpen] = useState(false);
  const [selectedCompletedItem, setSelectedCompletedItem] = useState(null);

  const loadShelf = useCallback((status) => {
    setLoading(true);
    setError(null);

    getMyReadingList(0, 50, status)
      .then((response) => {
        const result = response.data.result;
        setCounts((prev) => ({ ...prev, [status]: result.totalElements }));

        // BA Backlog GAP-02 (be-report.md, bổ sung 2026-09-18): sách
        // UNAVAILABLE đã bị xoá thật khỏi catalog — getBookById() sẽ 404,
        // làm vỡ Promise.all nếu vẫn enrich như 3 trạng thái kia. Chỉ dùng
        // đúng dữ liệu thô đã có trong ReadingList entry.
        if (status === "UNAVAILABLE") return result.data;

        return Promise.all(
          result.data.map((entry) =>
            getBookById(entry.bookId).then((bookResponse) => ({
              ...entry,
              book: bookResponse.data.result,
            }))
          )
        );
      })
      .then((merged) => setItems(merged))
      .catch((err) => setError(getBookErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    // Tab "HISTORY" đọc tiến độ thật từ reading-service (qua ReadingContext),
    // khác hẳn trạng thái kệ sách của book-service — không gọi loadShelf().
    if (currentTab === "HISTORY") {
      fetchHistory();
      return;
    }
    loadShelf(currentTab);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab, loadShelf]);

  const handleTabChange = (_, newValue) => {
    setCurrentTab(newValue);
  };

  const handleUpdateStatus = (id, status) => {
    updateReadingProgress(id, { status })
      .then(() => {
        showSuccess(`Đã chuyển trạng thái sang "${getStatusLabel(status)}"`);
        loadShelf(currentTab);
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleRemove = (id) => {
    removeFromShelf(id)
      .then(() => {
        showSuccess("Đã xoá khỏi kệ sách");
        loadShelf(currentTab);
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleOpenProgress = (item) => {
    setSelectedProgressItem(item);
    setProgressDialogOpen(true);
  };

  const handleSaveProgress = (id, newPage) => {
    updateReadingProgress(id, { currentPage: newPage })
      .then(() => {
        showSuccess("Đã cập nhật tiến độ đọc thành công");
        loadShelf(currentTab);
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleOpenMarkCompleted = (item) => {
    setSelectedCompletedItem(item);
    setCompletedDialogOpen(true);
  };

  const handleConfirmCompleted = (id, finalPage) => {
    updateReadingProgress(id, { status: "COMPLETED", currentPage: finalPage })
      .then(() => {
        showSuccess("Đã đánh dấu đọc xong");
        loadShelf(currentTab);
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case "WANT_TO_READ":
        return "Muốn đọc";
      case "READING":
        return "Đang đọc";
      case "COMPLETED":
        return "Đã đọc";
      default:
        return status;
    }
  };

  const getEmptyStateContent = () => {
    switch (currentTab) {
      case "WANT_TO_READ":
        return {
          title: 'Kệ "Muốn đọc" đang trống',
          description: "Bạn chưa đánh dấu cuốn sách nào muốn đọc. Hãy khám phá và thêm các tựa sách yêu thích vào đây!",
        };
      case "READING":
        return {
          title: "Bạn chưa đọc cuốn nào",
          description: "Hãy chọn một cuốn sách từ thư viện để bắt đầu hành trình đọc sách và theo dõi tiến độ của bạn.",
        };
      case "COMPLETED":
        return {
          title: "Chưa có sách nào đã hoàn thành",
          description: "Khi bạn đọc xong một cuốn sách, hãy đánh dấu hoàn thành để ghi nhận thành tích nhé!",
        };
      case "UNAVAILABLE":
        return {
          title: "Không có sách nào không khả dụng",
          description: "Đây là nơi lưu lại sách đã từng có trong kệ nhưng bị gỡ khỏi hệ thống.",
        };
      default:
        return { title: "", description: "" };
    }
  };

  const emptyContent = getEmptyStateContent();

  return (
    <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ width: "100%", pb: 4 }}>
            <Box sx={{ mb: 3 }}>
              <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, fontSize: "1.05rem" }}>
                Quản lý danh mục sách đã lưu, theo dõi mục tiêu và tiến độ đọc sách cá nhân
              </Typography>
            </Box>

            <ReadingStatsPanel />

            <ReadingWrapUpCard />

            <Card sx={{ p: { xs: 2, sm: 3 }, borderRadius: 2 }}>
              <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
                <Tabs
                  value={currentTab}
                  onChange={handleTabChange}
                  variant="scrollable"
                  scrollButtons="auto"
                  sx={{
                    "& .MuiTab-root": {
                      textTransform: "none",
                      fontWeight: 600,
                      fontSize: "0.95rem",
                      minHeight: 48,
                    },
                  }}
                >
                  <Tab icon={<AutoStoriesIcon fontSize="small" />} iconPosition="start" label={`Đang đọc (${counts.READING})`} value="READING" />
                  <Tab icon={<BookmarkBorderIcon fontSize="small" />} iconPosition="start" label={`Muốn đọc (${counts.WANT_TO_READ})`} value="WANT_TO_READ" />
                  <Tab icon={<CheckCircleIcon fontSize="small" />} iconPosition="start" label={`Đã đọc (${counts.COMPLETED})`} value="COMPLETED" />
                  <Tab icon={<HistoryIcon fontSize="small" />} iconPosition="start" label={`Lịch sử đọc (${history.length})`} value="HISTORY" />
                  <Tab icon={<RemoveCircleOutlineIcon fontSize="small" />} iconPosition="start" label={`Không khả dụng${counts.UNAVAILABLE ? ` (${counts.UNAVAILABLE})` : ""}`} value="UNAVAILABLE" />
                </Tabs>
              </Box>

              {currentTab === "HISTORY" ? (
                historyLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                    <CircularProgress size={28} />
                  </Box>
                ) : history.length > 0 ? (
                  <Box>
                    {history.map((h) => (
                      <Paper
                        key={h.id}
                        elevation={0}
                        onClick={() => navigate(`/reader/${h.bookId}`)}
                        sx={{ p: 2, mb: 1.5, borderRadius: 2.5, border: "1px solid rgba(0,0,0,0.08)", display: "flex", alignItems: "center", gap: 2, cursor: "pointer", transition: "box-shadow 0.2s", "&:hover": { boxShadow: "0 4px 12px rgba(0,0,0,0.06)" } }}
                      >
                        <Avatar variant="rounded" src={h.coverImage || undefined} alt={h.bookTitle} sx={{ width: 48, height: 68, borderRadius: 1.5 }} />
                        <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                          <Typography variant="subtitle2" sx={{ fontWeight: 700 }} noWrap>
                            {h.bookTitle}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" noWrap sx={{ display: "block" }}>
                            {h.authorName} • {h.currentChapterTitle}
                          </Typography>
                          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 0.5 }}>
                            <LinearProgress variant="determinate" value={h.progressPercent} sx={{ flexGrow: 1, height: 6, borderRadius: 3 }} />
                            <Typography variant="caption" sx={{ fontWeight: 700, minWidth: 32 }}>
                              {h.progressPercent}%
                            </Typography>
                          </Box>
                        </Box>
                      </Paper>
                    ))}
                  </Box>
                ) : (
                  <EmptyState title="Chưa có lịch sử đọc" description="Mở một cuốn sách ở trang Đọc thử để bắt đầu theo dõi tiến độ tại đây." actionText="Khám phá sách" onAction={() => navigate("/books")} />
                )
              ) : (
                <>
                  {error && <ErrorBanner message={error} onRetry={() => loadShelf(currentTab)} />}

                  {loading ? (
                    <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                      <CircularProgress size={28} />
                    </Box>
                  ) : items.length > 0 ? (
                    <Box>
                      {items.map((item) =>
                        currentTab === "UNAVAILABLE" ? (
                          <UnavailableShelfItem key={item.id} item={item} onRemove={handleRemove} />
                        ) : (
                          <ShelfItem
                            key={item.id}
                            item={item}
                            currentTab={currentTab}
                            onUpdateStatus={handleUpdateStatus}
                            onOpenProgressDialog={handleOpenProgress}
                            onOpenMarkCompletedDialog={handleOpenMarkCompleted}
                            onRemove={handleRemove}
                          />
                        )
                      )}
                    </Box>
                  ) : (
                    !error && (
                      <EmptyState
                        title={emptyContent.title}
                        description={emptyContent.description}
                        actionText="Khám phá sách"
                        onAction={() => navigate("/books")}
                      />
                    )
                  )}
                </>
              )}
            </Card>
          </Box>
        </Container>

        <ProgressDialog
          open={progressDialogOpen}
          item={selectedProgressItem}
          onClose={() => setProgressDialogOpen(false)}
          onSave={handleSaveProgress}
        />

        <MarkCompletedDialog
          open={completedDialogOpen}
          item={selectedCompletedItem}
          onClose={() => setCompletedDialogOpen(false)}
          onConfirm={handleConfirmCompleted}
        />
    </Scene>
  );
}
