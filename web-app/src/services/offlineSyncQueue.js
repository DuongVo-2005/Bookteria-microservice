// BA v2 Phase 3 §3.1 (be-report.md, bổ sung 2026-09-22) — hàng đợi offline
// PHÍA CLIENT, thuần localStorage (per-browser, không đồng bộ giữa thiết
// bị/tab khác — đúng bản chất "hàng đợi cục bộ chờ đồng bộ", không phải
// state cần dùng chung). Chỉ dùng cho reading progress (saveProgress tự
// động chạy nền, im lặng khi lỗi từ trước — không có UI nào phụ thuộc trả
// về đồng bộ). Highlight/bookmark KHÔNG được xếp hàng ở đây — đó là hành
// động người dùng chủ động, cần item thật (có id server) để cập nhật UI
// ngay, xếp hàng offline cho 2 loại đó cần optimistic-item + reconcile phức
// tạp hơn hẳn, cố tình chưa làm (xem Known Issues trong fe-report.md).
const QUEUE_KEY = "bookteria_offline_progress_queue";

const readQueue = () => {
  try {
    const raw = localStorage.getItem(QUEUE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const writeQueue = (items) => {
  try {
    localStorage.setItem(QUEUE_KEY, JSON.stringify(items));
  } catch {
    // localStorage có thể bị chặn (private mode, đã đầy...) — im lặng bỏ
    // qua, đúng tinh thần "best-effort" đã áp dụng cho saveProgress từ trước.
  }
};

export const enqueueProgress = (item) => {
  const items = readQueue();
  items.push({ ...item, clientUpdatedAt: new Date().toISOString() });
  writeQueue(items);
};

export const hasQueuedProgress = () => readQueue().length > 0;

// syncBatchFn được truyền vào (thay vì import readingService trực tiếp) để
// tránh phụ thuộc vòng service↔queue, và dễ test độc lập.
export const flushOfflineQueue = (syncBatchFn) => {
  const items = readQueue();
  if (items.length === 0) return Promise.resolve(null);

  return syncBatchFn({ progress: items, highlights: [], bookmarks: [] }).then((response) => {
    writeQueue([]);
    return response?.data?.result || null;
  });
};
