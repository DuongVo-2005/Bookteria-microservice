import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

// report-service (port 8091) — hoàn toàn mới ở Phase 4 (be-report.md,
// 2026-09-18). ReportTargetType: POST|COMMENT|USER|GROUP|REVIEW.
export const createReport = async (targetType, targetId, reason) => {
  return await httpClient.post(API.REPORTS, { targetType, targetId, reason }, { headers: jsonHeader() });
};

// ADMIN only (BE tự chặn 403 nếu không phải ADMIN, FE không tự đoán quyền).
export const getReports = async (status, targetType, page = 0, size = 10) => {
  return await httpClient.get(API.REPORTS, { headers: authHeader(), params: { status, targetType, page, size } });
};

export const getReportById = async (id) => {
  return await httpClient.get(`${API.REPORTS}/${id}`, { headers: authHeader() });
};

// status: REVIEWED | ACTION_TAKEN | DISMISSED (PENDING không hợp lệ làm target update).
// BA Backlog GAP-03 (be-report.md, bổ sung 2026-09-18): status=ACTION_TAKEN giờ
// BẮT BUỘC kèm actionType (DELETE_POST|DELETE_COMMENT|LOCK_USER|HIDE_GROUP|
// REMOVE_REVIEW, phải khớp đúng targetType của report) — BE thực thi lệnh
// thật qua Kafka (admin-commands), không còn chỉ đổi nhãn trạng thái.
export const updateReportStatus = async (id, status, adminNote, actionType) => {
  const body = { status };
  if (adminNote && adminNote.trim()) body.adminNote = adminNote.trim();
  if (status === "ACTION_TAKEN" && actionType) body.actionType = actionType;
  return await httpClient.patch(`${API.REPORTS}/${id}/status`, body, { headers: jsonHeader() });
};
