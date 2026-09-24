import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Autocomplete,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  Divider,
} from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import dayjs from "dayjs";

const ROLE_OPTIONS = ["MAIN_AUTHOR", "CONTRIBUTOR", "TRANSLATOR"];
const FORMAT_OPTIONS = ["HARDCOVER", "PAPERBACK", "EBOOK", "AUDIOBOOK"];
const STATUS_OPTIONS = ["DRAFT", "PUBLISHED"];

const emptyForm = {
  title: "",
  subtitle: "",
  isbn13: "",
  description: "",
  selectedAuthors: [],
  selectedCategories: [],
  selectedPublisherId: "",
  publishedDate: null,
  pageCount: "",
  language: "",
  format: "PAPERBACK",
  edition: "",
  coverImage: "",
  status: "DRAFT",
};

const bookToForm = (book) => ({
  title: book.title || "",
  subtitle: book.subtitle || "",
  isbn13: book.isbn13 || "",
  description: book.description || "",
  selectedAuthors: (book.authors || []).map((a) => ({ authorId: a.authorId, name: a.name, role: a.role })),
  selectedCategories: (book.categories || []).map((c) => ({ categoryId: c.categoryId, name: c.name })),
  selectedPublisherId: book.publisher?.publisherId || "",
  publishedDate: book.metadata?.publishedDate ? dayjs(book.metadata.publishedDate) : null,
  pageCount: book.metadata?.pageCount ?? "",
  language: book.metadata?.language || "",
  format: book.metadata?.format || "PAPERBACK",
  edition: book.metadata?.edition || "",
  coverImage: book.metadata?.coverImage || "",
  status: book.status || "DRAFT",
});

