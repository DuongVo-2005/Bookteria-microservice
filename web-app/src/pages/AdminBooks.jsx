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
  Avatar,
  Chip,
  IconButton,
  Button,
  Pagination,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import TravelExploreIcon from "@mui/icons-material/TravelExplore";
import MergeTypeIcon from "@mui/icons-material/MergeType";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAdmin, hasPermission } from "../services/authenticationService";
import {
  getBooks,
  getBookById,
  getAuthors,
  getCategories,
  getPublishers,
  createBook,
  updateBook,
  deleteBook,
} from "../services/bookService";
import { getBookErrorMessage } from "../services/errorMessages";
import { useToast } from "../context/ToastContext";
import { ErrorBanner } from "../components/book/ErrorBanner";
import { AdminBookFormDialog } from "../components/book/AdminBookFormDialog";
import { ImportBookContentDialog } from "../components/book/ImportBookContentDialog";
import { ImportGoogleBooksDialog } from "../components/book/ImportGoogleBooksDialog";
import { MergeBooksDialog } from "../components/book/MergeBooksDialog";

const PAGE_SIZE = 10;

// BA v2 Phase 1 P1-02 (be-report.md, bổ sung 2026-09-19) — LIBRARIAN giờ cũng
// quản lý được catalog (không chỉ ADMIN); trang này chỉ chặn theo permission,
// từng nút hành động cụ thể (sửa/xoá/nhập...) vẫn dựa vào BE 403 + toast lỗi
// nếu user có 1 vài permission trong nhóm nhưng thiếu permission cho đúng
// hành động đang bấm — không tự đoán/giấu nút theo từng permission lẻ.
const CATALOG_PERMISSIONS = [
  "book:create",
  "book:update",
  "book:delete",
  "book:import",
  "reading:import_content",
  "review:lock",
  "author:manage",
  "category:manage",
  "publisher:manage",
];

