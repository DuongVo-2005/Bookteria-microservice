import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import { io } from "socket.io-client";
import {
  sendFriendRequest as apiSendFriendRequest,
  acceptFriendRequest as apiAcceptFriendRequest,
  rejectFriendRequest as apiRejectFriendRequest,
  cancelFriendRequest as apiCancelFriendRequest,
  removeFriend as apiRemoveFriend,
  getFriends as apiGetFriends,
  getReceivedRequests as apiGetReceivedRequests,
  getSentRequests as apiGetSentRequests,
  blockUser as apiBlockUser,
  unblockUser as apiUnblockUser,
  getBlockedUsers as apiGetBlockedUsers,
  getFriendStatus as apiGetFriendStatus,
} from "../services/friendService";
import { getFriendErrorMessage } from "../services/friendErrorMessages";
import { isAuthenticated, getCurrentUserId } from "../services/authenticationService";
import { getToken } from "../services/localStorageService";
import { CONFIG } from "../configurations/configuration";
import { useToast } from "./ToastContext";

const FriendContext = createContext(undefined);

export const FriendProvider = ({ children }) => {
  const { showSuccess, showInfo, showError } = useToast();
  const location = useLocation();

  const [friends, setFriends] = useState([]);
  const [incomingRequests, setIncomingRequests] = useState([]);
  const [outgoingRequests, setOutgoingRequests] = useState([]);
  const [blockedUsers, setBlockedUsers] = useState([]);

  const hasInitRef = useRef(false);
  const socketRef = useRef(null);
  // Expose socket qua state (không chỉ ref) để các Context/trang khác
  // (Notification, Chat...) re-render và tự gắn thêm listener của riêng họ
  // lên ĐÚNG 1 kết nối Socket.IO dùng chung này — tránh mở nhiều kết nối
  // trùng lặp tới cùng server (mỗi trang/':context' tự "new io(...)" riêng).
  const [socket, setSocket] = useState(null);

  const refreshFriends = useCallback(() => {
    apiGetFriends()
      .then((response) => setFriends(response.data.result || []))
      .catch((err) => showError(getFriendErrorMessage(err)));
  }, [showError]);

  const refreshIncoming = useCallback(() => {
    apiGetReceivedRequests()
      .then((response) => setIncomingRequests(response.data.result || []))
      .catch((err) => showError(getFriendErrorMessage(err)));
  }, [showError]);

  const refreshOutgoing = useCallback(() => {
    apiGetSentRequests()
      .then((response) => setOutgoingRequests(response.data.result || []))
      .catch((err) => showError(getFriendErrorMessage(err)));
  }, [showError]);

  const refreshBlocked = useCallback(() => {
    apiGetBlockedUsers()
      .then((response) => setBlockedUsers(response.data.result || []))
      .catch((err) => showError(getFriendErrorMessage(err)));
  }, [showError]);

  // Xử lý WebSocket event theo đúng mục 6 friend-system-fe-tasks.md: mỗi loại
  // event chỉ trigger gọi lại đúng REST endpoint tương ứng để lấy dữ liệu thật,
  // không tự dựng dữ liệu từ payload (payload không đủ field: thiếu requestId,
  // username, avatar — xem mục 8 hợp đồng).
  const handleFriendEvent = useCallback(
    (event) => {
      switch (event?.eventType) {
        case "FRIEND_REQUEST_SENT":
          refreshIncoming();
          showInfo("Bạn có lời mời kết bạn mới");
          break;
        case "FRIEND_REQUEST_ACCEPTED":
          refreshFriends();
          refreshOutgoing();
          showSuccess("Lời mời kết bạn của bạn đã được chấp nhận");
          break;
        case "FRIEND_REQUEST_REJECTED":
          // Không toast nổi bật theo đúng mục 10 hợp đồng
          refreshOutgoing();
          break;
        case "FRIEND_REMOVED":
          refreshFriends();
          break;
        // ⚠️ 2 case dưới đây theo đúng shape mục 6b friend-system-fe-tasks.md —
        // payload dùng field blockerId/blockedId (khác senderId/receiverId ở
        // trên) — nhưng CHƯA verify bằng WebSocket client thật (chỉ verify
        // bằng đọc code BE), coi là "khả năng đúng", có thể cần chỉnh lại khi
        // có điều kiện test thật.
        case "FRIEND_BLOCKED": {
          const myId = getCurrentUserId();
          refreshFriends();
          if (event.blockerId === myId) {
            refreshBlocked();
          }
          break;
        }
        case "FRIEND_UNBLOCKED": {
          const myId = getCurrentUserId();
          if (event.blockerId === myId) {
            refreshBlocked();
          }
          break;
        }
        default:
          break;
      }
    },
    [refreshFriends, refreshIncoming, refreshOutgoing, refreshBlocked, showInfo, showSuccess]
  );

  // Khởi tạo dữ liệu + kết nối WebSocket đúng 1 lần đầu tiên khi đã đăng nhập.
  // Phụ thuộc location.pathname để tự bắt được thời điểm vừa đăng nhập xong
  // (Login.jsx điều hướng bằng navigate(), không reload trang, nên effect chỉ
  // chạy 1 lần lúc mount sẽ bỏ lỡ nếu không có dependency này).
  useEffect(() => {
    if (!isAuthenticated() || hasInitRef.current) return;
    hasInitRef.current = true;

    refreshFriends();
    refreshIncoming();
    refreshOutgoing();
    refreshBlocked();

    const newSocket = io(CONFIG.SOCKET_URL, {
      query: { token: getToken() },
      transports: ["websocket"],
    });
    newSocket.on("friend-event", handleFriendEvent);
    socketRef.current = newSocket;
    setSocket(newSocket);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname]);

  // Ngắt kết nối khi FriendProvider unmount (thực tế gần như không xảy ra vì
  // bọc quanh cả Router, nhưng vẫn dọn cho đúng).
  useEffect(() => {
    return () => {
      if (socketRef.current) {
        socketRef.current.disconnect();
      }
    };
  }, []);

  const sendFriendRequest = (receiverUsername) => {
    return apiSendFriendRequest(receiverUsername)
      .then((response) => {
        const result = response.data.result;
        if (result.status === "ACCEPTED") {
          // Trường hợp 2 người gửi lời mời cho nhau cùng lúc — BE tự accept
          // luôn, không tạo request PENDING mới (mục 3 friend-system-fe-tasks.md)
          showSuccess("Hai bạn đã trở thành bạn bè");
          refreshFriends();
          refreshIncoming();
        } else {
          showSuccess("Đã gửi lời mời kết bạn");
          refreshOutgoing();
        }
        return result;
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  const acceptFriendRequest = (requestId) => {
    return apiAcceptFriendRequest(requestId)
      .then((response) => {
        showSuccess("Đã chấp nhận lời mời kết bạn");
        refreshIncoming();
        refreshFriends();
        return response.data.result;
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  const rejectFriendRequest = (requestId) => {
    return apiRejectFriendRequest(requestId)
      .then((response) => {
        showInfo("Đã từ chối lời mời kết bạn");
        refreshIncoming();
        return response.data.result;
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  const cancelFriendRequest = (requestId) => {
    return apiCancelFriendRequest(requestId)
      .then(() => {
        showInfo("Đã huỷ lời mời kết bạn");
        refreshOutgoing();
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  const unfriend = (friendUserId) => {
    return apiRemoveFriend(friendUserId)
      .then(() => {
        showInfo("Đã xoá kết bạn");
        refreshFriends();
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  // POST /friends/block/{userId} — block tự huỷ friendship/request PENDING
  // đang có giữa 2 người (mục 6 hợp đồng), nên refresh cả 4 danh sách sau khi
  // thành công, không chỉ riêng blockedUsers (mục 2.1 friend-system-fe-tasks.md).
  const blockUser = (userId) => {
    return apiBlockUser(userId)
      .then(() => {
        showInfo("Đã chặn người dùng");
        refreshFriends();
        refreshIncoming();
        refreshOutgoing();
        refreshBlocked();
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  // DELETE /friends/block/{userId} — không tự khôi phục friendship/request cũ
  // (mục 11 hợp đồng), chỉ cần refresh lại danh sách block.
  const unblockUser = (userId) => {
    return apiUnblockUser(userId)
      .then(() => {
        showSuccess("Đã bỏ chặn người dùng");
        refreshBlocked();
      })
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  // GET /friends/status/{otherUserId} — dẫn xuất tại chỗ từ các danh sách đã
  // tải đủ, tương đương kết quả endpoint thật mà không cần gọi thêm request
  // riêng cho mỗi lần hiện nút. Ưu tiên kiểm tra BLOCKED trước (override mọi
  // trạng thái khác, đúng mục 12 hợp đồng).
  // ⚠️ KHÔNG thể suy ra "BLOCKED_BY" từ state có sẵn — GET /friends/blocks chỉ
  // trả người MÌNH chặn, không có API liệt kê ai đang chặn mình (mục 3.1
  // friend-system-fe-tasks.md). Cần biết chính xác BLOCKED_BY thì dùng
  // fetchFriendshipStatus() bên dưới (gọi thật GET /friends/status/{id}).
  const getFriendshipStatus = useCallback(
    (otherUserId) => {
      const myId = getCurrentUserId();
      if (!otherUserId || otherUserId === myId) return "NONE";
      if (blockedUsers.some((b) => b.userId === otherUserId)) return "BLOCKED";
      if (friends.some((f) => f.userId === otherUserId)) return "ACCEPTED";
      if (outgoingRequests.some((r) => r.receiverId === otherUserId && r.status === "PENDING")) return "PENDING_SENT";
      if (incomingRequests.some((r) => r.senderId === otherUserId && r.status === "PENDING")) return "PENDING_RECEIVED";
      return "NONE";
    },
    [blockedUsers, friends, outgoingRequests, incomingRequests]
  );

  // Bản gọi thật GET /friends/status/{otherUserId} — nguồn duy nhất biết được
  // "BLOCKED_BY" chính xác (xem giới hạn ở getFriendshipStatus phía trên).
  const fetchFriendshipStatus = (otherUserId) => {
    return apiGetFriendStatus(otherUserId)
      .then((response) => response.data.result?.status)
      .catch((err) => {
        showError(getFriendErrorMessage(err));
        throw err;
      });
  };

  return (
    <FriendContext.Provider
      value={{
        socket,
        friends,
        incomingRequests,
        outgoingRequests,
        blockedUsers,
        sendFriendRequest,
        acceptFriendRequest,
        rejectFriendRequest,
        cancelFriendRequest,
        unfriend,
        blockUser,
        unblockUser,
        getFriendshipStatus,
        fetchFriendshipStatus,
        refreshFriends,
        refreshIncoming,
        refreshOutgoing,
        refreshBlocked,
      }}
    >
      {children}
    </FriendContext.Provider>
  );
};

export const useFriend = () => {
  const context = useContext(FriendContext);
  if (!context) {
    throw new Error("useFriend must be used within a FriendProvider");
  }
  return context;
};
