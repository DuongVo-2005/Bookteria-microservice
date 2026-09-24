import React, { createContext, useContext, useState, useEffect, useCallback } from "react";
import { getMyInfo } from "../services/userService";
import { getCurrentUserId, isAdmin as checkIsAdmin } from "../services/authenticationService";
import { useToast } from "./ToastContext";
import { useFriend } from "./FriendContext";
import * as groupService from "../services/groupService";
import { getGroupErrorMessage } from "../services/groupErrorMessages";

// group-service (port 8089) đã có thật, xong 100% — context này gọi API
// thật, không còn dữ liệu giả lập. Chuẩn hoá lại field của member/post/
// comment sang dạng lồng nhau (user/author) để khớp với các component con
// (GroupMemberItem, GroupPostCard) đã viết theo shape đó từ trước — response
// thật của BE trả field phẳng (username/avatar, authorId/authorUsername/
// authorAvatar), không lồng nhau.
const GroupContext = createContext(undefined);

const normalizeMember = (m) => ({
  id: m.id,
  userId: m.userId,
  groupId: m.groupId,
  role: m.role,
  joinedAt: m.joinedAt,
  user: { userId: m.userId, username: m.username, avatar: m.avatar },
});

const normalizeComment = (c) => ({
  id: c.id,
  content: c.content,
  createdAt: c.createdAt,
  author: { userId: c.authorId, username: c.authorUsername, avatar: c.authorAvatar },
});

const normalizePost = (p) => ({
  id: p.id,
  groupId: p.groupId,
  content: p.content,
  bookRef: p.bookRef,
  createdAt: p.createdAt,
  likesCount: p.likesCount,
  commentsCount: p.commentsCount,
  isLiked: p.isLiked,
  // BA Backlog OPS-01 (be-report.md, bổ sung 2026-09-19): PENDING_APPROVAL |
  // PUBLISHED | REJECTED khi group.requireApproval=true — undefined (falsy)
  // ở nhóm không bật duyệt bài, coi như đã publish (hành vi cũ không đổi).
  status: p.status,
  author: { userId: p.authorId, username: p.authorUsername, avatar: p.authorAvatar },
  comments: (p.comments || []).map(normalizeComment),
});

