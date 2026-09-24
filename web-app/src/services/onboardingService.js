import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

// BA Backlog FEAT-01 (be-report.md, bổ sung 2026-09-19): Onboarding Wizard 3
// bước — chọn ≥3 category, ≥3 author, gợi ý follow. Dùng làm tín hiệu
// Cold-Start cho recommendation khi user chưa có ReadingList nào.
export const getOnboardingPreferences = async () => {
  return await httpClient.get(API.ONBOARDING_PREFERENCES, { headers: authHeader() });
};

export const setOnboardingPreferences = async (categoryIds, authorIds) => {
  return await httpClient.post(API.ONBOARDING_PREFERENCES, { categoryIds, authorIds }, { headers: jsonHeader() });
};

// Bước 3 (gợi ý follow) — BE không có hệ thống "follow" riêng, chỉ trả gợi ý
// độc giả tích cực; FE dùng lại cơ chế kết bạn đã có (không có API follow
// riêng nào được xác nhận).
export const getActiveReaders = async (limit = 5) => {
  return await httpClient.get(API.ACTIVE_READERS, { headers: authHeader(), params: { limit } });
};
