import React, { useState, useCallback, useEffect } from "react";
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
  Avatar,
  Chip,
  Select,
  MenuItem,
  TextField,
  InputAdornment,
  Pagination,
  CircularProgress,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import PublicIcon from "@mui/icons-material/Public";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAdmin } from "../services/authenticationService";
import { getAdminGroups, updateGroupStatus } from "../services/groupService";
import { getGroupErrorMessage } from "../services/groupErrorMessages";
import { useToast } from "../context/ToastContext";
import { ErrorBanner } from "../components/book/ErrorBanner";

const PAGE_SIZE = 10;
const CATEGORY_FILTERS = ["Tất cả thể loại", "Công nghệ & Lập trình", "Tâm lý & Kỹ năng sống", "Văn học & Tiểu thuyết", "Lịch sử & Triết học", "Kinh doanh & Khởi nghiệp", "Thiết kế & Nghệ thuật"];
const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Hoạt động", color: "success" },
  { value: "FROZEN", label: "Tạm khoá đăng bài", color: "warning" },
  { value: "SUSPENDED", label: "Đã ẩn", color: "error" },
];

// BA v2 Phase 2 §2.3 (be-report.md, bổ sung 2026-09-19) — GET /groups/admin
// (ADMIN, mới) trả TẤT CẢ nhóm kể cả SUSPENDED/Private, khác GET /groups
// thường (đã tự lọc theo discovery). Group.hidden (boolean) cũ đã bị BE thay
// hoàn toàn bằng Group.status (ACTIVE/FROZEN/SUSPENDED).
export default function AdminGroups() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  useEffect(() => {
    if (!isAdmin()) {
      navigate("/");
    }
  }, [navigate]);

  const [groups, setGroups] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("Tất cả thể loại");
  const [statusFilter, setStatusFilter] = useState("");

  const loadPage = useCallback(
    (pageToLoad) => {
      setLoading(true);
      setError(null);
      const categoryParam = category === "Tất cả thể loại" ? undefined : category;
      getAdminGroups(search.trim() || undefined, categoryParam, statusFilter || undefined, pageToLoad, PAGE_SIZE)
        .then((response) => {
          const result = response.data.result;
          setGroups(result.data);
          setTotalPages(result.totalPages);
          setPage(pageToLoad);
        })
        .catch((err) => setError(getGroupErrorMessage(err)))
        .finally(() => setLoading(false));
    },
    [search, category, statusFilter]
  );

  useEffect(() => {
    const timer = setTimeout(() => loadPage(0), 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, category, statusFilter]);

  const handlePageChange = (event, value) => {
    loadPage(value - 1);
  };

  const handleStatusChange = (group, newStatus) => {
    if (newStatus === group.status) return;
    setUpdatingId(group.id);
    updateGroupStatus(group.id, newStatus)
      .then(() => {
        showSuccess(`Đã đổi trạng thái nhóm "${group.name}" thành ${STATUS_OPTIONS.find((s) => s.value === newStatus)?.label}.`);
        setGroups((prev) => prev.map((g) => (g.id === group.id ? { ...g, status: newStatus } : g)));
      })
      .catch((err) => showError(getGroupErrorMessage(err)))
      .finally(() => setUpdatingId(null));
  };

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3, fontSize: "1.05rem" }}>
          Quản lý tất cả hội nhóm trong hệ thống, kể cả nhóm riêng tư và nhóm đã ẩn
        </Typography>

        <Box sx={{ display: "flex", gap: 1.5, mb: 2, flexWrap: "wrap" }}>
          <TextField
            size="small"
            placeholder="Tìm theo tên nhóm..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            sx={{ minWidth: 220 }}
          />
          <Select size="small" value={category} onChange={(e) => setCategory(e.target.value)} sx={{ minWidth: 200 }}>
            {CATEGORY_FILTERS.map((cat) => (
              <MenuItem key={cat} value={cat}>
                {cat}
              </MenuItem>
            ))}
          </Select>
          <Select size="small" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} displayEmpty sx={{ minWidth: 180 }}>
            <MenuItem value="">Tất cả trạng thái</MenuItem>
            {STATUS_OPTIONS.map((opt) => (
              <MenuItem key={opt.value} value={opt.value}>
                {opt.label}
              </MenuItem>
            ))}
          </Select>
        </Box>

        {error && <ErrorBanner message={error} onRetry={() => loadPage(page)} />}

        <Card>
          {loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Nhóm</TableCell>
                  <TableCell>Thể loại</TableCell>
                  <TableCell>Riêng tư</TableCell>
                  <TableCell>Thành viên</TableCell>
                  <TableCell>Trạng thái</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {groups.map((group) => (
                  <TableRow key={group.id} hover>
                    <TableCell>
                      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                        <Avatar variant="rounded" src={group.avatar} sx={{ width: 36, height: 36 }} />
                        <Typography
                          variant="body2"
                          sx={{ fontWeight: 600, cursor: "pointer", "&:hover": { color: "primary.main" } }}
                          onClick={() => navigate(`/groups/${group.id}`)}
                        >
                          {group.name}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>{group.category}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        icon={group.visibility === "PUBLIC" ? <PublicIcon sx={{ fontSize: "14px !important" }} /> : <LockOutlinedIcon sx={{ fontSize: "14px !important" }} />}
                        label={group.visibility === "PUBLIC" ? "Công khai" : "Riêng tư"}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>{group.memberCount}</TableCell>
                    <TableCell>
                      <Select
                        size="small"
                        value={group.status || "ACTIVE"}
                        onChange={(e) => handleStatusChange(group, e.target.value)}
                        disabled={updatingId === group.id}
                        sx={{ minWidth: 170 }}
                      >
                        {STATUS_OPTIONS.map((opt) => (
                          <MenuItem key={opt.value} value={opt.value}>
                            <Chip size="small" label={opt.label} color={opt.color} sx={{ fontWeight: 600 }} />
                          </MenuItem>
                        ))}
                      </Select>
                    </TableCell>
                  </TableRow>
                ))}
                {groups.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">Không tìm thấy nhóm nào</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </Card>

        {totalPages > 1 && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <Pagination count={totalPages} page={page + 1} onChange={handlePageChange} color="primary" />
          </Box>
        )}
      </Container>
    </Scene>
  );
}
