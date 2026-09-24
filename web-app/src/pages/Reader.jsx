import React, { useState, useEffect, useCallback } from "react";
import { Box, CircularProgress, Container } from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import { useReading } from "../context/ReadingContext";
import { getBookById } from "../services/bookService";
import { getBookErrorMessage } from "../services/errorMessages";
import { getReadingErrorMessage } from "../services/readingErrorMessages";
import { shareHighlight } from "../services/readingService";
import { useToast } from "../context/ToastContext";
import { ReaderHeader } from "../components/reader/ReaderHeader";
import { ReaderTableOfContents } from "../components/reader/ReaderTableOfContents";
import { ReaderContent } from "../components/reader/ReaderContent";
import { ReaderFooter } from "../components/reader/ReaderFooter";
import { ReaderSettingsDialog } from "../components/reader/ReaderSettingsDialog";
import { ReaderBookmarksDrawer } from "../components/reader/ReaderBookmarksDrawer";
import { ShareQuoteDialog } from "../components/reader/ShareQuoteDialog";
import { EmptyState } from "../components/book/EmptyState";

const themeStyles = {
  light: { bg: "#f8fafc", contentBg: "#ffffff", text: "#1e293b", border: "rgba(0,0,0,0.08)", activeItemBg: "rgba(25, 118, 210, 0.08)" },
  sepia: { bg: "#f4e7d0", contentBg: "#fbf0d9", text: "#433422", border: "#e4d2b5", activeItemBg: "rgba(180, 83, 9, 0.12)" },
  dark: { bg: "#181a1b", contentBg: "#1e2124", text: "#e2e8f0", border: "#2e343b", activeItemBg: "rgba(255, 255, 255, 0.08)" },
  oled: { bg: "#0a0a0a", contentBg: "#000000", text: "#d4d4d8", border: "#222222", activeItemBg: "rgba(255, 255, 255, 0.1)" },
  nord: { bg: "#242933", contentBg: "#2e3440", text: "#eceff4", border: "#3b4252", activeItemBg: "rgba(136, 192, 208, 0.15)" },
};

