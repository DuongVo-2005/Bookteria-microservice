export const CONFIG = {
  API_GATEWAY: "http://localhost:8888/api/v1",
  SOCKET_URL: "http://localhost:8099",
};

export const API = {
  LOGIN: "/identity/auth/token",
  OAUTH2_EXCHANGE: "/identity/auth/oauth2/exchange",
  // BA Backlog OPS-03 (be-report.md, bổ sung 2026-09-19).
  IDENTITY_USERS: "/identity/users",
  MY_INFO: "/profile/users/my-profile",
  MY_POST: "/post/my-posts",
  // BE xác nhận thật (be-report.md, Phase 3, 2026-09-18): POST /post/ (không
  // phải /post/create như bản cũ trước Phase 3) — "Breaking-safe" với
  // /post/my-posts, giữ nguyên response shape cũ.
  CREATE_POST: "/post/",
  POST_FEED: "/post/feed",
  POST_FEED_FRIENDS: "/post/feed/friends",
  POST_BASE: "/post",
  POST_USERS: "/post/users",
  POST_COMMENTS: "/post/comments",
  UPDATE_PROFILE: "/profile/users/my-profile",
  UPDATE_AVATAR: "/profile/users/avatar",
  SEARCH_USER: "/profile/users/search",
  USER_PROFILE: "/profile/users",
  MY_CONVERSATIONS: "/chat/conversations/my-conversations",
  CREATE_CONVERSATION: "/chat/conversations/create",
  CONVERSATION_REQUESTS: "/chat/conversations/requests",
  CONVERSATION_REQUESTS_COUNT: "/chat/conversations/requests/count",
  CONVERSATION_BASE: "/chat/conversations",
  CREATE_MESSAGE: "/chat/messages/create",
  GET_CONVERSATION_MESSAGES: "/chat/messages",
  BOOKS: "/book/books",
  BOOK_DETAIL: "/book/books",
  BOOK_BY_SLUG: "/book/books/slug",
  BOOK_BY_CATEGORY: "/book/books/category",
  BOOK_BY_AUTHOR: "/book/books/author",
  BOOK_REVIEWS: "/book/books",
  REVIEW_DETAIL: "/book/reviews",
  READING_LIST: "/book/me/reading-list",
  READING_LIST_DETAIL: "/book/me/reading-list",
  // Phase 6 (be-report.md, 2026-09-18).
  RECOMMENDATIONS: "/book/me/recommendations",
  // BA Backlog Phase 3 (be-report.md, bổ sung 2026-09-19): FEAT-01/FEAT-02.
  ONBOARDING_PREFERENCES: "/book/onboarding/preferences",
  ACTIVE_READERS: "/book/reviews/active-readers",
  READING_CHALLENGE: "/book/me/reading-challenge",
  READING_STREAK: "/reading/me/streak",
  AUTHORS: "/book/authors",
  CATEGORIES: "/book/categories",
  PUBLISHERS: "/book/publishers",
  // BA v2 Phase 3 (be-report.md, bổ sung 2026-09-22): §4.3 Shelf Privacy.
  SHELF_PRIVACY: "/book/me/shelf-privacy",
  USER_READING_LIST_BASE: "/book/users",
  SYNC_BATCH: "/reading/sync-batch",
  SEARCH_BOOKS: "/search/books",
  SEARCH_BOOKS_SUGGEST: "/search/books/suggest",
  // §22.1 Search Integration (be-report.md, bổ sung vào Phase 6, 2026-09-18)
  // — verify 100% qua HTTP thật, không còn mục "chưa làm được" nào.
  SEARCH_POSTS: "/search/posts",
  SEARCH_HASHTAGS_TRENDING: "/search/hashtags/trending",
  SEARCH_GROUPS: "/search/groups",
  SEARCH_REINDEX_POSTS: "/search/admin/reindex/posts",
  SEARCH_REINDEX_GROUPS: "/search/admin/reindex/groups",
  FRIENDS: "/friend/friends",
  FRIEND_REQUESTS: "/friend/friends/requests",
  FRIEND_REQUESTS_RECEIVED: "/friend/friends/requests/received",
  FRIEND_REQUESTS_SENT: "/friend/friends/requests/sent",
  FRIEND_STATUS: "/friend/friends/status",
  FRIEND_BLOCK: "/friend/friends/block",
  FRIEND_BLOCKS: "/friend/friends/blocks",
  NOTIFICATIONS: "/notification/notifications",
  NOTIFICATIONS_UNREAD_COUNT: "/notification/notifications/unread-count",
  NOTIFICATIONS_READ_ALL: "/notification/notifications/read-all",
  GROUPS: "/group/groups",
  READING_BASE: "/reading",
  // BA Backlog GAP-05/FEAT-04 (be-report.md, bổ sung 2026-09-19).
  MEDIA_UPLOAD: "/file/media/upload",
  MEDIA_BASE: "/file/media",
  // Phase 4 (be-report.md, 2026-09-18): report-service hoàn toàn mới.
  REPORTS: "/report/reports",
  // Google Books (book-service) — BE xác nhận thật 2026-09-18, đúng path
  // "/book/books/..." (không phải "/books/..." như bản nháp trước đó).
  GOOGLE_BOOKS_SEARCH: "/book/books/google/search",
  GOOGLE_BOOKS_IMPORT: "/book/books/google/import",
};
