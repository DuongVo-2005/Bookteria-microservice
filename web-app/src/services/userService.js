import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const getMyInfo = async () => {
  return await httpClient.get(API.MY_INFO, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const updateProfile = async (profileData) => {
  return await httpClient.put(API.UPDATE_PROFILE, profileData, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
      "Content-Type": "application/json",
    },
  });
};

export const uploadAvatar = async (formData) => {
  return await httpClient.put(API.UPDATE_AVATAR, formData, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
      "Content-Type": "multipart/form-data",
    },
  });
};

export const getUserProfile = async (userId) => {
  return await httpClient.get(`${API.USER_PROFILE}/${userId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const search = async (keyword, size = 20) => {
  return await httpClient.post(
    `${API.SEARCH_USER}?size=${size}`,
    { keyword: keyword },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};
