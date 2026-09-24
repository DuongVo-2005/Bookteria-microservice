import React, { useState, useEffect, useCallback } from "react";
import {
  Box,
  Typography,
  Container,
  Card,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Chip,
  Button,
  Pagination,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Link as MuiLink,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAdmin } from "../services/authenticationService";
import { getReports, updateReportStatus } from "../services/reportService";
import { getReportErrorMessage } from "../services/reportErrorMessages";
import { reindexPosts, reindexGroups } from "../services/searchService";
import { getBookErrorMessage } from "../services/errorMessages";
import { useToast } from "../context/ToastContext";
import { ErrorBanner } from "../components/book/ErrorBanner";
import { EmptyState } from "../components/book/EmptyState";

const PAGE_SIZE = 10;

const TARGET_TYPES = ["POST", "COMMENT", "USER", "GROUP", "REVIEW"];
const STATUSES = ["PENDING", "REVIEWED", "ACTION_TAKEN", "DISMISSED"];
// Chỉ 3 trạng thái này hợp lệ làm target update (PENDING không hợp lệ, đúng
// contract be-report.md Phase 4: "status=PENDING không hợp lệ làm target update").
const UPDATABLE_STATUSES = ["REVIEWED", "ACTION_TAKEN", "DISMISSED"];

const STATUS_META = {
  PENDING: { label: "Đang chờ", color: "warning" },
  REVIEWED: { label: "Đã xem xét", color: "info" },
  ACTION_TAKEN: { label: "Đã xử lý", color: "success" },
  DISMISSED: { label: "Đã bỏ qua", color: "default" },
};

// BA Backlog GAP-03 (be-report.md, bổ sung 2026-09-18): actionType BẮT BUỘC
// phải khớp đúng targetType khi đánh dấu ACTION_TAKEN — BE giờ thực thi lệnh
// THẬT qua Kafka (xoá post/comment thật, khoá tài khoản thật, ẩn nhóm thật,
// gỡ đánh giá thật), không còn chỉ đổi nhãn trạng thái. FE tự chọn đúng theo
// targetType (không cho ADMIN tự chọn sai rồi mới báo lỗi 1016).
const ACTION_TYPE_BY_TARGET_TYPE = {
  POST: "DELETE_POST",
  COMMENT: "DELETE_COMMENT",
  USER: "LOCK_USER",
  GROUP: "HIDE_GROUP",
  REVIEW: "REMOVE_REVIEW",
};

const ACTION_TYPE_LABELS = {
  DELETE_POST: "Xoá bài viết này",
  DELETE_COMMENT: "Xoá bình luận này",
  LOCK_USER: "Khoá tài khoản người dùng này",
  HIDE_GROUP: "Ẩn nhóm này",
  REMOVE_REVIEW: "Gỡ đánh giá này",
};

// report-service KHÔNG tự validate/enrich targetId (quyết định phạm vi v1 đã
// xác nhận, be-report.md Phase 4) — FE chỉ có thể tự dẫn link tới đúng trang
// cho GROUP/USER (2 route sẵn có dùng thẳng ID làm path param); POST/COMMENT/
// REVIEW chưa có trang xem theo ID riêng nên chỉ hiện ID thô để ADMIN tự đối
// chiếu thủ công (đúng như BE đã ghi rõ).
const targetLink = (targetType, targetId) => {
  if (targetType === "GROUP") return `/groups/${targetId}`;
  if (targetType === "USER") return `/users/${targetId}`;
  return null;
};

