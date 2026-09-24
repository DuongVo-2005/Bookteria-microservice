import React, { useState, useEffect, useRef, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  TextField,
  Typography,
  IconButton,
  Avatar,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemButton,
  Divider,
  Badge,
  CircularProgress,
  Alert,
  Stack,
  Tabs,
  Tab,
  Chip,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  InputAdornment,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import SendIcon from "@mui/icons-material/Send";
import AddIcon from "@mui/icons-material/Add";
import RefreshIcon from "@mui/icons-material/Refresh";
import BlockIcon from "@mui/icons-material/Block";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import SearchIcon from "@mui/icons-material/Search";
import Scene from "./Scene";
import NewChatPopover from "../components/NewChatPopover";
import BlockConfirmDialog from "../components/friend/BlockConfirmDialog";
import {
  getMyConversations,
  createConversation,
  getMessages,
  createMessage,
  getPendingConversationRequests,
  acceptConversationRequest,
  rejectConversationRequest,
  markConversationAsRead,
  hideConversation,
} from "../services/chatService";
import { getChatErrorMessage } from "../services/chatErrorMessages";
import { getFriendStatus } from "../services/friendService";
import { getCurrentUserId } from "../services/authenticationService";
import { useToast } from "../context/ToastContext";
import { useFriend } from "../context/FriendContext";

const TABS = { ALL: "ALL", UNREAD: "UNREAD", REQUESTS: "REQUESTS" };
const PAGE_SIZE = 20;

export default function Chat() {
  const { showError, showSuccess, showInfo } = useToast();
  const { blockUser, unblockUser, socket } = useFriend();
  const location = useLocation();
  const navigate = useNavigate();
  const hasHandledStartChatRef = useRef(false);
  const myUserId = getCurrentUserId();

  const [currentTab, setCurrentTab] = useState(TABS.ALL);
  const [searchQuery, setSearchQuery] = useState("");
  const [blockDialogTarget, setBlockDialogTarget] = useState(null);
  const [message, setMessage] = useState("");
  const [newChatAnchorEl, setNewChatAnchorEl] = useState(null);
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState(null);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [messagesMap, setMessagesMap] = useState({});
  const [blockedStatus, setBlockedStatus] = useState(null); // "BLOCKED" | "BLOCKED_BY" | null
  const [convMenuAnchor, setConvMenuAnchor] = useState(null);
  const [convMenuTarget, setConvMenuTarget] = useState(null);

  // Tin nhắn chờ — nguồn thật GET /chat/conversations/requests (chỉ những
  // request mình ĐANG NHẬN, chưa accept/reject — đúng định nghĩa BE trả về).
  const [pendingRequests, setPendingRequests] = useState([]);
  const [pendingLoading, setPendingLoading] = useState(false);

  const messageContainerRef = useRef(null);
  const scrollToBottom = useCallback(() => {
    if (messageContainerRef.current) {
      messageContainerRef.current.scrollTop = messageContainerRef.current.scrollHeight;
      setTimeout(() => {
        if (messageContainerRef.current) messageContainerRef.current.scrollTop = messageContainerRef.current.scrollHeight;
      }, 100);
      setTimeout(() => {
        if (messageContainerRef.current) messageContainerRef.current.scrollTop = messageContainerRef.current.scrollHeight;
      }, 300);
    }
  }, []);

  const handleNewChatClick = (event) => setNewChatAnchorEl(event.currentTarget);
  const handleCloseNewChat = () => setNewChatAnchorEl(null);

  const handleSelectNewChatUser = async (user) => {
    let response;
    try {
      response = await createConversation({ type: "DIRECT", participantIds: [user.userId] });
    } catch (err) {
      showError(getChatErrorMessage(err));
      return;
    }

    const newConversation = response?.data?.result;
    const existingConversation = conversations.find((conv) => conv.id === newConversation.id);

    if (existingConversation) {
      setSelectedConversation(existingConversation);
    } else {
      setConversations((prev) => [newConversation, ...prev]);
      setSelectedConversation(newConversation);
    }
    setCurrentTab(TABS.ALL);
  };

  const getOtherParticipantId = useCallback((conversation) => {
    return conversation?.participants?.find((p) => p.userId !== myUserId)?.userId || null;
  }, [myUserId]);

  const refreshBlockedStatus = useCallback((otherUserId) => {
    if (!otherUserId) {
      setBlockedStatus(null);
      return;
    }
    getFriendStatus(otherUserId)
      .then((response) => {
        const status = response?.data?.result?.status;
        setBlockedStatus(status === "BLOCKED" || status === "BLOCKED_BY" ? status : null);
      })
      .catch(() => setBlockedStatus(null));
  }, []);

  useEffect(() => {
    const otherUserId = getOtherParticipantId(selectedConversation);
    refreshBlockedStatus(otherUserId);
  }, [selectedConversation, getOtherParticipantId, refreshBlockedStatus]);

  const handleOpenBlockDialog = () => {
    const otherUserId = getOtherParticipantId(selectedConversation);
    if (!otherUserId) return;
    const otherParticipant = selectedConversation.participants.find((p) => p.userId === otherUserId);
    setBlockDialogTarget({
      userId: otherUserId,
      username: otherParticipant?.username || selectedConversation.conversationName,
      avatar: otherParticipant?.avatar,
    });
  };

  const handleConfirmBlock = async (userId) => {
    try {
      await blockUser(userId);
      refreshBlockedStatus(userId);
    } catch (err) {
      // Toast lỗi đã hiện qua FriendContext
    }
  };

  const handleUnblock = async () => {
    const otherUserId = getOtherParticipantId(selectedConversation);
    if (!otherUserId) return;
    try {
      await unblockUser(otherUserId);
      refreshBlockedStatus(otherUserId);
    } catch (err) {
      // Toast lỗi đã hiện qua FriendContext
    }
  };

  // BE trả về dạng phân trang thật (PageResponse: currentPage/totalPages/data)
  // — pageToLoad=0 thay thế toàn bộ danh sách (làm mới), trang sau nối thêm
  // vào cuối (nút "Tải thêm", không tự động tải hết mọi trang cùng lúc).
  const fetchConversations = useCallback((pageToLoad = 0) => {
    if (pageToLoad === 0) {
      setLoading(true);
      setError(null);
    } else {
      setLoadingMore(true);
    }
    getMyConversations(pageToLoad, PAGE_SIZE)
      .then((response) => {
        const result = response?.data?.result;
        const data = Array.isArray(result?.data) ? result.data : [];
        setConversations((prev) => (pageToLoad === 0 ? data : [...prev, ...data]));
        setPage(result?.currentPage ?? pageToLoad);
        setTotalPages(result?.totalPages ?? 0);
      })
      .catch((err) => {
        console.error("Error fetching conversations:", err);
        setError(getChatErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
        setLoadingMore(false);
      });
  }, []);

  const handleLoadMoreConversations = () => {
    if (loadingMore || page + 1 >= totalPages) return;
    fetchConversations(page + 1);
  };

  const fetchPendingRequests = useCallback(() => {
    setPendingLoading(true);
    getPendingConversationRequests()
      .then((response) => setPendingRequests(response?.data?.result || []))
      .catch((err) => showError(getChatErrorMessage(err)))
      .finally(() => setPendingLoading(false));
  }, [showError]);

  useEffect(() => {
    fetchConversations(0);
    fetchPendingRequests();
  }, [fetchConversations, fetchPendingRequests]);

  // Mở sẵn hội thoại với 1 người cụ thể khi điều hướng từ nút "Nhắn tin" ở
  // trang hồ sơ người khác (PublicProfile) — chỉ chạy khi có state, không
  // ảnh hưởng hành vi mặc định của trang Chat khi vào trực tiếp.
  useEffect(() => {
    const targetUserId = location.state?.startChatWithUserId;
    if (!targetUserId || hasHandledStartChatRef.current) return;
    hasHandledStartChatRef.current = true;
    handleSelectNewChatUser({ userId: targetUserId });
    navigate(location.pathname, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state]);

  // Conversation PENDING mà mình là người NHẬN (chưa accept/reject) không
  // hiện ở tab "Tất cả"/"Chưa đọc" — chỉ hiện ở tab "Tin nhắn chờ" (nguồn
  // fetchPendingRequests riêng). Conversation PENDING mà mình là người GỬI
  // (đang chờ đối phương chấp nhận) vẫn hiện ở "Tất cả", có nhãn riêng.
  const visibleConversations = conversations.filter(
    (c) => !(c.status === "PENDING" && c.requestedBy && c.requestedBy !== myUserId)
  );

  // BE đã sort sẵn theo lastMessageAt DESC khi tải trang — chỉ cần sort lại
  // cục bộ để phản ánh ngay các cập nhật realtime (tin nhắn mới tới) mà
  // không phải chờ tải lại từ server.
  const sortedConversations = [...visibleConversations].sort((a, b) => {
    const aTime = new Date(a.lastMessageAt || a.modifiedDate || a.createdDate || 0).getTime();
    const bTime = new Date(b.lastMessageAt || b.modifiedDate || b.createdDate || 0).getTime();
    return bTime - aTime;
  });

  const tabConversations = currentTab === TABS.UNREAD ? sortedConversations.filter((c) => (c.unreadCount || 0) > 0) : sortedConversations;

  const normalizedQuery = searchQuery.trim().toLowerCase();
  const searchedConversations = normalizedQuery
    ? tabConversations.filter((c) => (c.conversationName || "").toLowerCase().includes(normalizedQuery))
    : tabConversations;
  const searchedPendingRequests = normalizedQuery
    ? pendingRequests.filter((r) => (r.conversationName || "").toLowerCase().includes(normalizedQuery))
    : pendingRequests;

  useEffect(() => {
    if (conversations.length > 0 && !selectedConversation) {
      const firstVisible = sortedConversations[0];
      if (firstVisible) setSelectedConversation(firstVisible);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversations, selectedConversation]);

  useEffect(() => {
    const fetchMessagesForConversation = async (conversationId) => {
      try {
        if (!messagesMap[conversationId]) {
          const response = await getMessages(conversationId);
          if (response?.data?.result) {
            const sortedMessages = [...response.data.result].sort((a, b) => new Date(a.createdDate) - new Date(b.createdDate));
            setMessagesMap((prev) => ({ ...prev, [conversationId]: sortedMessages }));
          }
        }
      } catch (err) {
        console.error(`Error fetching messages for conversation ${conversationId}:`, err);
      }
    };

    if (selectedConversation?.id) {
      fetchMessagesForConversation(selectedConversation.id);
    }
  }, [selectedConversation, messagesMap]);

  // Đánh dấu đã đọc thật (POST /conversations/{id}/read) đúng 1 lần mỗi khi
  // chuyển sang hội thoại khác — tách riêng khỏi effect tải tin nhắn phía
  // trên để không gọi lặp lại API này mỗi khi messagesMap đổi.
  useEffect(() => {
    const conversationId = selectedConversation?.id;
    if (!conversationId) return;
    setConversations((prev) => prev.map((conv) => (conv.id === conversationId ? { ...conv, unreadCount: 0 } : conv)));
    markConversationAsRead(conversationId).catch(() => {});
  }, [selectedConversation?.id]);

  const currentMessages = selectedConversation ? messagesMap[selectedConversation.id] || [] : [];
  const currentConversationId = selectedConversation?.id;
  const currentMessagesForConv = currentConversationId ? messagesMap[currentConversationId] : undefined;

  useEffect(() => {
    scrollToBottom();
  }, [currentConversationId, currentMessagesForConv, scrollToBottom]);

  // Helper function to handle incoming socket messages
  const handleIncomingMessage = useCallback(
    (message) => {
      setMessagesMap((prev) => {
        const existingMessages = prev[message.conversationId] || [];
        const messageExists = existingMessages.some((msg) => msg.id && message.id && msg.id === message.id);

        if (!messageExists) {
          const updatedMessages = [...existingMessages, message].sort((a, b) => new Date(a.createdDate) - new Date(b.createdDate));
          return { ...prev, [message.conversationId]: updatedMessages };
        }
        return prev;
      });

      const isOpenConversation = selectedConversation?.id === message.conversationId;

      setConversations((prevConversations) =>
        prevConversations.map((conv) =>
          conv.id === message.conversationId
            ? {
                ...conv,
                lastMessage: message.message,
                lastMessageAt: message.createdDate,
                unreadCount: isOpenConversation ? 0 : (conv.unreadCount || 0) + 1,
                modifiedDate: message.createdDate,
              }
            : conv
        )
      );

      // Đang mở sẵn đúng hội thoại này khi tin nhắn mới tới -> báo đã đọc
      // luôn cho server, tránh unreadCount lệch dù UI đang hiển thị 0.
      if (isOpenConversation) {
        markConversationAsRead(message.conversationId).catch(() => {});
      }
    },
    [selectedConversation]
  );

  // Đúng 1 kết nối Socket.IO dùng chung với FriendContext (useFriend().socket)
  // — Chat.jsx không tự mở kết nối riêng nữa, chỉ gắn thêm listener của mình
  // lên kết nối đã có sẵn khi đăng nhập.
  useEffect(() => {
    if (!socket) return;

    const handleMessage = (raw) => {
      const messageObject = JSON.parse(raw);
      if (messageObject?.conversationId) {
        handleIncomingMessage(messageObject);
      }
    };

    const handleMessageRequestEvent = (event) => {
      if (event?.eventType === "MESSAGE_REQUEST_RECEIVED") {
        fetchPendingRequests();
        fetchConversations(0);
      } else if (event?.eventType === "MESSAGE_REQUEST_ACCEPTED" || event?.eventType === "MESSAGE_REQUEST_REJECTED") {
        fetchConversations(0);
      }
    };

    socket.on("message", handleMessage);
    socket.on("message-request-event", handleMessageRequestEvent);
    return () => {
      socket.off("message", handleMessage);
      socket.off("message-request-event", handleMessageRequestEvent);
    };
  }, [socket, handleIncomingMessage, fetchPendingRequests, fetchConversations]);

  const handleConversationSelect = (conversation) => {
    setSelectedConversation(conversation);
  };

  const handleSendMessage = async () => {
    if (!message.trim() || !selectedConversation || blockedStatus) return;

    const messageToSend = message;
    setMessage("");

    try {
      await createMessage({ conversationId: selectedConversation.id, message: messageToSend });
    } catch (error) {
      const code = error?.response?.data?.code;
      if (code === 1011) {
        setBlockedStatus("BLOCKED");
        return;
      }
      showError(getChatErrorMessage(error));
      setMessage(messageToSend);
    }
  };

  const handleAcceptRequest = (conversationId) => {
    acceptConversationRequest(conversationId)
      .then((response) => {
        const updated = response?.data?.result;
        showSuccess("Đã chấp nhận yêu cầu nhắn tin");
        const acceptedFromPending = pendingRequests.find((r) => r.id === conversationId);
        setPendingRequests((prev) => prev.filter((r) => r.id !== conversationId));
        setConversations((prev) => {
          const exists = prev.some((c) => c.id === conversationId);
          const merged = { ...(acceptedFromPending || {}), ...(updated || {}) };
          return exists ? prev.map((c) => (c.id === conversationId ? { ...c, ...updated } : c)) : [merged, ...prev];
        });
        setCurrentTab(TABS.ALL);
        setSelectedConversation((prev) => ({ ...(acceptedFromPending || prev), ...(updated || {}) }));
      })
      .catch((err) => showError(getChatErrorMessage(err)));
  };

  const handleRejectRequest = (conversationId) => {
    rejectConversationRequest(conversationId)
      .then(() => {
        showInfo("Đã từ chối yêu cầu nhắn tin");
        setPendingRequests((prev) => prev.filter((r) => r.id !== conversationId));
        setConversations((prev) => prev.map((c) => (c.id === conversationId ? { ...c, status: "REJECTED" } : c)));
      })
      .catch((err) => showError(getChatErrorMessage(err)));
  };

  const handleOpenConvMenu = (e, conversation) => {
    e.stopPropagation();
    setConvMenuAnchor(e.currentTarget);
    setConvMenuTarget(conversation);
  };
  const handleCloseConvMenu = () => {
    setConvMenuAnchor(null);
    setConvMenuTarget(null);
  };
  const handleHideConversation = () => {
    const target = convMenuTarget;
    handleCloseConvMenu();
    if (!target) return;

    hideConversation(target.id)
      .then(() => {
        setConversations((prev) => prev.filter((c) => c.id !== target.id));
        setSelectedConversation((prev) => (prev?.id === target.id ? null : prev));
        showSuccess("Đã ẩn cuộc trò chuyện khỏi danh sách.");
      })
      .catch((err) => showError(getChatErrorMessage(err)));
  };

  const isSelectedPendingByMe = selectedConversation?.status === "PENDING" && selectedConversation?.requestedBy === myUserId;
  const isSelectedRejected = selectedConversation?.status === "REJECTED";

  return (
    <Scene>
      <Card
        sx={{
          width: "100%",
          height: "calc(100vh - 64px)",
          maxHeight: "100%",
          display: "flex",
          flexDirection: "row",
          mb: "-64px",
          overflow: "hidden",
        }}
      >
        {/* Conversations List */}
        <Box sx={{ width: 320, borderRight: 1, borderColor: "divider", display: "flex", flexDirection: "column" }}>
          <Box sx={{ p: 2, pb: 1, borderBottom: 1, borderColor: "divider", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <Typography variant="h6">Chats</Typography>
            <IconButton
              color="primary"
              size="small"
              onClick={handleNewChatClick}
              sx={{ bgcolor: "primary.light", color: "white", "&:hover": { bgcolor: "primary.main" } }}
            >
              <AddIcon fontSize="small" />
            </IconButton>
            <NewChatPopover anchorEl={newChatAnchorEl} open={Boolean(newChatAnchorEl)} onClose={handleCloseNewChat} onSelectUser={handleSelectNewChatUser} />
          </Box>

          <Tabs
            value={currentTab}
            onChange={(_, v) => setCurrentTab(v)}
            variant="fullWidth"
            sx={{ borderBottom: 1, borderColor: "divider", minHeight: 40, "& .MuiTab-root": { minHeight: 40, fontSize: "0.78rem", textTransform: "none", fontWeight: 600, px: 0.5 } }}
          >
            <Tab value={TABS.ALL} label="Tất cả" />
            <Tab value={TABS.UNREAD} label="Chưa đọc" />
            <Tab
              value={TABS.REQUESTS}
              label={
                <Badge badgeContent={pendingRequests.length} color="error" sx={{ "& .MuiBadge-badge": { right: -10, top: 2 } }}>
                  <span>Tin nhắn chờ</span>
                </Badge>
              }
            />
          </Tabs>

          <Box sx={{ p: 1.5, pb: 1 }}>
            <TextField
              size="small"
              fullWidth
              placeholder={currentTab === TABS.REQUESTS ? "Tìm người gửi..." : "Tìm hội thoại..."}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" sx={{ color: "text.secondary" }} />
                    </InputAdornment>
                  ),
                  sx: { borderRadius: 2.5, bgcolor: "rgba(0, 0, 0, 0.02)" },
                },
              }}
            />
          </Box>

          <Box sx={{ flexGrow: 1, overflowY: "auto" }}>
            {currentTab === TABS.REQUESTS ? (
              pendingLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
                  <CircularProgress size={28} />
                </Box>
              ) : searchedPendingRequests.length === 0 ? (
                <Box sx={{ p: 2, textAlign: "center" }}>
                  <Typography color="text.secondary" variant="body2">
                    {normalizedQuery ? "Không tìm thấy yêu cầu nào phù hợp." : "Không có yêu cầu nhắn tin nào đang chờ."}
                  </Typography>
                </Box>
              ) : (
                <List sx={{ width: "100%" }}>
                  {searchedPendingRequests.map((req) => (
                    <React.Fragment key={req.id}>
                      <ListItem alignItems="flex-start" sx={{ display: "block", py: 1.5 }}>
                        <Box sx={{ display: "flex", gap: 1.5, mb: 1 }}>
                          <Avatar src={req.conversationAvatar} />
                          <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                            <Typography variant="body2" sx={{ fontWeight: 700 }} noWrap>
                              {req.conversationName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                              {req.lastMessage || "…"}
                            </Typography>
                            <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 0.3 }}>
                              {(req.lastMessageAt || req.createdDate) ? new Date(req.lastMessageAt || req.createdDate).toLocaleString("vi-VN") : ""}
                            </Typography>
                          </Box>
                        </Box>
                        <Box sx={{ display: "flex", gap: 1 }}>
                          <Button size="small" variant="contained" startIcon={<CheckIcon sx={{ fontSize: 14 }} />} onClick={() => handleAcceptRequest(req.id)} sx={{ fontSize: "0.72rem", textTransform: "none", borderRadius: 1.5 }}>
                            Chấp nhận
                          </Button>
                          <Button size="small" variant="outlined" color="inherit" startIcon={<CloseIcon sx={{ fontSize: 14 }} />} onClick={() => handleRejectRequest(req.id)} sx={{ fontSize: "0.72rem", textTransform: "none", borderRadius: 1.5 }}>
                            Từ chối
                          </Button>
                        </Box>
                      </ListItem>
                      <Divider />
                    </React.Fragment>
                  ))}
                </List>
              )
            ) : (
              (() => {
                if (loading) {
                  return (
                    <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
                      <CircularProgress size={28} />
                    </Box>
                  );
                }
                if (error) {
                  return (
                    <Box sx={{ p: 2 }}>
                      <Alert severity="error" sx={{ mb: 2 }} action={<IconButton color="inherit" size="small" onClick={() => fetchConversations(0)}><RefreshIcon fontSize="small" /></IconButton>}>
                        {error}
                      </Alert>
                    </Box>
                  );
                }
                if (searchedConversations.length === 0) {
                  return (
                    <Box sx={{ p: 2, textAlign: "center" }}>
                      <Typography color="text.secondary" variant="body2">
                        {normalizedQuery
                          ? "Không tìm thấy hội thoại nào phù hợp."
                          : currentTab === TABS.UNREAD
                          ? "Không có hội thoại nào chưa đọc."
                          : "Chưa có cuộc trò chuyện nào. Bắt đầu chat mới để bắt đầu."}
                      </Typography>
                    </Box>
                  );
                }
                return (
                  <List sx={{ width: "100%" }}>
                    {searchedConversations.map((conversation) => {
                      const isOwnPending = conversation.status === "PENDING" && conversation.requestedBy === myUserId;
                      const isRejected = conversation.status === "REJECTED";
                      const isSelected = selectedConversation?.id === conversation.id;
                      return (
                        <React.Fragment key={conversation.id}>
                          <ListItem
                            alignItems="flex-start"
                            disablePadding
                            secondaryAction={
                              <IconButton size="small" onClick={(e) => handleOpenConvMenu(e, conversation)} aria-label="Tuỳ chọn">
                                <MoreVertIcon fontSize="small" />
                              </IconButton>
                            }
                          >
                            <ListItemButton
                              onClick={() => handleConversationSelect(conversation)}
                              selected={isSelected}
                              sx={{
                                pr: 5,
                                borderLeft: isSelected ? "4px solid #1976d2" : "4px solid transparent",
                                "&.Mui-selected": {
                                  bgcolor: alpha("#1976d2", 0.1),
                                  "&:hover": { bgcolor: alpha("#1976d2", 0.15) },
                                },
                              }}
                            >
                              <ListItemAvatar>
                                <Badge color="error" badgeContent={conversation.unreadCount} invisible={!conversation.unreadCount} overlap="circular">
                                  <Avatar src={conversation.conversationAvatar || ""} />
                                </Badge>
                              </ListItemAvatar>
                              <ListItemText
                                primary={
                                  <Stack direction="row" display="flex" justifyContent="space-between" alignItems="center" gap={0.5}>
                                    <Typography component="span" variant="body2" color="text.primary" noWrap sx={{ display: "inline" }}>
                                      {conversation.conversationName}
                                    </Typography>
                                    {(isOwnPending || isRejected) && (
                                      <Chip
                                        label={isOwnPending ? "Đang chờ" : "Đã từ chối"}
                                        size="small"
                                        color={isOwnPending ? "warning" : "default"}
                                        sx={{ height: 18, fontSize: "0.62rem" }}
                                      />
                                    )}
                                  </Stack>
                                }
                                secondary={
                                  <Typography sx={{ display: "inline" }} component="span" variant="body2" color="text.primary" noWrap>
                                    {conversation.lastMessage || "Bắt đầu cuộc trò chuyện"}
                                  </Typography>
                                }
                                primaryTypographyProps={{ fontWeight: conversation.unreadCount > 0 ? "bold" : "normal" }}
                                sx={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                              />
                            </ListItemButton>
                          </ListItem>
                          <Divider variant="inset" component="li" />
                        </React.Fragment>
                      );
                    })}
                    {page + 1 < totalPages && (
                      <ListItem sx={{ justifyContent: "center", py: 1.5 }}>
                        <Button size="small" onClick={handleLoadMoreConversations} disabled={loadingMore} sx={{ textTransform: "none", fontWeight: 600 }}>
                          {loadingMore ? <CircularProgress size={16} /> : "Tải thêm"}
                        </Button>
                      </ListItem>
                    )}
                  </List>
                );
              })()
            )}
          </Box>
        </Box>

        {/* Chat Area */}
        <Box sx={{ flexGrow: 1, display: "flex", flexDirection: "column" }}>
          {selectedConversation ? (
            <>
              <Box sx={{ p: 2, borderBottom: 1, borderColor: "divider", display: "flex", alignItems: "center" }}>
                <Avatar src={selectedConversation.conversationAvatar} sx={{ mr: 2 }} />
                <Typography variant="h6" sx={{ flexGrow: 1 }}>
                  {selectedConversation.conversationName}
                </Typography>
                {blockedStatus === "BLOCKED" ? (
                  <IconButton color="primary" size="small" onClick={handleUnblock} title="Bỏ chặn">
                    <LockOpenIcon fontSize="small" />
                  </IconButton>
                ) : blockedStatus !== "BLOCKED_BY" ? (
                  <IconButton color="error" size="small" onClick={handleOpenBlockDialog} title="Chặn người này">
                    <BlockIcon fontSize="small" />
                  </IconButton>
                ) : null}
              </Box>

              {blockedStatus && (
                <Alert severity="warning" sx={{ borderRadius: 0 }}>
                  {blockedStatus === "BLOCKED" ? "Bạn đã chặn người này. Bỏ chặn để có thể nhắn tin trở lại." : "Bạn không thể liên lạc với người này."}
                </Alert>
              )}
              {!blockedStatus && isSelectedPendingByMe && (
                <Alert severity="info" icon={<ForumOutlinedIcon fontSize="small" />} sx={{ borderRadius: 0 }}>
                  Đây là lời mời nhắn tin — bạn chỉ gửi được 1 tin nhắn cho tới khi người nhận chấp nhận.
                </Alert>
              )}
              {!blockedStatus && isSelectedRejected && (
                <Alert severity="error" sx={{ borderRadius: 0 }}>
                  Yêu cầu nhắn tin này đã bị từ chối, không thể tiếp tục trò chuyện.
                </Alert>
              )}

              <Box id="messageContainer" ref={messageContainerRef} sx={{ flexGrow: 1, p: 2, overflowY: "auto", display: "flex", flexDirection: "column", position: "relative" }}>
                <Box sx={{ display: "flex", flexDirection: "column", width: "100%", margin: "auto 0 0 0" }}>
                  {currentMessages.map((msg) => (
                    <Box key={msg.id} sx={{ display: "flex", justifyContent: msg.me ? "flex-end" : "flex-start", mb: 1.5 }}>
                      <Box
                        sx={{
                          maxWidth: { xs: "85%", sm: "70%" },
                          p: 1.5,
                          px: 2,
                          borderRadius: msg.me ? "18px 18px 4px 18px" : "18px 18px 18px 4px",
                          bgcolor: msg.me ? "primary.main" : "#ffffff",
                          color: msg.me ? "#ffffff" : "text.primary",
                          boxShadow: "0 2px 6px rgba(0,0,0,0.04)",
                          border: msg.me ? "none" : "1px solid rgba(0,0,0,0.06)",
                          opacity: msg.pending ? 0.7 : 1,
                        }}
                      >
                        <Typography variant="body2" sx={{ lineHeight: 1.5, wordBreak: "break-word" }}>
                          {msg.message}
                        </Typography>
                        <Stack direction="row" spacing={1} alignItems="center" justifyContent="flex-end" sx={{ mt: 0.5 }}>
                          {msg.failed && (
                            <Typography variant="caption" sx={{ color: msg.me ? "#ffcdd2" : "error.main" }}>
                              Failed to send
                            </Typography>
                          )}
                          {msg.pending && (
                            <Typography variant="caption" sx={{ opacity: 0.75 }}>
                              Sending...
                            </Typography>
                          )}
                          <Typography variant="caption" sx={{ display: "block", textAlign: "right", fontSize: "0.65rem", opacity: 0.75 }}>
                            {new Date(msg.createdDate).toLocaleString()}
                          </Typography>
                        </Stack>
                      </Box>
                    </Box>
                  ))}
                </Box>
              </Box>
              <Box
                component="form"
                sx={{ p: 2, borderTop: 1, borderColor: "divider", display: "flex" }}
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
              >
                <TextField
                  fullWidth
                  placeholder={blockedStatus || isSelectedRejected ? "Không thể nhắn tin" : "Type a message"}
                  variant="outlined"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  size="small"
                  disabled={Boolean(blockedStatus) || isSelectedRejected}
                />
                <IconButton color="primary" sx={{ ml: 1 }} onClick={handleSendMessage} disabled={!message.trim() || Boolean(blockedStatus) || isSelectedRejected}>
                  <SendIcon />
                </IconButton>
              </Box>
            </>
          ) : (
            <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
              <Typography variant="h6" color="text.secondary">
                Select a conversation to start chatting
              </Typography>
            </Box>
          )}
        </Box>
      </Card>

      <Menu anchorEl={convMenuAnchor} open={Boolean(convMenuAnchor)} onClose={handleCloseConvMenu}>
        <MenuItem onClick={handleHideConversation}>
          <ListItemIcon>
            <VisibilityOffOutlinedIcon fontSize="small" />
          </ListItemIcon>
          Ẩn cuộc trò chuyện
        </MenuItem>
      </Menu>

      <BlockConfirmDialog open={Boolean(blockDialogTarget)} user={blockDialogTarget} onClose={() => setBlockDialogTarget(null)} onConfirm={handleConfirmBlock} />
    </Scene>
  );
}