export const GroupProvider = ({ children }) => {
  const { showSuccess, showInfo, showError } = useToast();
  const { socket } = useFriend();

  const [groups, setGroups] = useState([]);
  const [discoverPage, setDiscoverPage] = useState(0);
  const [discoverTotalPages, setDiscoverTotalPages] = useState(0);
  const [discoverTotalElements, setDiscoverTotalElements] = useState(0);
  const [discoverLoading, setDiscoverLoading] = useState(false);

  const [myGroups, setMyGroups] = useState([]);
  const [myGroupsLoading, setMyGroupsLoading] = useState(false);

  const [myProfile, setMyProfile] = useState(null);

  useEffect(() => {
    getMyInfo()
      .then((response) => setMyProfile(response?.data?.result || null))
      .catch(() => setMyProfile(null));
  }, []);

  const myUserId = getCurrentUserId();
  const myDisplayName = myProfile?.username || myUserId;
  const myAvatar = myProfile?.avatar || undefined;
  const isAdmin = checkIsAdmin();

  // pageToLoad=0 thay thế toàn bộ danh sách (đổi filter/tìm kiếm); trang sau
  // nối vào cuối — cùng pattern "Tải thêm" đã dùng cho Chat/Books.
  const fetchDiscoverGroups = useCallback(
    (search, category, pageToLoad = 0, size = 12) => {
      setDiscoverLoading(true);
      return groupService
        .getGroups(search || undefined, category || undefined, pageToLoad, size)
        .then((response) => {
          const result = response?.data?.result;
          const data = Array.isArray(result?.data) ? result.data : [];
          setGroups((prev) => (pageToLoad === 0 ? data : [...prev, ...data]));
          setDiscoverPage(result?.currentPage ?? pageToLoad);
          setDiscoverTotalPages(result?.totalPages ?? 0);
          setDiscoverTotalElements(result?.totalElements ?? 0);
        })
        .catch((err) => showError(getGroupErrorMessage(err)))
        .finally(() => setDiscoverLoading(false));
    },
    [showError]
  );

  const fetchMyGroups = useCallback(() => {
    setMyGroupsLoading(true);
    return groupService
      .getMyGroups()
      .then((response) => setMyGroups(response?.data?.result || []))
      .catch((err) => showError(getGroupErrorMessage(err)))
      .finally(() => setMyGroupsLoading(false));
  }, [showError]);

  useEffect(() => {
    fetchDiscoverGroups();
    fetchMyGroups();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // group-event realtime (cùng 1 socket dùng chung với FriendContext) — chỉ
  // dùng để biết CÓ thay đổi rồi refetch đúng danh sách, không tự dựng dữ
  // liệu từ payload (payload group-event không đủ field để render đầy đủ 1
  // GroupResponse/GroupPostResponse).
  useEffect(() => {
    if (!socket) return;
    const handleGroupEvent = (event) => {
      if (!event?.eventType) return;
      if (
        (event.eventType === "GROUP_MEMBER_JOINED" || event.eventType === "GROUP_MEMBER_LEFT" || event.eventType === "GROUP_ROLE_CHANGED") &&
        event.userId === myUserId
      ) {
        fetchMyGroups();
      }
    };
    socket.on("group-event", handleGroupEvent);
    return () => socket.off("group-event", handleGroupEvent);
  }, [socket, myUserId, fetchMyGroups]);

  const getGroupDetail = useCallback((groupId) => {
    return groupService.getGroupDetail(groupId).then((response) => response?.data?.result);
  }, []);

  const getGroupMembers = useCallback((groupId) => {
    return groupService.getGroupMembers(groupId).then((response) => (response?.data?.result || []).map(normalizeMember));
  }, []);

  const getGroupPosts = useCallback((groupId, page = 0, size = 10) => {
    return groupService.getGroupPosts(groupId, page, size).then((response) => {
      const result = response?.data?.result;
      return {
        data: (result?.data || []).map(normalizePost),
        currentPage: result?.currentPage ?? page,
        totalPages: result?.totalPages ?? 0,
        totalElements: result?.totalElements ?? 0,
      };
    });
  }, []);

  const createGroup = (input) => {
    return groupService
      .createGroup({
        name: input.name,
        description: input.description,
        category: input.category,
        visibility: input.visibility,
        avatar: input.avatar,
        coverImage: input.coverImage,
        rules: input.rules,
      })
      .then((response) => {
        const newGroup = response?.data?.result;
        setGroups((prev) => [newGroup, ...prev]);
        setMyGroups((prev) => [newGroup, ...prev]);
        showSuccess(`Đã tạo thành công nhóm "${newGroup.name}"!`);
        return newGroup;
      })
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const joinGroup = (groupId) => {
    const target = groups.find((g) => g.id === groupId) || myGroups.find((g) => g.id === groupId);
    return groupService
      .joinGroup(groupId)
      .then(() => {
        setGroups((prev) => prev.map((g) => (g.id === groupId ? { ...g, isJoined: true, currentUserRole: "MEMBER", memberCount: g.memberCount + 1 } : g)));
        showSuccess(`Bạn đã tham gia nhóm "${target?.name || ""}"!`);
        return fetchMyGroups();
      })
      .catch((err) => showError(getGroupErrorMessage(err)));
  };

  const leaveGroup = (groupId) => {
    const target = groups.find((g) => g.id === groupId) || myGroups.find((g) => g.id === groupId);
    if (target?.currentUserRole === "OWNER") {
      showError("Trưởng nhóm không thể rời nhóm. Vui lòng chuyển giao quyền quản trị trước.");
      return Promise.resolve();
    }
    return groupService
      .leaveGroup(groupId)
      .then(() => {
        setGroups((prev) =>
          prev.map((g) => (g.id === groupId ? { ...g, isJoined: false, currentUserRole: null, memberCount: Math.max(0, g.memberCount - 1) } : g))
        );
        setMyGroups((prev) => prev.filter((g) => g.id !== groupId));
        showInfo(`Bạn đã rời khỏi nhóm "${target?.name || ""}".`);
      })
      .catch((err) => showError(getGroupErrorMessage(err)));
  };

  const updateMemberRole = (groupId, userId, newRole) => {
    return groupService
      .changeMemberRole(groupId, userId, newRole)
      .then(() => showSuccess("Đã cập nhật vai trò thành viên."))
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const removeMember = (groupId, userId) => {
    return groupService
      .removeMember(groupId, userId)
      .then(() => {
        setGroups((prev) => prev.map((g) => (g.id === groupId ? { ...g, memberCount: Math.max(0, g.memberCount - 1) } : g)));
        showInfo("Đã xoá thành viên khỏi nhóm.");
      })
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const transferOwnership = (groupId, userId) => {
    return groupService
      .transferOwnership(groupId, userId)
      .then(() => showSuccess("Đã chuyển quyền trưởng nhóm."))
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  // GroupPostCreateRequest thật chỉ nhận {content, bookId} (String) — BE tự
  // join lại thông tin sách qua bookRef trong response, FE không tự gửi
  // bookTitle/authorName/coverImage nữa.
  // OPS-01: nhóm bật requireApproval → status=PENDING_APPROVAL, thông báo và
  // gọi onCreated khác đi (không tự chèn vào feed hiện tại — bài chưa publish).
  const createPost = (groupId, content, bookId) => {
    return groupService
      .createGroupPost(groupId, content, bookId)
      .then((response) => {
        const post = normalizePost(response?.data?.result);
        showSuccess(post.status === "PENDING_APPROVAL" ? "Đã gửi bài viết — đang chờ quản trị viên nhóm duyệt." : "Đã đăng bài viết thảo luận thành công!");
        return post;
      })
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const getPendingPosts = useCallback((groupId, page = 0, size = 10) => {
    return groupService.getPendingGroupPosts(groupId, page, size).then((response) => {
      const result = response?.data?.result;
      return {
        data: (result?.data || []).map(normalizePost),
        currentPage: result?.currentPage ?? page,
        totalPages: result?.totalPages ?? 0,
        totalElements: result?.totalElements ?? 0,
      };
    });
  }, []);

  const approvePost = (groupId, postId) => {
    return groupService
      .approveGroupPost(groupId, postId)
      .then(() => showSuccess("Đã duyệt bài viết."))
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const rejectPost = (groupId, postId, reason) => {
    return groupService
      .rejectGroupPost(groupId, postId, reason)
      .then(() => showInfo("Đã từ chối bài viết."))
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const toggleLikePost = (groupId, postId) => {
    return groupService.toggleLikePost(groupId, postId).catch((err) => {
      showError(getGroupErrorMessage(err));
      throw err;
    });
  };

  const addComment = (groupId, postId, content) => {
    if (!content.trim()) return Promise.resolve();
    return groupService
      .addPostComment(groupId, postId, content.trim())
      .then((response) => {
        showSuccess("Đã gửi bình luận");
        return normalizeComment(response?.data?.result);
      })
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  const deletePost = (groupId, postId) => {
    return groupService.deleteGroupPost(groupId, postId).catch((err) => {
      showError(getGroupErrorMessage(err));
      throw err;
    });
  };

  const deleteComment = (groupId, postId, commentId) => {
    return groupService.deletePostComment(groupId, postId, commentId).catch((err) => {
      showError(getGroupErrorMessage(err));
      throw err;
    });
  };

  // Phase 4 (be-report.md, 2026-09-18): group OWNER HOẶC platform ADMIN.
  const deleteGroupById = (groupId) => {
    return groupService
      .deleteGroup(groupId)
      .then(() => {
        setGroups((prev) => prev.filter((g) => g.id !== groupId));
        setMyGroups((prev) => prev.filter((g) => g.id !== groupId));
        showSuccess("Đã xoá hội nhóm.");
      })
      .catch((err) => {
        showError(getGroupErrorMessage(err));
        throw err;
      });
  };

  return (
    <GroupContext.Provider
      value={{
        groups,
        myGroups,
        discoverPage,
        discoverTotalPages,
        discoverTotalElements,
        discoverLoading,
        myGroupsLoading,
        myUserId,
        myAvatar,
        myDisplayName,
        isAdmin,
        fetchDiscoverGroups,
        fetchMyGroups,
        getGroupDetail,
        getGroupMembers,
        getGroupPosts,
        createGroup,
        joinGroup,
        leaveGroup,
        updateMemberRole,
        removeMember,
        transferOwnership,
        createPost,
        toggleLikePost,
        addComment,
        deletePost,
        deleteComment,
        deleteGroupById,
        getPendingPosts,
        approvePost,
        rejectPost,
      }}
    >
      {children}
    </GroupContext.Provider>
  );
};

export const useGroup = () => {
  const context = useContext(GroupContext);
  if (!context) {
    throw new Error("useGroup must be used within a GroupProvider");
  }
  return context;
};