export default function AdminReports() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  useEffect(() => {
    if (!isAdmin()) {
      navigate("/");
    }
  }, [navigate]);

  const [reports, setReports] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [targetTypeFilter, setTargetTypeFilter] = useState("");

  const [processTarget, setProcessTarget] = useState(null);
  const [newStatus, setNewStatus] = useState("ACTION_TAKEN");
  const [adminNote, setAdminNote] = useState("");
  const [processing, setProcessing] = useState(false);

  // §22.1 (be-report.md, 2026-09-18): công cụ vận hành ADMIN, đồng bộ lại
  // index Elasticsearch từ dữ liệu gốc — hiếm khi cần (chỉ khi nghi ngờ index
  // lệch dữ liệu thật), đặt ở đây vì đây là trang thao tác ADMIN duy nhất
  // ngoài Quản trị sách.
  const [reindexingPosts, setReindexingPosts] = useState(false);
  const [reindexingGroups, setReindexingGroups] = useState(false);

  const handleReindexPosts = () => {
    setReindexingPosts(true);
    reindexPosts()
      .then((res) => showSuccess(`Đã đánh chỉ mục lại ${res?.data?.result?.indexedCount ?? ""} bài viết.`))
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setReindexingPosts(false));
  };

  const handleReindexGroups = () => {
    setReindexingGroups(true);
    reindexGroups()
      .then((res) => showSuccess(`Đã đánh chỉ mục lại ${res?.data?.result?.indexedCount ?? ""} nhóm.`))
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setReindexingGroups(false));
  };

  const loadPage = useCallback(
    (pageToLoad) => {
      setLoading(true);
      setError(null);
      getReports(statusFilter || undefined, targetTypeFilter || undefined, pageToLoad, PAGE_SIZE)
        .then((response) => {
          const result = response.data.result;
          setReports(result.data || []);
          setTotalPages(result.totalPages || 0);
          setPage(pageToLoad);
        })
        .catch((err) => setError(getReportErrorMessage(err)))
        .finally(() => setLoading(false));
    },
    [statusFilter, targetTypeFilter]
  );

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  const handlePageChange = (_, value) => loadPage(value - 1);

  const handleOpenProcess = (report) => {
    setProcessTarget(report);
    setNewStatus("ACTION_TAKEN");
    setAdminNote("");
  };

  const handleSubmitProcess = () => {
    if (!processTarget) return;
    setProcessing(true);
    const actionType = ACTION_TYPE_BY_TARGET_TYPE[processTarget.targetType];
    updateReportStatus(processTarget.id, newStatus, adminNote, actionType)
      .then(() => {
        showSuccess(
          newStatus === "ACTION_TAKEN"
            ? `Đã cập nhật trạng thái báo cáo và thực thi: ${ACTION_TYPE_LABELS[actionType] || actionType}.`
            : "Đã cập nhật trạng thái báo cáo."
        );
        setProcessTarget(null);
        loadPage(page);
      })
      .catch((err) => showError(getReportErrorMessage(err)))
      .finally(() => setProcessing(false));
  };

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3, fontSize: "1.05rem" }}>
          report-service không tự xác thực nội dung bị báo cáo — hãy tự đối chiếu targetId với đúng service trước khi xử lý.
        </Typography>

        <Card sx={{ p: 2, mb: 3, borderRadius: 2, display: "flex", alignItems: "center", gap: 2, flexWrap: "wrap" }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            Công cụ tìm kiếm:
          </Typography>
          <Button size="small" variant="outlined" onClick={handleReindexPosts} disabled={reindexingPosts} sx={{ textTransform: "none" }}>
            {reindexingPosts ? <CircularProgress size={16} /> : "Đánh chỉ mục lại bài viết"}
          </Button>
          <Button size="small" variant="outlined" onClick={handleReindexGroups} disabled={reindexingGroups} sx={{ textTransform: "none" }}>
            {reindexingGroups ? <CircularProgress size={16} /> : "Đánh chỉ mục lại nhóm"}
          </Button>
        </Card>

        <Box sx={{ display: "flex", gap: 2, mb: 3, flexWrap: "wrap" }}>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Trạng thái</InputLabel>
            <Select label="Trạng thái" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <MenuItem value="">Tất cả</MenuItem>
              {STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {STATUS_META[s].label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>Loại nội dung</InputLabel>
            <Select label="Loại nội dung" value={targetTypeFilter} onChange={(e) => setTargetTypeFilter(e.target.value)}>
              <MenuItem value="">Tất cả</MenuItem>
              {TARGET_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        {error && <ErrorBanner message={error} onRetry={() => loadPage(page)} />}

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
            <CircularProgress />
          </Box>
        ) : reports.length === 0 ? (
          <EmptyState title="Không có báo cáo nào" description="Không có báo cáo nào khớp với bộ lọc hiện tại." />
        ) : (
          <Card sx={{ borderRadius: 3, overflow: "hidden" }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>Loại</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Nội dung</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Người báo cáo</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Lý do</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Trạng thái</TableCell>
                  <TableCell sx={{ fontWeight: 700 }} align="right">
                    Hành động
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {reports.map((r) => {
                  const link = targetLink(r.targetType, r.targetId);
                  return (
                    <TableRow key={r.id} hover>
                      <TableCell>
                        <Chip label={r.targetType} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: "0.78rem", maxWidth: 160, overflow: "hidden", textOverflow: "ellipsis" }}>
                        {link ? (
                          <MuiLink component="button" onClick={() => navigate(link)} sx={{ fontFamily: "monospace", fontSize: "0.78rem" }}>
                            {r.targetId}
                          </MuiLink>
                        ) : (
                          r.targetId
                        )}
                      </TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: "0.75rem" }}>{r.reporterId}</TableCell>
                      <TableCell sx={{ maxWidth: 260 }}>
                        <Typography variant="body2" sx={{ fontSize: "0.82rem", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                          {r.reason}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip label={STATUS_META[r.status]?.label || r.status} size="small" color={STATUS_META[r.status]?.color || "default"} />
                      </TableCell>
                      <TableCell align="right">
                        <Button size="small" variant="outlined" onClick={() => handleOpenProcess(r)} sx={{ textTransform: "none", fontWeight: 600 }}>
                          Xử lý
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Card>
        )}

        {totalPages > 1 && (
          <Box sx={{ display: "flex", justifyContent: "center", mt: 3 }}>
            <Pagination count={totalPages} page={page + 1} onChange={handlePageChange} color="primary" />
          </Box>
        )}

        <Dialog open={Boolean(processTarget)} onClose={() => !processing && setProcessTarget(null)} fullWidth maxWidth="sm">
          <DialogTitle sx={{ fontWeight: 700 }}>Xử lý báo cáo</DialogTitle>
          <DialogContent>
            {processTarget && (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                  <strong>{processTarget.targetType}</strong> — <code>{processTarget.targetId}</code>
                </Typography>
                <Typography variant="body2" sx={{ mb: 2, fontStyle: "italic" }}>
                  "{processTarget.reason}"
                </Typography>
              </>
            )}
            <FormControl fullWidth size="small" sx={{ mb: 2 }}>
              <InputLabel>Trạng thái mới</InputLabel>
              <Select label="Trạng thái mới" value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
                {UPDATABLE_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {STATUS_META[s].label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {newStatus === "ACTION_TAKEN" && processTarget && (
              <Box sx={{ mb: 2, p: 1.5, borderRadius: 2, bgcolor: "rgba(211,47,47,0.06)", border: "1px solid", borderColor: "error.light" }}>
                <Typography variant="caption" sx={{ fontWeight: 700, color: "error.main", display: "block" }}>
                  ⚠️ Hành động sẽ thực thi ngay, không thể hoàn tác:
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {ACTION_TYPE_LABELS[ACTION_TYPE_BY_TARGET_TYPE[processTarget.targetType]]}
                </Typography>
              </Box>
            )}
            <TextField fullWidth multiline minRows={2} maxRows={5} label="Ghi chú (không bắt buộc)" value={adminNote} onChange={(e) => setAdminNote(e.target.value)} />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2.5 }}>
            <Button onClick={() => setProcessTarget(null)} disabled={processing} sx={{ textTransform: "none" }}>
              Huỷ
            </Button>
            <Button variant="contained" onClick={handleSubmitProcess} disabled={processing} sx={{ textTransform: "none", fontWeight: 700 }}>
              {processing ? <CircularProgress size={18} color="inherit" /> : "Xác nhận"}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Scene>
  );
}
