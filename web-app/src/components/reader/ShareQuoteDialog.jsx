import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Box, Typography, Button, IconButton, CircularProgress, Divider } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import ShareIcon from "@mui/icons-material/Share";
import DownloadIcon from "@mui/icons-material/Download";
import ImageIcon from "@mui/icons-material/Image";
import FormatQuoteIcon from "@mui/icons-material/FormatQuote";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import { useToast } from "../../context/ToastContext";
import { createPost } from "../../services/postService";
import { shareHighlight } from "../../services/readingService";
import { getReadingErrorMessage } from "../../services/readingErrorMessages";
import { uploadMedia, confirmMediaAttach } from "../../services/fileService";
import { getFileErrorMessage } from "../../services/fileErrorMessages";

const cardThemes = [
  { id: "sunset", name: "Hoàng hôn", bg: "linear-gradient(135deg, #f97316 0%, #db2777 100%)", gradient: ["#f97316", "#db2777"], text: "#ffffff" },
  { id: "ocean", name: "Đại dương", bg: "linear-gradient(135deg, #0284c7 0%, #1e40af 100%)", gradient: ["#0284c7", "#1e40af"], text: "#ffffff" },
  { id: "emerald", name: "Ngọc bích", bg: "linear-gradient(135deg, #059669 0%, #064e3b 100%)", gradient: ["#059669", "#064e3b"], text: "#ffffff" },
  { id: "classic", name: "Giấy cổ", bg: "linear-gradient(135deg, #fef3c7 0%, #fde68a 100%)", gradient: ["#fef3c7", "#fde68a"], text: "#451a03" },
  { id: "midnight", name: "Đêm đen", bg: "linear-gradient(135deg, #18181b 0%, #09090b 100%)", gradient: ["#18181b", "#09090b"], text: "#f4f4f5" },
];

const CARD_SIZE = 1080;

// FEAT-04 (be-report.md, bổ sung 2026-09-19): "chọn font/size/màu nền/ảnh
// nền, tự căn Tên sách + Tác giả" là việc của FE (canvas rendering), BE chỉ
// cấp dữ liệu thật + nhận ảnh lúc share. Vẽ trực tiếp bằng Canvas 2D API
// (không thêm thư viện như html2canvas — dự án chưa dùng, giữ bundle nhỏ),
// tự viết word-wrap vì Canvas không tự xuống dòng.
const wrapText = (ctx, text, maxWidth) => {
  const words = text.split(/\s+/);
  const lines = [];
  let current = "";
  words.forEach((word) => {
    const test = current ? `${current} ${word}` : word;
    if (ctx.measureText(test).width > maxWidth && current) {
      lines.push(current);
      current = word;
    } else {
      current = test;
    }
  });
  if (current) lines.push(current);
  return lines;
};

const renderQuoteCardCanvas = (theme, quoteText, bookTitle, authorText) => {
  const canvas = document.createElement("canvas");
  canvas.width = CARD_SIZE;
  canvas.height = CARD_SIZE;
  const ctx = canvas.getContext("2d");

  const gradient = ctx.createLinearGradient(0, 0, CARD_SIZE, CARD_SIZE);
  gradient.addColorStop(0, theme.gradient[0]);
  gradient.addColorStop(1, theme.gradient[1]);
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, CARD_SIZE, CARD_SIZE);

  const padding = 90;
  const maxWidth = CARD_SIZE - padding * 2;
  ctx.fillStyle = theme.text;
  ctx.textBaseline = "alphabetic";

  ctx.font = "italic 500 48px Georgia, serif";
  const lines = wrapText(ctx, `"${quoteText}"`, maxWidth);
  const lineHeight = 64;
  const quoteBlockHeight = lines.length * lineHeight;
  const startY = Math.max(padding + 60, CARD_SIZE / 2 - quoteBlockHeight / 2);
  lines.forEach((line, i) => ctx.fillText(line, padding, startY + i * lineHeight));

  const sepY = CARD_SIZE - padding - 90;
  ctx.strokeStyle = theme.text;
  ctx.globalAlpha = 0.3;
  ctx.beginPath();
  ctx.moveTo(padding, sepY);
  ctx.lineTo(CARD_SIZE - padding, sepY);
  ctx.stroke();
  ctx.globalAlpha = 1;

  ctx.font = "bold 34px Arial, sans-serif";
  ctx.fillText(bookTitle || "", padding, sepY + 50);
  ctx.font = "26px Arial, sans-serif";
  ctx.globalAlpha = 0.85;
  ctx.fillText(authorText || "", padding, sepY + 90);
  ctx.globalAlpha = 1;

  ctx.font = "bold 24px Arial, sans-serif";
  ctx.textAlign = "right";
  ctx.fillText("Bookteria", CARD_SIZE - padding, sepY + 50);
  ctx.textAlign = "left";

  return canvas;
};

