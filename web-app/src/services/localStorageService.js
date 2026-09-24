export const KEY_TOKEN = "accessToken";

export const setToken = (token) => {
  // Không lưu chuỗi "undefined"/"null" nếu response thiếu token.
  if (!token) {
    removeToken();
    return;
  }
  localStorage.setItem(KEY_TOKEN, token);
};

export const getToken = () => {
  return localStorage.getItem(KEY_TOKEN);
};

export const removeToken = () => {
  localStorage.removeItem(KEY_TOKEN);
};
