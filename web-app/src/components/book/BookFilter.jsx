import React from "react";
import { Box, Typography, FormControl, InputLabel, Select, MenuItem, Button, Paper } from "@mui/material";
import FilterListIcon from "@mui/icons-material/FilterList";
import RestartAltIcon from "@mui/icons-material/RestartAlt";

export const BookFilter = ({
  categories,
  authors,
  selectedCategory,
  selectedAuthor,
  onCategoryChange,
  onAuthorChange,
  onResetFilter,
  activeFilterCount,
}) => {
  return (
    <Box sx={{ mb: 3 }}>
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 2.5, flexWrap: "wrap", gap: 1 }}>
        <Box>
          <Typography variant="h4" component="h1" sx={{ fontWeight: 700, color: "text.primary" }}>
            Khám phá sách
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Tìm kiếm hàng ngàn đầu sách phong phú, đánh giá từ cộng đồng độc giả
          </Typography>
        </Box>
        {activeFilterCount > 0 && (
          <Button
            size="small"
            startIcon={<RestartAltIcon />}
            onClick={onResetFilter}
            color="primary"
            variant="outlined"
          >
            Xoá bộ lọc ({activeFilterCount})
          </Button>
        )}
      </Box>

      <Paper
        elevation={0}
        sx={{
          p: 2,
          borderRadius: 2,
          bgcolor: "background.paper",
          border: "1px solid rgba(0,0,0,0.08)",
          display: "flex",
          flexWrap: "wrap",
          alignItems: "center",
          gap: 2,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", color: "text.secondary", mr: 1 }}>
          <FilterListIcon fontSize="small" sx={{ mr: 0.8 }} />
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            Bộ lọc:
          </Typography>
        </Box>

        <FormControl size="small" sx={{ minWidth: 200, flex: { xs: "1 1 100%", sm: "0 1 220px" } }}>
          <InputLabel id="category-filter-label">Thể loại sách</InputLabel>
          <Select
            labelId="category-filter-label"
            value={selectedCategory}
            label="Thể loại sách"
            onChange={(e) => onCategoryChange(e.target.value)}
          >
            <MenuItem value="">
              <em>Tất cả thể loại</em>
            </MenuItem>
            {categories.map((cat) => (
              <MenuItem key={cat.categoryId} value={cat.categoryId}>
                {cat.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 200, flex: { xs: "1 1 100%", sm: "0 1 220px" } }}>
          <InputLabel id="author-filter-label">Tác giả</InputLabel>
          <Select
            labelId="author-filter-label"
            value={selectedAuthor}
            label="Tác giả"
            onChange={(e) => onAuthorChange(e.target.value)}
          >
            <MenuItem value="">
              <em>Tất cả tác giả</em>
            </MenuItem>
            {authors.map((auth) => (
              <MenuItem key={auth.authorId} value={auth.authorId}>
                {auth.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Paper>
    </Box>
  );
};

export default BookFilter;