// Trang đọc sách toàn màn hình (không bọc Scene/SideMenu — trải nghiệm đọc
// không gián đoạn). reading-service thật: danh sách chương (GET .../chapters)
// KHÔNG kèm content (tránh tải cả cuốn 1 lần) — content chỉ lấy riêng qua
// GET .../chapters/{chapterNumber} mỗi khi đổi chương. Sách chưa được seed
// chương sẽ có danh sách rỗng — không phải lỗi tải trang.
export default function Reader() {
  const { bookId } = useParams();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const {
    settings,
    updateSettings,
    getChapters,
    getChapterContent,
    getProgress,
    saveProgress,
    fetchBookmarks,
    getBookmarks,
    addBookmark,
    removeBookmark,
    isBookmarked,
    fetchHighlights,
    getHighlights,
    addHighlight,
    removeHighlight,
    updateHighlightNote,
    shareQuoteText,
    setShareQuoteText,
  } = useReading();

  const [book, setBook] = useState(null);
  const [chapters, setChapters] = useState([]);
  const [currentChapterIndex, setCurrentChapterIndex] = useState(0);
  const [currentChapterDetail, setCurrentChapterDetail] = useState(null);
  const [chapterContentLoading, setChapterContentLoading] = useState(false);
  const [currentProgressPercent, setCurrentProgressPercent] = useState(0);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [tocOpen, setTocOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [notesDrawerOpen, setNotesDrawerOpen] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    if (!bookId) return;
    setLoading(true);
    setNotFound(false);
    setCurrentChapterDetail(null);

    let loadedChapters = [];

    getBookById(bookId)
      .then((response) => {
        setBook(response.data.result);
        return getChapters(bookId);
      })
      .then((chapterList) => {
        loadedChapters = chapterList;
        setChapters(chapterList);
        return getProgress(bookId);
      })
      .then((savedProgress) => {
        let index = 0;
        let percent = 0;
        if (savedProgress && savedProgress.status !== "NOT_STARTED" && savedProgress.currentChapterIndex < loadedChapters.length) {
          index = savedProgress.currentChapterIndex;
          percent = savedProgress.progressPercent;
        }
        setCurrentChapterIndex(index);
        setCurrentProgressPercent(percent);

        if (loadedChapters[index]) {
          setChapterContentLoading(true);
          return getChapterContent(bookId, loadedChapters[index].chapterNumber).then((detail) => setCurrentChapterDetail(detail));
        }
        return null;
      })
      .then(() => {
        fetchBookmarks(bookId);
        fetchHighlights(bookId);
      })
      .catch((err) => {
        if (err.response?.status === 404) {
          setNotFound(true);
        } else {
          showError(getBookErrorMessage(err));
        }
      })
      .finally(() => {
        setLoading(false);
        setChapterContentLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookId]);

  const currentChapter = currentChapterDetail || chapters[currentChapterIndex];
  const colors = themeStyles[settings.theme] || themeStyles.sepia;

  const bookBookmarks = bookId ? getBookmarks(bookId) : [];
  const bookHighlights = bookId ? getHighlights(bookId) : [];
  const isCurrentBookmarked = Boolean(bookId && currentChapter && isBookmarked(bookId, currentChapter.id));

  const handleSelectChapter = useCallback(
    (chapter, index) => {
      setCurrentChapterIndex(index);
      setCurrentChapterDetail(null);
      setChapterContentLoading(true);
      getChapterContent(bookId, chapter.chapterNumber)
        .then((detail) => setCurrentChapterDetail(detail))
        .catch((err) => showError(getReadingErrorMessage(err)))
        .finally(() => setChapterContentLoading(false));

      const newPercent = Math.round(((index + 1) / chapters.length) * 100);
      setCurrentProgressPercent(newPercent);
      if (bookId) {
        saveProgress(bookId, chapter.id, index, newPercent);
      }
      window.scrollTo({ top: 0, behavior: "smooth" });
    },
    [bookId, chapters.length, getChapterContent, saveProgress, showError]
  );

  const handlePrevChapter = () => {
    if (currentChapterIndex > 0) {
      handleSelectChapter(chapters[currentChapterIndex - 1], currentChapterIndex - 1);
    }
  };

  const handleNextChapter = () => {
    if (currentChapterIndex < chapters.length - 1) {
      handleSelectChapter(chapters[currentChapterIndex + 1], currentChapterIndex + 1);
    }
  };

  const handleSeekProgress = (percent) => {
    setCurrentProgressPercent(percent);
    if (bookId && currentChapter) {
      saveProgress(bookId, currentChapter.id, currentChapterIndex, percent);
    }
  };

  const handleToggleBookmark = () => {
    if (!bookId || !currentChapter) return;
    if (isCurrentBookmarked) {
      const targetBm = bookBookmarks.find((b) => b.chapterId === currentChapter.id);
      if (targetBm) removeBookmark(targetBm.id).catch(() => {});
    } else {
      addBookmark({
        bookId,
        chapterId: currentChapter.id,
        chapterNumber: currentChapter.chapterNumber,
        chapterTitle: currentChapter.title,
        positionPercent: currentProgressPercent,
        snippet: currentChapter.content ? currentChapter.content.substring(0, 120) + "..." : "",
      }).catch(() => {});
    }
  };

  const handleAddHighlight = (selectedText, color) => {
    if (!bookId || !currentChapter) return;
    addHighlight({ bookId, chapterId: currentChapter.id, chapterNumber: currentChapter.chapterNumber, chapterTitle: currentChapter.title, selectedText, color }).catch(() => {});
  };

  const handleSelectBookmark = (bm) => {
    const idx = chapters.findIndex((c) => c.id === bm.chapterId);
    if (idx !== -1) handleSelectChapter(chapters[idx], idx);
  };

  const handleSelectHighlight = (hl) => {
    const idx = chapters.findIndex((c) => c.id === hl.chapterId);
    if (idx !== -1) handleSelectChapter(chapters[idx], idx);
  };

  const handleQuickShareHighlight = (highlightId) => {
    shareHighlight(highlightId)
      .then(() => showSuccess("Đã chia sẻ trích dẫn lên Bảng tin"))
      .catch((err) => showError(getReadingErrorMessage(err)));
  };

  // FEAT-04 (be-report.md, bổ sung 2026-09-19): mở ShareQuoteDialog kèm
  // highlightId khi chia sẻ từ 1 highlight đã lưu (bật được luồng tạo ảnh
  // thiệp) — chọn văn bản tuỳ ý lúc đang đọc (từ ReaderContent) không có
  // highlightId, dialog tự chỉ cho chia sẻ dạng chữ như cũ.
  const [shareHighlightId, setShareHighlightId] = useState(null);
  const handleOpenShareQuote = (text, highlightId) => {
    setShareQuoteText(text);
    setShareHighlightId(highlightId || null);
  };

  const handleToggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "80vh" }}>
        <CircularProgress />
      </Box>
    );
  }

  if (notFound || !book || chapters.length === 0 || !currentChapter) {
    return (
      <Container sx={{ py: 6 }}>
        <EmptyState
          title={chapters.length === 0 && book ? "Sách này chưa có nội dung chương" : "Không tìm thấy nội dung sách"}
          description={chapters.length === 0 && book ? "Nội dung đọc của cuốn sách này đang được cập nhật, vui lòng quay lại sau." : "Sách bạn đang tìm kiếm không khả dụng hoặc đã bị gỡ bỏ."}
          actionText="Về trang Khám phá"
          onAction={() => navigate("/books")}
        />
      </Container>
    );
  }

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: colors.bg, color: colors.text, display: "flex", flexDirection: "column", transition: "background-color 0.3s, color 0.3s" }}>
      <ReaderHeader
        book={book}
        currentChapter={currentChapter}
        settings={settings}
        onUpdateSettings={updateSettings}
        onToggleToC={() => setTocOpen(!tocOpen)}
        onOpenSettings={() => setSettingsOpen(true)}
        onToggleBookmark={handleToggleBookmark}
        isCurrentBookmarked={isCurrentBookmarked}
        onOpenNotesDrawer={() => setNotesDrawerOpen(true)}
        notesCount={bookHighlights.length + bookBookmarks.length}
        isFullscreen={isFullscreen}
        onToggleFullscreen={handleToggleFullscreen}
        headerBg={colors.bg}
        headerTextColor={colors.text}
        borderColor={colors.border}
      />

      <Box sx={{ flex: 1 }}>
        {chapterContentLoading || !currentChapterDetail ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 10 }}>
            <CircularProgress />
          </Box>
        ) : (
          <ReaderContent chapter={currentChapterDetail} settings={settings} onAddHighlight={handleAddHighlight} onOpenShareQuote={(text) => handleOpenShareQuote(text)} contentBg={colors.contentBg} textColor={colors.text} />
        )}
      </Box>

      <ReaderFooter
        currentChapterIndex={currentChapterIndex}
        totalChapters={chapters.length}
        currentChapter={currentChapter}
        progressPercent={currentProgressPercent}
        onPrevChapter={handlePrevChapter}
        onNextChapter={handleNextChapter}
        onSeekProgress={handleSeekProgress}
        footerBg={colors.bg}
        textColor={colors.text}
        borderColor={colors.border}
      />

      <ReaderTableOfContents
        open={tocOpen}
        onClose={() => setTocOpen(false)}
        book={book}
        chapters={chapters}
        currentChapterId={chapters[currentChapterIndex]?.id}
        onSelectChapter={handleSelectChapter}
        progressPercent={currentProgressPercent}
        drawerBg={colors.bg}
        textColor={colors.text}
        activeItemBg={colors.activeItemBg}
        borderColor={colors.border}
      />

      <ReaderSettingsDialog open={settingsOpen} onClose={() => setSettingsOpen(false)} settings={settings} onUpdateSettings={updateSettings} />

      <ReaderBookmarksDrawer
        open={notesDrawerOpen}
        onClose={() => setNotesDrawerOpen(false)}
        bookmarks={bookBookmarks}
        highlights={bookHighlights}
        onSelectBookmark={handleSelectBookmark}
        onSelectHighlight={handleSelectHighlight}
        onDeleteBookmark={(id) => removeBookmark(id).catch(() => {})}
        onDeleteHighlight={(id) => removeHighlight(id).catch(() => {})}
        onUpdateHighlightNote={(id, note) => updateHighlightNote(id, note).catch(() => {})}
        onShareQuote={handleOpenShareQuote}
        onQuickShareHighlight={handleQuickShareHighlight}
        drawerBg={colors.bg}
        textColor={colors.text}
        borderColor={colors.border}
      />

      <ShareQuoteDialog
        open={Boolean(shareQuoteText)}
        onClose={() => {
          setShareQuoteText(null);
          setShareHighlightId(null);
        }}
        quoteText={shareQuoteText}
        book={book}
        chapterTitle={currentChapter.title}
        highlightId={shareHighlightId}
      />
    </Box>
  );
}
