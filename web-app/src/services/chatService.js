import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

// BE giờ đã trả về phân trang thật (đã xác nhận trực tiếp với source
// chat-service: ConversationController.myConversations), sort sẵn theo
// lastMessageAt DESC — không cần tự sort lại phía FE nữa cho dữ liệu mới tải.
export const getMyConversations = async (page = 0, size = 20) => {
  return await httpClient.get(`${API.MY_CONVERSATIONS}?page=${page}&size=${size}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const markConversationAsRead = async (conversationId) => {
  return await httpClient.post(
    `${API.CONVERSATION_BASE}/${conversationId}/read`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};

export const hideConversation = async (conversationId) => {
  return await httpClient.post(
    `${API.CONVERSATION_BASE}/${conversationId}/hide`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};

export const createConversation = async (data) => {
  return await httpClient.post(
    API.CREATE_CONVERSATION,
    {
      type: data.type,
      participantIds: data.participantIds,
    },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};


export const createMessage = async (data) => {
  return await httpClient.post(
    API.CREATE_MESSAGE,
    {
      conversationId: data.conversationId,
      message: data.message,
    },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};

export const getMessages = async (conversationId) => {
  return await httpClient.get(`${API.GET_CONVERSATION_MESSAGES}?conversationId=${conversationId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

// GET /chat/conversations/requests — lời mời nhắn tin (conversation PENDING)
// mình đang nhận, chưa chấp nhận/từ chối.
export const getPendingConversationRequests = async () => {
  return await httpClient.get(API.CONVERSATION_REQUESTS, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getPendingConversationRequestCount = async () => {
  return await httpClient.get(API.CONVERSATION_REQUESTS_COUNT, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const acceptConversationRequest = async (conversationId) => {
  return await httpClient.post(
    `${API.CONVERSATION_BASE}/${conversationId}/accept`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};

export const rejectConversationRequest = async (conversationId) => {
  return await httpClient.post(
    `${API.CONVERSATION_BASE}/${conversationId}/reject`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};