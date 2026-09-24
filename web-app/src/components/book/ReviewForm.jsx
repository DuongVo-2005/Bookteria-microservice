import React, { useState } from "react";
import { Box, Typography, Rating, TextField, Button, Paper, FormHelperText, FormControlLabel, Checkbox } from "@mui/material";
import StarIcon from "@mui/icons-material/Star";
import SendIcon from "@mui/icons-material/Send";

export const ReviewForm = ({ initialRating = 0, initialContent = "", initialHasSpoiler = false, isEditing = false, onSubmit, onCancel }) => {
  const [rating, setRating] = useState(initialRating > 0 ? initialRating : null);
  const [content, setContent] = useState(initialContent);
  const [hasSpoiler, setHasSpoiler] = useState(initialHasSpoiler);
  const [error, setError] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!rating || rating === 0) {
      setError(true);
      return;
    }
    setError(false);
    onSubmit(rating, content, hasSpoiler);
  };

  return (
    <Paper
      elevation={0}
      component="form"
      onSubmit={handleSubmit}
      sx={{
        p: 2.5,
        mb: 3,
        borderRadius: 2,
        bgcolor: "#f8fafc",
        border: "1px solid rgba(0,0,0,0.08)",
      }}
    >
      <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
        {isEditing ? "Chỉnh sửa đánh giá của bạn" : "Viết đánh giá của bạn"}
      </Typography>

      <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mr: 1.5 }}>
          Đánh giá của bạn:
        </Typography>
        <Rating
          value={rating}
          onChange={(_, newValue) => {
            setRating(newValue);
            if (newValue) setError(false);
          }}
          emptyIcon={<StarIcon style={{ opacity: 0.4 }} fontSize="inherit" />}
          size="medium"
        />
      </Box>
      {error && (
        <FormHelperText error sx={{ mb: 1, fontWeight: 500 }}>
          Vui lòng chọn số sao đánh giá (từ 1 đến 5 sao)
        </FormHelperText>
      )}

      <TextField
        fullWidth
        multiline
        rows={3}
        placeholder="Chia sẻ cảm nhận của bạn về cuốn sách này (tuỳ chọn)..."
        value={content}
        onChange={(e) => setContent(e.target.value)}
        variant="outlined"
        size="small"
        sx={{
          mb: 2,
          bgcolor: "background.paper",
        }}
      />

      <FormControlLabel
        control={<Checkbox size="small" checked={hasSpoiler} onChange={(e) => setHasSpoiler(e.target.checked)} />}
        label={<Typography variant="caption">Đánh giá này có tiết lộ nội dung (spoiler)</Typography>}
        sx={{ mb: 1 }}
      />

      <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1 }}>
        {isEditing && onCancel && (
          <Button variant="text" color="inherit" onClick={onCancel}>
            Huỷ
          </Button>
        )}
        <Button type="submit" variant="contained" color="primary" endIcon={<SendIcon />}>
          {isEditing ? "Cập nhật đánh giá" : "Gửi đánh giá"}
        </Button>
      </Box>
    </Paper>
  );
};

export default ReviewForm;
