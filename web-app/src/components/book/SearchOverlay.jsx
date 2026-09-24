import React, { useState, useEffect } from "react";
import { Paper, List, ListItemButton, ListItemAvatar, ListItemText, Typography, Divider, Box, CircularProgress } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { useNavigate } from "react-router-dom";
import { searchBooks } from "../../services/searchService";
import { PlaceholderCover } from "./PlaceholderCover";

export const SearchOverlay = ({ query, open, onClose, anchorEl }) => {
  const navigate = useNavigate();
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [imgErrors, setImgErrors] = useState({});

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    let isMounted = true;
    const timer = setTimeout(() => {
      searchBooks(query.trim(), undefined, undefined, 0, 5)
        .then((response) => {
          if (isMounted) {
            setResults(response.data.result.data);
          }
        })
        .catch(() => {
          if (isMounted) {
            setResults([]);
          }
        })
        .finally(() => {
          if (isMounted) {
            setLoading(false);
          }
        });
    }, 450);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [query]);

  if (!open || !query.trim()) return null;

  const handleSelectBook = (bookId) => {
    onClose();
    navigate(`/books/${bookId}`);
  };

  const handleViewAll = () => {
    onClose();
    navigate(`/search?q=${encodeURIComponent(query.trim())}`);
  };

  const anchorRect = anchorEl?.getBoundingClientRect();

  return (
    <Paper
      elevation={8}
      sx={{
        position: "fixed",
        top: (anchorRect?.bottom || 64) + 6,
        left: anchorRect?.left || 200,
        width: anchorRect?.width || 360,
        maxWidth: "90vw",
        zIndex: 1400,
        borderRadius: 2,
        overflow: "hidden",
        border: "1px solid rgba(0,0,0,0.08)",
        maxHeight: 420,
        display: "flex",
        flexDirection: "column",
      }}
    >
      {loading ? (
        <Box sx={{ p: 3, display: "flex", justifyContent: "center", alignItems: "center" }}>
          <CircularProgress size={24} />
          <Typography variant="body2" sx={{ ml: 1.5 }} color="text.secondary">
            Đang tìm kiếm...
          </Typography>
        </Box>
      ) : results.length === 0 ? (
        <Box sx={{ p: 3, textAlign: "center" }}>
          <Typography variant="body2" color="text.secondary">
            Không tìm thấy kết quả nào cho "{query}"
          </Typography>
        </Box>
      ) : (
        <List disablePadding sx={{ overflowY: "auto" }}>
          {results.map((book) => {
            const authorsText =
              book.authorNames && book.authorNames.length > 0 ? book.authorNames.join(", ") : "Tác giả chưa cập nhật";
            return (
              <ListItemButton
                key={book.id}
                onClick={() => handleSelectBook(book.id)}
                sx={{ py: 1, px: 1.5, "&:hover": { bgcolor: "action.hover" } }}
              >
                <ListItemAvatar sx={{ minWidth: 46 }}>
                  {book.coverImage && !imgErrors[book.id] ? (
                    <Box
                      component="img"
                      src={book.coverImage}
                      alt={book.title}
                      onError={() => setImgErrors((prev) => ({ ...prev, [book.id]: true }))}
                      sx={{
                        width: 36,
                        height: 52,
                        objectFit: "cover",
                        borderRadius: 0.5,
                        boxShadow: "0 1px 3px rgba(0,0,0,0.2)",
                      }}
                    />
                  ) : (
                    <Box sx={{ width: 36, height: 52 }}>
                      <PlaceholderCover title={book.title} />
                    </Box>
                  )}
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 600,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {book.title}
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary">
                      {authorsText}
                    </Typography>
                  }
                />
              </ListItemButton>
            );
          })}
          <Divider />
          <ListItemButton onClick={handleViewAll} sx={{ py: 1.2, bgcolor: "action.selected" }}>
            <SearchIcon fontSize="small" sx={{ mr: 1, color: "primary.main" }} />
            <Typography variant="body2" color="primary.main" sx={{ fontWeight: 600 }}>
              Xem tất cả kết quả cho "{query}"
            </Typography>
          </ListItemButton>
        </List>
      )}
    </Paper>
  );
};

export default SearchOverlay;