export const AdminBookFormDialog = ({ open, book, categories, authors, publishers, onClose, onSubmit }) => {
  const [form, setForm] = useState(emptyForm);
  const isEditing = Boolean(book);

  useEffect(() => {
    if (open) {
      setForm(book ? bookToForm(book) : emptyForm);
    }
  }, [open, book]);

  const setField = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));

  const setAuthorRole = (authorId, role) => {
    setForm((prev) => ({
      ...prev,
      selectedAuthors: prev.selectedAuthors.map((a) => (a.authorId === authorId ? { ...a, role } : a)),
    }));
  };

  const handleAuthorsChange = (_, value) => {
    setForm((prev) => ({
      ...prev,
      selectedAuthors: value.map((v, idx) => {
        const existing = prev.selectedAuthors.find((a) => a.authorId === v.authorId);
        return { authorId: v.authorId, name: v.name, role: existing?.role || (idx === 0 ? "MAIN_AUTHOR" : "CONTRIBUTOR") };
      }),
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const bookData = {
      title: form.title,
      subtitle: form.subtitle || undefined,
      isbn13: form.isbn13 || undefined,
      description: form.description || undefined,
      authors: form.selectedAuthors.map((a) => ({ authorId: a.authorId, role: a.role })),
      category: form.selectedCategories.map((c) => ({ categoryId: c.categoryId })),
      metadata: {
        publishedDate: form.publishedDate ? form.publishedDate.format("YYYY-MM-DD") : undefined,
        pageCount: form.pageCount ? Number(form.pageCount) : undefined,
        language: form.language || undefined,
        format: form.format,
        edition: form.edition || undefined,
        coverImage: form.coverImage || undefined,
      },
      status: form.status,
      publishers: { publisherId: form.selectedPublisherId },
    };
    onSubmit(bookData);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle sx={{ fontWeight: 600 }}>{isEditing ? "Sửa sách" : "Tạo sách mới"}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
            <TextField
              label="Tên sách"
              required
              fullWidth
              value={form.title}
              onChange={(e) => setField("title", e.target.value)}
            />
            <TextField
              label="Phụ đề"
              fullWidth
              value={form.subtitle}
              onChange={(e) => setField("subtitle", e.target.value)}
            />
            <TextField
              label="ISBN-13"
              fullWidth
              value={form.isbn13}
              onChange={(e) => setField("isbn13", e.target.value)}
            />
            <TextField
              label="Mô tả"
              fullWidth
              multiline
              rows={3}
              value={form.description}
              onChange={(e) => setField("description", e.target.value)}
            />

            <Autocomplete
              multiple
              options={authors}
              getOptionLabel={(option) => option.name}
              isOptionEqualToValue={(option, value) => option.authorId === value.authorId}
              value={form.selectedAuthors}
              onChange={handleAuthorsChange}
              renderInput={(params) => <TextField {...params} label="Tác giả" required={form.selectedAuthors.length === 0} />}
            />
            {form.selectedAuthors.length > 0 && (
              <Box sx={{ display: "flex", flexDirection: "column", gap: 1, pl: 1 }}>
                {form.selectedAuthors.map((a) => (
                  <Box key={a.authorId} sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                    <Typography variant="body2" sx={{ minWidth: 140 }}>
                      {a.name}
                    </Typography>
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                      <InputLabel>Vai trò</InputLabel>
                      <Select
                        label="Vai trò"
                        value={a.role}
                        onChange={(e) => setAuthorRole(a.authorId, e.target.value)}
                      >
                        {ROLE_OPTIONS.map((r) => (
                          <MenuItem key={r} value={r}>
                            {r}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Box>
                ))}
              </Box>
            )}

            <Autocomplete
              multiple
              options={categories}
              getOptionLabel={(option) => option.name}
              isOptionEqualToValue={(option, value) => option.categoryId === value.categoryId}
              value={form.selectedCategories}
              onChange={(_, value) => setField("selectedCategories", value)}
              renderInput={(params) => <TextField {...params} label="Thể loại" required={form.selectedCategories.length === 0} />}
            />

            <Autocomplete
              options={publishers}
              getOptionLabel={(option) => option.name}
              isOptionEqualToValue={(option, value) => option.publisherId === value.publisherId}
              value={publishers.find((p) => p.publisherId === form.selectedPublisherId) || null}
              onChange={(_, value) => setField("selectedPublisherId", value?.publisherId || "")}
              renderInput={(params) => <TextField {...params} label="Nhà xuất bản" required />}
            />

            <Divider sx={{ my: 1 }} />
            <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
              Thông tin bổ sung
            </Typography>

            <LocalizationProvider dateAdapter={AdapterDayjs}>
              <DatePicker
                label="Ngày xuất bản"
                value={form.publishedDate}
                onChange={(newValue) => setField("publishedDate", newValue)}
                slotProps={{ textField: { fullWidth: true } }}
              />
            </LocalizationProvider>

            <TextField
              label="Số trang"
              type="number"
              fullWidth
              value={form.pageCount}
              onChange={(e) => setField("pageCount", e.target.value)}
            />
            <TextField
              label="Ngôn ngữ"
              fullWidth
              value={form.language}
              onChange={(e) => setField("language", e.target.value)}
            />
            <FormControl fullWidth>
              <InputLabel>Định dạng</InputLabel>
              <Select label="Định dạng" value={form.format} onChange={(e) => setField("format", e.target.value)}>
                {FORMAT_OPTIONS.map((f) => (
                  <MenuItem key={f} value={f}>
                    {f}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Phiên bản"
              fullWidth
              value={form.edition}
              onChange={(e) => setField("edition", e.target.value)}
            />
            <TextField
              label="URL ảnh bìa"
              fullWidth
              value={form.coverImage}
              onChange={(e) => setField("coverImage", e.target.value)}
            />
            <FormControl fullWidth>
              <InputLabel>Trạng thái</InputLabel>
              <Select label="Trạng thái" value={form.status} onChange={(e) => setField("status", e.target.value)}>
                {STATUS_OPTIONS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit">
            Huỷ
          </Button>
          <Button type="submit" variant="contained" color="primary">
            {isEditing ? "Lưu thay đổi" : "Tạo sách"}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default AdminBookFormDialog;