const canvasToBlob = (canvas) => new Promise((resolve) => canvas.toBlob(resolve, "image/png"));

// "Chia sẻ lên Bảng tin" (chữ) gọi thật POST /post/ (post-service). "Chia sẻ
// dưới dạng ảnh" (chỉ khi có highlightId — trích dẫn từ 1 highlight đã lưu)
// vẽ canvas → upload file-service → share kèm mediaUrls → confirm-attach
// (GAP-05, bắt buộc theo đúng thứ tự nếu không ảnh sẽ tự bị xoá sau 24h).
export const ShareQuoteDialog = ({ open, onClose, quoteText, book, chapterTitle, highlightId }) => {
  const { showSuccess, showError } = useToast();
  const [selectedThemeId, setSelectedThemeId] = useState("sunset");
  const [sharing, setSharing] = useState(false);
  const [sharingImage, setSharingImage] = useState(false);

  if (!quoteText) return null;

  const currentTheme = cardThemes.find((t) => t.id === selectedThemeId) || cardThemes[0];
  const authorText = book.authors?.map((a) => a.name).join(", ") || "";

  const buildQuoteText = () => `"${quoteText}"\n\n— ${book.title} (${authorText})${chapterTitle ? ` • ${chapterTitle}` : ""}`;

  const handleCopyText = () => {
    navigator.clipboard.writeText(buildQuoteText());
    showSuccess("Đã sao chép trích dẫn vào clipboard!");
  };

  const handleSharePost = () => {
    setSharing(true);
    createPost(buildQuoteText())
      .then(() => {
        showSuccess("Đã chia sẻ trích dẫn lên Bảng tin");
        onClose();
      })
      .catch(() => {
        showError("Không chia sẻ được trích dẫn. Vui lòng thử lại.");
      })
      .finally(() => setSharing(false));
  };

  const buildCanvas = () => renderQuoteCardCanvas(currentTheme, quoteText, book.title, authorText || chapterTitle);

  const handleDownload = () => {
    const canvas = buildCanvas();
    const link = document.createElement("a");
    link.download = `bookteria-quote-${Date.now()}.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
  };

  const handleShareAsImage = () => {
    if (!highlightId) return;
    setSharingImage(true);
    canvasToBlob(buildCanvas())
      .then((blob) => {
        const file = new File([blob], `quote-card-${Date.now()}.png`, { type: "image/png" });
        return uploadMedia(file).catch((err) => {
          const wrapped = new Error("upload failed");
          wrapped.step = "upload";
          wrapped.cause = err;
          throw wrapped;
        });
      })
      .then((uploadRes) => {
        const { fileId, url } = uploadRes?.data?.result || {};
        return shareHighlight(highlightId, url ? [url] : undefined)
          .then(() => fileId)
          .catch((err) => {
            const wrapped = new Error("share failed");
            wrapped.step = "share";
            wrapped.cause = err;
            throw wrapped;
          });
      })
      .then((fileId) => {
        // confirm-attach thất bại không nên chặn báo thành công (Post đã tạo
        // xong) — chỉ best-effort, lỗi ở bước này âm thầm bỏ qua.
        if (fileId) return confirmMediaAttach(fileId).catch(() => {});
      })
      .then(() => {
        showSuccess("Đã chia sẻ thiệp trích dẫn lên Bảng tin");
        onClose();
      })
      .catch((failure) => {
        const message = failure?.step === "upload" ? getFileErrorMessage(failure.cause) : getReadingErrorMessage(failure?.cause || failure);
        showError(message);
      })
      .finally(() => setSharingImage(false));
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm" slotProps={{ paper: { sx: { borderRadius: 3, p: 1 } } }}>
      <DialogTitle sx={{ fontWeight: 700, display: "flex", alignItems: "center", justifyContent: "space-between", pb: 1 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <FormatQuoteIcon color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Chia sẻ trích dẫn sách
          </Typography>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ pt: 1, pb: 2 }}>
        <Box sx={{ mb: 2.5, display: "flex", alignItems: "center", gap: 1.2, flexWrap: "wrap" }}>
          <Typography variant="caption" sx={{ fontWeight: 700, mr: 0.5 }}>
            Phong cách:
          </Typography>
          {cardThemes.map((theme) => {
            const isSelected = selectedThemeId === theme.id;
            return (
              <Box
                key={theme.id}
                onClick={() => setSelectedThemeId(theme.id)}
                sx={{
                  px: 1.5,
                  py: 0.6,
                  borderRadius: 2,
                  background: theme.bg,
                  color: theme.text,
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  cursor: "pointer",
                  border: isSelected ? "2px solid #ffffff" : "none",
                  boxShadow: isSelected ? "0 0 0 2px #1976d2" : "0 2px 6px rgba(0,0,0,0.1)",
                  transition: "transform 0.15s",
                  "&:hover": { transform: "scale(1.05)" },
                }}
              >
                {theme.name}
              </Box>
            );
          })}
        </Box>

        <Box sx={{ p: { xs: 3, sm: 4 }, borderRadius: 3, background: currentTheme.bg, color: currentTheme.text, boxShadow: "0 10px 30px rgba(0,0,0,0.15)", position: "relative", overflow: "hidden", minHeight: 200, display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <FormatQuoteIcon sx={{ position: "absolute", top: 10, right: 15, fontSize: 90, opacity: 0.15 }} />

          <Typography variant="h6" sx={{ fontStyle: "italic", fontWeight: 500, lineHeight: 1.6, mb: 3, position: "relative", zIndex: 1, fontFamily: "Merriweather, serif" }}>
            "{quoteText}"
          </Typography>

          <Box sx={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", position: "relative", zIndex: 1, pt: 2, borderTop: "1px solid rgba(255,255,255,0.25)" }}>
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 800 }}>
                {book.title}
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.85, display: "block" }}>
                {authorText}
                {chapterTitle ? ` • ${chapterTitle}` : ""}
              </Typography>
            </Box>

            <Box sx={{ display: "flex", alignItems: "center", gap: 0.6, opacity: 0.85 }}>
              <AutoStoriesIcon sx={{ fontSize: 16 }} />
              <Typography variant="caption" sx={{ fontWeight: 700, letterSpacing: 0.8 }}>
                Bookteria
              </Typography>
            </Box>
          </Box>
        </Box>

        <Box sx={{ display: "flex", gap: 1, mt: 2, flexWrap: "wrap" }}>
          <Button size="small" startIcon={<DownloadIcon fontSize="small" />} onClick={handleDownload} variant="text" sx={{ textTransform: "none", fontWeight: 600 }}>
            Tải ảnh về máy
          </Button>
          {highlightId && (
            <Button
              size="small"
              startIcon={sharingImage ? <CircularProgress size={14} color="inherit" /> : <ImageIcon fontSize="small" />}
              onClick={handleShareAsImage}
              disabled={sharingImage}
              variant="text"
              sx={{ textTransform: "none", fontWeight: 600 }}
            >
              Chia sẻ dưới dạng ảnh
            </Button>
          )}
        </Box>
      </DialogContent>

      <Divider />

      <DialogActions sx={{ px: 3, py: 2, justifyContent: "space-between" }}>
        <Button startIcon={<ContentCopyIcon />} onClick={handleCopyText} variant="outlined" sx={{ fontWeight: 600, textTransform: "none", borderRadius: 2 }}>
          Sao chép chữ
        </Button>

        <Box sx={{ display: "flex", gap: 1 }}>
          <Button onClick={onClose} color="inherit" disabled={sharing || sharingImage} sx={{ textTransform: "none" }}>
            Đóng
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={sharing ? <CircularProgress size={16} color="inherit" /> : <ShareIcon />}
            onClick={handleSharePost}
            disabled={sharing || sharingImage}
            sx={{ fontWeight: 700, px: 2.5, textTransform: "none", borderRadius: 2 }}
          >
            Chia sẻ dạng chữ
          </Button>
        </Box>
      </DialogActions>
    </Dialog>
  );
};

export default ShareQuoteDialog;
