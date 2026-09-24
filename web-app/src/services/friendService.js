import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const sendFriendRequest = async (receiverUsername) => {
  return await httpClient.post(
    API.FRIEND_REQUESTS,
    { receiverUsername },
    {
      headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" },
    }
  );
};

export const acceptFriendRequest = async (requestId) => {
  return await httpClient.put(
    `${API.FRIEND_REQUESTS}/${requestId}/accept`,
    {},
    {
      headers: { Authorization: `Bearer ${getToken()}` },
    }
  );
};

export const rejectFriendRequest = async (requestId) => {
  return await httpClient.put(
    `${API.FRIEND_REQUESTS}/${requestId}/reject`,
    {},
    {
      headers: { Authorization: `Bearer ${getToken()}` },
    }
  );
};

export const cancelFriendRequest = async (requestId) => {
  return await httpClient.delete(`${API.FRIEND_REQUESTS}/${requestId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const removeFriend = async (friendUserId) => {
  return await httpClient.delete(`${API.FRIENDS}/${friendUserId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getFriends = async () => {
  return await httpClient.get(API.FRIENDS, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getReceivedRequests = async () => {
  return await httpClient.get(API.FRIEND_REQUESTS_RECEIVED, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getSentRequests = async () => {
  return await httpClient.get(API.FRIEND_REQUESTS_SENT, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getFriendStatus = async (otherUserId) => {
  return await httpClient.get(`${API.FRIEND_STATUS}/${otherUserId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const blockUser = async (userId) => {
  return await httpClient.post(
    `${API.FRIEND_BLOCK}/${userId}`,
    {},
    {
      headers: { Authorization: `Bearer ${getToken()}` },
    }
  );
};

export const unblockUser = async (userId) => {
  return await httpClient.delete(`${API.FRIEND_BLOCK}/${userId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getBlockedUsers = async () => {
  return await httpClient.get(API.FRIEND_BLOCKS, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};
