import React, { createContext, useContext, useState, useCallback, useEffect } from "react";
import { defaultReaderSettings } from "../data/readerSettingsDefaults";
import { useToast } from "./ToastContext";
import { getBookById } from "../services/bookService";
import * as readingService from "../services/readingService";
import { getReadingErrorMessage } from "../services/readingErrorMessages";
import { enqueueProgress, hasQueuedProgress, flushOfflineQueue } from "../services/offlineSyncQueue";

// reading-service (port 8090) đã có thật — context này gọi API thật cho
// chương sách/tiến độ đọc/dấu trang/highlight. Riêng `settings` (theme, cỡ
// chữ...) là tuỳ chọn hiển thị THUẦN PHÍA CLIENT, BE không có API lưu trữ
// cho phần này — vẫn giữ local-only như trước, không phải phần còn thiếu.
//
// ⚠️ Nội dung chương sách thật vẫn đang trong giai đoạn seed dữ liệu mẫu ở
// BE (chưa chốt import EPUB/TXT/HTML hay nhập tay) — sách chưa được seed sẽ
// có danh sách chương RỖNG, không phải lỗi.
const ReadingContext = createContext(undefined);

const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");

export const ReadingProvider = ({ children }) => {
  const { showSuccess, showInfo, showError } = useToast();

  const [settings, setSettings] = useState(defaultReaderSettings);
  const [bookmarksMap, setBookmarksMap] = useState({});
  const [highlightsMap, setHighlightsMap] = useState({});
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [shareQuoteText, setShareQuoteText] = useState(null);

  const updateSettings = useCallback((partial) => {
    setSettings((prev) => ({ ...prev, ...partial }));
  }, []);

  // GET /books/{bookId}/chapters — danh sách chương KHÔNG kèm content (tránh
  // tải cả cuốn sách trong 1 response). Rỗng nếu sách chưa được seed chương.
  const getChapters = useCallback((bookId) => {
    return readingService.getChapterList(bookId, 0, 100).then((response) => response?.data?.result?.data || []);
  }, []);

  // GET /books/{bookId}/chapters/{chapterNumber} — chi tiết đầy đủ kèm content,
  // gọi riêng mỗi khi đổi chương (không nằm sẵn trong danh sách ở trên).
  const getChapterContent = useCallback((bookId, chapterNumber) => {
    return readingService.getChapterContent(bookId, chapterNumber).then((response) => response?.data?.result);
  }, []);

  // Luôn trả về object (BE mặc định NOT_STARTED cho sách chưa từng đọc, không
  // phải 404) — không cần try/catch riêng cho "chưa có tiến độ".
  const getProgress = useCallback((bookId) => {
    return readingService.getReadingProgress(bookId).then((response) => response?.data?.result);
  }, []);

  // Đồng bộ lại hàng đợi offline — gọi khi mount (phòng khi mạng đã có lại
  // trước khi tab này mở, sự kiện "online" của trình duyệt không tự bắn lại)
  // và mỗi khi trình duyệt báo có mạng trở lại.
  const flushOfflineProgress = useCallback(() => {
    if (!hasQueuedProgress()) return;
    flushOfflineQueue(readingService.syncBatch)
      .then((result) => {
        if (result && (result.progressApplied > 0 || result.progressSkipped > 0)) {
          showInfo(`Đã đồng bộ ${result.progressApplied} tiến độ đọc lưu offline.`);
        }
      })
      .catch(() => {
        // Vẫn chưa có mạng thật hoặc lỗi tạm thời — giữ nguyên hàng đợi, thử
        // lại ở lần flush kế tiếp (mount sau hoặc sự kiện "online" sau).
      });
  }, [showInfo]);

  // §3.1 Offline Sync Batch (be-report.md, BA v2 Phase 3, 2026-09-22): trước
  // đây MỌI lỗi ở đây đều bị bỏ qua hoàn toàn (mất tiến độ nếu mất mạng lúc
  // đang đọc — hành vi cũ). Giờ chỉ lỗi KHÔNG có response (mất kết nối thật,
  // phân biệt với lỗi validate/4xx thật từ BE qua !err.response) mới được
  // xếp vào hàng đợi để đồng bộ lại — tránh giữ mãi 1 request thật sự sai.
  const saveProgress = useCallback(
    (bookId, currentChapterId, currentChapterIndex, progressPercent) => {
      const status = progressPercent >= 100 ? "COMPLETED" : "READING";
      return readingService
        .updateReadingProgress(bookId, { currentChapterId, currentChapterIndex, progressPercent, status })
        .then((response) => {
          flushOfflineProgress();
          return response;
        })
        .catch((err) => {
          if (!err.response) {
            enqueueProgress({ bookId, currentChapterId, currentChapterIndex, progressPercent, status });
          }
        });
    },
    [flushOfflineProgress]
  );

  useEffect(() => {
    flushOfflineProgress();
    window.addEventListener("online", flushOfflineProgress);
    return () => window.removeEventListener("online", flushOfflineProgress);
  }, [flushOfflineProgress]);

  // Lịch sử đọc thật (GET /progress) chỉ có bookId + tiến độ, KHÔNG có tên
  // sách/ảnh bìa/tên tác giả (đó là dữ liệu của book-service) — join thêm
  // qua getBookById cho từng cuốn. Tên chương hiện chỉ hiện dạng "Chương N"
  // (theo currentChapterIndex thật) thay vì tiêu đề chương đầy đủ, để tránh
  // phải gọi thêm 1 API/cuốn chỉ để lấy 1 chuỗi text.
  const fetchHistory = useCallback(() => {
    setHistoryLoading(true);
    return readingService
      .getReadingHistory()
      .then((response) => response?.data?.result || [])
      .then((progressList) => {
        const relevant = progressList.filter((p) => p.status && p.status !== "NOT_STARTED");
        return Promise.all(
          relevant.map((p) =>
            getBookById(p.bookId)
              .then((bookResponse) => {
                const book = bookResponse?.data?.result;
                return {
                  id: p.bookId,
                  bookId: p.bookId,
                  bookTitle: book?.title || "Sách không xác định",
                  coverImage: book?.metadata?.coverImage,
                  authorName: (book?.authors || []).map((a) => a.name).join(", "),
                  currentChapterTitle: `Chương ${p.currentChapterIndex + 1}`,
                  progressPercent: p.progressPercent,
                  lastReadAt: formatDate(p.lastReadAt),
                };
              })
              .catch(() => null)
          )
        );
      })
      .then((items) => setHistory(items.filter(Boolean)))
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false));
  }, []);

  const fetchBookmarks = useCallback((bookId) => {
    return readingService
      .getBookmarks(bookId)
      .then((response) => (response?.data?.result || []).map((b) => ({ ...b, createdAt: formatDate(b.createdAt) })))
      .then((list) => {
        setBookmarksMap((prev) => ({ ...prev, [bookId]: list }));
        return list;
      })
      .catch(() => {
        setBookmarksMap((prev) => ({ ...prev, [bookId]: [] }));
        return [];
      });
  }, []);

  const getBookmarks = useCallback((bookId) => bookmarksMap[bookId] || [], [bookmarksMap]);

  const addBookmark = useCallback(
    (bookmarkData) => {
      return readingService
        .createBookmark(bookmarkData)
        .then((response) => {
          const created = { ...response?.data?.result, createdAt: formatDate(response?.data?.result?.createdAt) };
          setBookmarksMap((prev) => ({ ...prev, [bookmarkData.bookId]: [created, ...(prev[bookmarkData.bookId] || [])] }));
          showSuccess(`Đã đánh dấu trang: ${bookmarkData.chapterTitle}`);
          return created;
        })
        .catch((err) => {
          showError(getReadingErrorMessage(err));
          throw err;
        });
    },
    [showSuccess, showError]
  );

  const removeBookmark = useCallback(
    (bookmarkId) => {
      return readingService
        .deleteBookmark(bookmarkId)
        .then(() => {
          setBookmarksMap((prev) => {
            const next = {};
            Object.entries(prev).forEach(([bookId, list]) => {
              next[bookId] = list.filter((b) => b.id !== bookmarkId);
            });
            return next;
          });
          showInfo("Đã xoá dấu trang");
        })
        .catch((err) => {
          showError(getReadingErrorMessage(err));
          throw err;
        });
    },
    [showInfo, showError]
  );

  const isBookmarked = useCallback((bookId, chapterId) => (bookmarksMap[bookId] || []).some((b) => b.chapterId === chapterId), [bookmarksMap]);

  const fetchHighlights = useCallback((bookId) => {
    return readingService
      .getHighlights(bookId)
      .then((response) => (response?.data?.result || []).map((h) => ({ ...h, createdAt: formatDate(h.createdAt) })))
      .then((list) => {
        setHighlightsMap((prev) => ({ ...prev, [bookId]: list }));
        return list;
      })
      .catch(() => {
        setHighlightsMap((prev) => ({ ...prev, [bookId]: [] }));
        return [];
      });
  }, []);

  const getHighlights = useCallback((bookId) => highlightsMap[bookId] || [], [highlightsMap]);

  const addHighlight = useCallback(
    (highlightData) => {
      return readingService
        .createHighlight(highlightData)
        .then((response) => {
          const created = { ...response?.data?.result, createdAt: formatDate(response?.data?.result?.createdAt) };
          setHighlightsMap((prev) => ({ ...prev, [highlightData.bookId]: [created, ...(prev[highlightData.bookId] || [])] }));
          showSuccess("Đã lưu đoạn tô sáng văn bản");
          return created;
        })
        .catch((err) => {
          showError(getReadingErrorMessage(err));
          throw err;
        });
    },
    [showSuccess, showError]
  );

  const removeHighlight = useCallback(
    (highlightId) => {
      return readingService
        .deleteHighlight(highlightId)
        .then(() => {
          setHighlightsMap((prev) => {
            const next = {};
            Object.entries(prev).forEach(([bookId, list]) => {
              next[bookId] = list.filter((h) => h.id !== highlightId);
            });
            return next;
          });
          showInfo("Đã xoá đoạn tô sáng");
        })
        .catch((err) => {
          showError(getReadingErrorMessage(err));
          throw err;
        });
    },
    [showInfo, showError]
  );

  const updateHighlightNote = useCallback(
    (highlightId, note) => {
      return readingService
        .updateHighlightNote(highlightId, note)
        .then((response) => {
          const updated = { ...response?.data?.result, createdAt: formatDate(response?.data?.result?.createdAt) };
          setHighlightsMap((prev) => {
            const next = {};
            Object.entries(prev).forEach(([bookId, list]) => {
              next[bookId] = list.map((h) => (h.id === highlightId ? updated : h));
            });
            return next;
          });
          showSuccess("Đã cập nhật ghi chú");
        })
        .catch((err) => {
          showError(getReadingErrorMessage(err));
          throw err;
        });
    },
    [showSuccess, showError]
  );

  return (
    <ReadingContext.Provider
      value={{
        settings,
        updateSettings,
        getChapters,
        getChapterContent,
        getProgress,
        saveProgress,
        history,
        historyLoading,
        fetchHistory,
        bookmarks: bookmarksMap,
        fetchBookmarks,
        getBookmarks,
        addBookmark,
        removeBookmark,
        isBookmarked,
        highlights: highlightsMap,
        fetchHighlights,
        getHighlights,
        addHighlight,
        removeHighlight,
        updateHighlightNote,
        shareQuoteText,
        setShareQuoteText,
      }}
    >
      {children}
    </ReadingContext.Provider>
  );
};

export const useReading = () => {
  const context = useContext(ReadingContext);
  if (!context) {
    throw new Error("useReading must be used within a ReadingProvider");
  }
  return context;
};
