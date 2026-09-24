import React from "react";
import { Box, Typography, Button, Slider } from "@mui/material";
import NavigateBeforeIcon from "@mui/icons-material/NavigateBefore";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";

export const ReaderFooter = ({ currentChapterIndex, totalChapters, progressPercent, onPrevChapter, onNextChapter, onSeekProgress, footerBg, textColor, borderColor }) => {
  const isFirst = currentChapterIndex <= 0;
  const isLast = currentChapterIndex >= totalChapters - 1;

  return (
    <Box
      component="footer"
      sx={{
        height: 64,
        px: { xs: 2, sm: 4 },
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: 2,
        bgcolor: footerBg,
        color: textColor,
        borderTop: `1px solid ${borderColor}`,
        position: "sticky",
        bottom: 0,
        zIndex: 1100,
        transition: "background-color 0.3s, color 0.3s, border-color 0.3s",
      }}
    >
      <Button
        size="small"
        variant="outlined"
        startIcon={<NavigateBeforeIcon />}
        onClick={onPrevChapter}
        disabled={isFirst}
        sx={{ color: "inherit", borderColor: "rgba(0,0,0,0.18)", borderRadius: 2, textTransform: "none", fontWeight: 600, flexShrink: 0, display: { xs: "none", sm: "inline-flex" } }}
      >
        Chương trước
      </Button>

      <Box sx={{ flex: 1, maxWidth: 600, mx: "auto", display: "flex", alignItems: "center", gap: 2 }}>
        <Typography variant="caption" sx={{ opacity: 0.75, minWidth: 65, textAlign: "right" }}>
          Chương {currentChapterIndex + 1}/{totalChapters}
        </Typography>

        <Slider size="small" value={progressPercent} min={0} max={100} onChange={(_, val) => onSeekProgress?.(val)} sx={{ flex: 1, color: "primary.main", "& .MuiSlider-thumb": { width: 12, height: 12 } }} />

        <Typography variant="caption" sx={{ fontWeight: 700, minWidth: 40 }}>
          {progressPercent}%
        </Typography>
      </Box>

      <Button size="small" variant="contained" color={isLast ? "success" : "primary"} endIcon={isLast ? <CheckCircleIcon /> : <NavigateNextIcon />} onClick={onNextChapter} sx={{ borderRadius: 2, textTransform: "none", fontWeight: 700, flexShrink: 0, px: 2 }}>
        {isLast ? "Hoàn thành" : "Chương sau"}
      </Button>
    </Box>
  );
};

export default ReaderFooter;
