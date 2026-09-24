import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

// BA Backlog FEAT-02 (be-report.md, bổ sung 2026-09-19): Annual Reading
// Challenge (book-service) — completedBooks tính theo NĂM của completedAt,
// không phải năm hiện tại lúc gọi.
export const setReadingChallenge = async (year, targetBooks) => {
  return await httpClient.post(API.READING_CHALLENGE, { year, targetBooks }, { headers: jsonHeader() });
};

export const getReadingChallenge = async (year) => {
  return await httpClient.get(API.READING_CHALLENGE, { headers: authHeader(), params: { year } });
};

// Daily Reading Streak (reading-service) — badge 7_DAYS/30_DAYS/100_DAYS chỉ
// tính gần đúng (BE dùng "có gọi updateProgress trong ngày" thay vì
// durationSeconds>=5 phút thật do chưa có bảng ReadingSession).
export const getReadingStreak = async () => {
  return await httpClient.get(API.READING_STREAK, { headers: authHeader() });
};
