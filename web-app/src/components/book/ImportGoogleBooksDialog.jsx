import React, { useState, useEffect, useRef } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  TextField,
  InputAdornment,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Chip,
  CircularProgress,
  Alert,
  Divider,
  Tabs,
  Tab,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import SearchIcon from "@mui/icons-material/Search";
import DownloadIcon from "@mui/icons-material/Download";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import { searchGoogleBooks, importGoogleBook, batchImportGoogleBooks } from "../../services/googleBooksService";
import { getGoogleBooksErrorMessage } from "../../services/googleBooksErrorMessages";

const SEARCH_DEBOUNCE_MS = 500; // theo đúng khuyến nghị BE, tránh dồn quota Google

// Admin tìm & nhập sách thật từ Google Books — POST /book/books/google/search
// (mọi user đăng nhập, không lưu DB) + POST /book/books/google/import/{id}
// (ADMIN, nhập 1 cuốn) + POST /book/books/google/import (ADMIN, batch theo
// từ khoá). Đã verify HTTP thật với BE (2026-09-18).
export const ImportGoogleBooksDialog = ({ open, onClose, onImported }) => {
  const [tab, setTab] = useState("SEARCH");

  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [importingId, setImportingId] = useState(null);
  const [importedIds, setImportedIds] = useState(new Set());

  const [batchQuery, setBatchQuery] = useState("");
  const [batchMaxResults, setBatchMaxResults] = useState(20);
  const [batchLoading, setBatchLoading] = useState(false);
  const [batchResult, setBatchResult] = useState(null);
  const [batchError, setBatchError] = useState(null);

  const debounceRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    if (!query.trim()) {
      setResults([]);
      setSearchError(null);
      return;
    }

    setSearching(true);
    setSearchError(null);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      searchGoogleBooks(query.trim(), 0, 20)
        .then((response) => setResults(response?.data?.result?.data || []))
        .catch((err) => {
          setSearchError(getGoogleBooksErrorMessage(err));
          setResults([]);
        })
        .finally(() => setSearching(false));
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(debounceRef.current);
  }, [query, open]);

  const handleClose = () => {
    setQuery("");
    setResults([]);
    setSearchError(null);
    setImportedIds(new Set());
    setBatchQuery("");
    setBatchResult(null);
    setBatchError(null);
    onClose();
  };

  const handleImportOne = (googleBookId) => {
    setImportingId(googleBookId);
    importGoogleBook(googleBookId)
      .then((response) => {
        setImportedIds((prev) => new Set(prev).add(googleBookId));
        onImported?.(response?.data?.result);
      })
      .catch((err) => setSearchError(getGoogleBooksErrorMessage(err)))
      .finally(() => setImportingId(null));
  };

  const handleBatchImport = () => {
    if (!batchQuery.trim()) return;
    setBatchLoading(true);
    setBatchError(null);
    setBatchResult(null);
    batchImportGoogleBooks({ query: batchQuery.trim(), maxResults: batchMaxResults, startIndex: 0 })
      .then((response) => {
        setBatchResult(response?.data?.result || {});
        onImported?.();
      })
      .catch((err) => setBatchError(getGoogleBooksErrorMessage(err)))
      .finally(() => setBatchLoading(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm" slotProps={{ paper: { sx: { borderRadius: 3, maxHeight: "85vh" } } }}>
      <DialogTitle sx={{ fontWeight: 700, display: "flex", justifyContent: "space-between", alignItems: "center", pb: 1 }}>
        <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
          Nhập sách từ Google Books
        </Typography>
        <IconButton onClick={handleClose} size="small" aria-label="Đóng">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="fullWidth" sx={{ borderBottom: 1, borderColor: "divider", px: 2 }}>
        <Tab value="SEARCH" label="Tìm & nhập từng cuốn" sx={{ textTransform: "none", fontWeight: 600 }} />
        <Tab value="BATCH" label="Nhập hàng loạt theo từ khoá" sx={{ textTransform: "none", fontWeight: 600 }} />
      </Tabs>

      {tab === "SEARCH" ? (
        <DialogContent sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
          <TextField
            autoFocus
            fullWidth
            placeholder="Tìm theo tên sách, tác giả..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="action" fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
          />

          {searchError && <Alert severity="error">{searchError}</Alert>}

          <Box sx={{ minHeight: 200 }}>
            {searching ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : results.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 3 }}>
                {query.trim() ? "Không tìm thấy kết quả nào." : "Nhập từ khoá để tìm sách trên Google Books."}
              </Typography>
            ) : (
              <List disablePadding>
                {results.map((item) => {
                  const isImported = importedIds.has(item.googleBookId);
                  return (
                    <React.Fragment key={item.googleBookId}>
                      <ListItem
                        alignItems="flex-start"
                        secondaryAction={
                          <Button
                            size="small"
                            variant={isImported ? "outlined" : "contained"}
                            color={isImported ? "success" : "primary"}
                            startIcon={isImported ? <CheckCircleIcon fontSize="small" /> : <DownloadIcon fontSize="small" />}
                            disabled={isImported || importingId === item.googleBookId}
                            onClick={() => handleImportOne(item.googleBookId)}
                            sx={{ textTransform: "none", fontWeight: 600 }}
                          >
                            {importingId === item.googleBookId ? <CircularProgress size={16} color="inherit" /> : isImported ? "Đã nhập" : "Nhập"}
                          </Button>
                        }
                      >
                        <ListItemAvatar>
                          <Avatar variant="rounded" src={item.thumbnail} sx={{ width: 40, height: 56 }}>
                            <AutoStoriesIcon fontSize="small" />
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          sx={{ pr: 10 }}
                          primary={
                            <Typography variant="body2" sx={{ fontWeight: 700 }}>
                              {item.title}
                            </Typography>
                          }
                          secondary={
                            <>
                              <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                                {(item.authors || []).join(", ")}
                              </Typography>
                              <Box sx={{ display: "flex", gap: 0.5, mt: 0.5, flexWrap: "wrap" }}>
                                {item.viewability && <Chip label={item.viewability} size="small" sx={{ height: 18, fontSize: "0.62rem" }} />}
                                {item.publicDomain && <Chip label="Public domain" size="small" color="success" sx={{ height: 18, fontSize: "0.62rem" }} />}
                              </Box>
                            </>
                          }
                        />
                      </ListItem>
                      <Divider component="li" />
                    </React.Fragment>
                  );
                })}
              </List>
            )}
          </Box>
        </DialogContent>
      ) : (
        <DialogContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Nhập cùng lúc nhiều sách khớp với từ khoá — sách đã nhập trước đó sẽ tự động bỏ qua, không lỗi, không tạo trùng.
          </Typography>

          <TextField label="Từ khoá tìm kiếm" fullWidth value={batchQuery} onChange={(e) => setBatchQuery(e.target.value)} />
          <TextField
            label="Số lượng tối đa"
            type="number"
            value={batchMaxResults}
            onChange={(e) => setBatchMaxResults(Math.max(1, Math.min(40, Number(e.target.value) || 1)))}
            sx={{ maxWidth: 160 }}
            slotProps={{ htmlInput: { min: 1, max: 40 } }}
          />

          {batchError && <Alert severity="error">{batchError}</Alert>}

          {batchResult && (
            <Alert severity="success">
              Đã xử lý xong.
              {typeof batchResult.skipped === "number" && ` Bỏ qua ${batchResult.skipped} sách đã nhập trước đó.`}
            </Alert>
          )}

          <Button
            variant="contained"
            disabled={!batchQuery.trim() || batchLoading}
            onClick={handleBatchImport}
            startIcon={batchLoading ? undefined : <DownloadIcon />}
            sx={{ alignSelf: "flex-start", textTransform: "none", fontWeight: 700 }}
          >
            {batchLoading ? <CircularProgress size={20} color="inherit" /> : "Nhập hàng loạt"}
          </Button>
        </DialogContent>
      )}

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose} color="inherit">
          Đóng
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ImportGoogleBooksDialog;