export default function AdminBooks() {
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  // Route guard — ADMIN hoặc user có ít nhất 1 permission quản lý catalog
  useEffect(() => {
    const canAccess = isAdmin() || CATALOG_PERMISSIONS.some((p) => hasPermission(p));
    if (!canAccess) {
      navigate("/");
    }
  }, [navigate]);

  const [books, setBooks] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [categories, setCategories] = useState([]);
  const [authors, setAuthors] = useState([]);
  const [publishers, setPublishers] = useState([]);

  const [formOpen, setFormOpen] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [formLoading, setFormLoading] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [importTarget, setImportTarget] = useState(null);
  const [googleImportOpen, setGoogleImportOpen] = useState(false);
  const [mergeTarget, setMergeTarget] = useState(null);

  const loadPage = useCallback((pageToLoad) => {
    setLoading(true);
    setError(null);
    getBooks(pageToLoad, PAGE_SIZE)
      .then((response) => {
        const result = response.data.result;
        setBooks(result.data);
        setTotalPages(result.totalPages);
        setPage(pageToLoad);
      })
      .catch((err) => setError(getBookErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  useEffect(() => {
    getCategories()
      .then((response) => setCategories(response.data.result?.data || response.data.result || []))
      .catch(() => setCategories([]));
    getAuthors()
      .then((response) => setAuthors(response.data.result?.data || response.data.result || []))
      .catch(() => setAuthors([]));
    getPublishers()
      .then((response) => setPublishers(response.data.result?.data || response.data.result || []))
      .catch(() => setPublishers([]));
  }, []);

  const handlePageChange = (event, value) => {
    loadPage(value - 1);
  };

  const handleOpenCreate = () => {
    setEditingBook(null);
    setFormOpen(true);
  };

  const handleOpenEdit = (bookId) => {
    setFormLoading(true);
    getBookById(bookId)
      .then((response) => {
        setEditingBook(response.data.result);
        setFormOpen(true);
      })
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setFormLoading(false));
  };

  const handleCloseForm = () => {
    setFormOpen(false);
    setEditingBook(null);
  };

  const handleSubmitForm = (bookData) => {
    const action = editingBook ? updateBook(editingBook.id, bookData) : createBook(bookData);
    action
      .then(() => {
        showSuccess(editingBook ? "Đã lưu thay đổi" : "Đã tạo sách mới thành công");
        setFormOpen(false);
        setEditingBook(null);
        loadPage(page);
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleConfirmDelete = () => {
    if (!deleteTarget) return;
    deleteBook(deleteTarget.id)
      .then(() => {
        showSuccess(`Đã xoá "${deleteTarget.title}"`);
        setDeleteTarget(null);
        loadPage(page);
      })
      .catch((err) => {
        showError(getBookErrorMessage(err));
        setDeleteTarget(null);
      });
  };

  const getMainAuthor = (book) =>
    book.authors?.find((a) => a.role === "MAIN_AUTHOR")?.name || book.authors?.[0]?.name || "—";

  return (
    <Scene>
        <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3, flexWrap: "wrap", gap: 2 }}>
            <Box>
              <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, fontSize: "1.05rem" }}>
                Tạo, sửa, xoá sách trong hệ thống
              </Typography>
            </Box>
            <Box sx={{ display: "flex", gap: 1.5 }}>
              <Button variant="outlined" startIcon={<TravelExploreIcon />} onClick={() => setGoogleImportOpen(true)}>
                Nhập từ Google Books
              </Button>
              <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate}>
                Tạo sách mới
              </Button>
            </Box>
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
                    <TableCell>Bìa</TableCell>
                    <TableCell>Tên sách</TableCell>
                    <TableCell>Tác giả chính</TableCell>
                    <TableCell>Trạng thái</TableCell>
                    <TableCell align="right">Hành động</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {books.map((book) => (
                    <TableRow key={book.id} hover>
                      <TableCell>
                        <Avatar variant="rounded" src={book.metadata?.coverImage || undefined} sx={{ width: 40, height: 56 }} />
                      </TableCell>
                      <TableCell>{book.title}</TableCell>
                      <TableCell>{getMainAuthor(book)}</TableCell>
                      <TableCell>
                        <Chip
                          label={book.status}
                          size="small"
                          color={book.status === "PUBLISHED" ? "success" : "default"}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton size="small" onClick={() => setImportTarget(book)} aria-label="Nạp nội dung sách" title="Nạp nội dung sách (ePub/PDF)">
                          <MenuBookIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => setMergeTarget(book)} aria-label="Gộp sách trùng" title="Gộp sách trùng">
                          <MergeTypeIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleOpenEdit(book.id)} aria-label="Sửa sách">
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => setDeleteTarget(book)}
                          aria-label="Xoá sách"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                  {books.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                        <Typography color="text.secondary">Chưa có sách nào</Typography>
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

        {formLoading && (
          <Box
            sx={{
              position: "fixed",
              inset: 0,
              bgcolor: "rgba(255,255,255,0.6)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 1500,
            }}
          >
            <CircularProgress />
          </Box>
        )}

        <AdminBookFormDialog
          open={formOpen}
          book={editingBook}
          categories={categories}
          authors={authors}
          publishers={publishers}
          onClose={handleCloseForm}
          onSubmit={handleSubmitForm}
        />

        <ImportGoogleBooksDialog
          open={googleImportOpen}
          onClose={() => setGoogleImportOpen(false)}
          onImported={() => {
            showSuccess("Đã nhập sách từ Google Books");
            loadPage(0);
          }}
        />

        <ImportBookContentDialog open={Boolean(importTarget)} book={importTarget} onClose={() => setImportTarget(null)} />

        <MergeBooksDialog
          open={Boolean(mergeTarget)}
          book={mergeTarget}
          onClose={() => setMergeTarget(null)}
          onMerged={() => {
            showSuccess(`Đã gộp sách trùng vào "${mergeTarget?.title}"`);
            loadPage(page);
          }}
        />

        <Dialog open={Boolean(deleteTarget)} onClose={() => setDeleteTarget(null)}>
          <DialogTitle sx={{ fontWeight: 600 }}>Xác nhận xoá sách</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Bạn có chắc muốn xoá "{deleteTarget?.title}"? Thao tác này không thể hoàn tác.
            </DialogContentText>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setDeleteTarget(null)} color="inherit">
              Huỷ
            </Button>
            <Button onClick={handleConfirmDelete} color="error" variant="contained">
              Xoá sách
            </Button>
          </DialogActions>
        </Dialog>
    </Scene>
  );
}
