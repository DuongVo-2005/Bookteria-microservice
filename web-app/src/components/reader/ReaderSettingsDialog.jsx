import React from "react";
import { Dialog, DialogTitle, DialogContent, Box, Typography, IconButton, ButtonGroup, Button, Slider, ToggleButtonGroup, ToggleButton, Divider } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import FormatSizeIcon from "@mui/icons-material/FormatSize";
import FormatAlignLeftIcon from "@mui/icons-material/FormatAlignLeft";
import FormatAlignJustifyIcon from "@mui/icons-material/FormatAlignJustify";
import ViewStreamIcon from "@mui/icons-material/ViewStream";
import ViewHeadlineIcon from "@mui/icons-material/ViewHeadline";
import ViewDayIcon from "@mui/icons-material/ViewDay";

const themeOptions = [
  { mode: "light", name: "Sáng", bg: "#ffffff", text: "#212529", border: "#e0e0e0" },
  { mode: "sepia", name: "Sepia", bg: "#fbf0d9", text: "#5f4b32", border: "#e6d8be" },
  { mode: "dark", name: "Tối", bg: "#1e2124", text: "#e0e0e0", border: "#3a3f44" },
  { mode: "oled", name: "Đêm OLED", bg: "#000000", text: "#d1d5db", border: "#222222" },
  { mode: "nord", name: "Nord Frost", bg: "#2e3440", text: "#eceff4", border: "#434c5e" },
];

const fontFamilies = [
  { font: "Merriweather", label: "Merriweather (Serif)", sample: "Chữ có chân chuẩn mực cho việc đọc" },
  { font: "Inter", label: "Inter (Hiện đại)", sample: "Gọn gàng, tối ưu cho màn hình số" },
  { font: "Roboto", label: "Roboto (Cổ điển)", sample: "Rõ ràng và dễ đọc" },
  { font: "Georgia", label: "Georgia (Tao nhã)", sample: "Đậm nét văn chương cổ điển" },
  { font: "Fira Code", label: "Fira Code (Code)", sample: "Phù hợp sách kỹ thuật & lập trình" },
];

export const ReaderSettingsDialog = ({ open, onClose, settings, onUpdateSettings }) => {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs" slotProps={{ paper: { sx: { borderRadius: 3, p: 1, boxShadow: "0 8px 32px rgba(0,0,0,0.18)" } } }}>
      <DialogTitle sx={{ fontWeight: 700, display: "flex", alignItems: "center", justifyContent: "space-between", pb: 1 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <FormatSizeIcon color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Tuỳ chỉnh giao diện đọc
          </Typography>
        </Box>
        <IconButton onClick={onClose} size="small" aria-label="Đóng">
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ pt: 1.5, pb: 2.5 }}>
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.2 }}>
            Chủ đề & Màu sắc
          </Typography>
          <Box sx={{ display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 1 }}>
            {themeOptions.map((t) => {
              const selected = settings.theme === t.mode;
              return (
                <Box
                  key={t.mode}
                  onClick={() => onUpdateSettings({ theme: t.mode })}
                  sx={{
                    p: 1,
                    borderRadius: 2,
                    bgcolor: t.bg,
                    color: t.text,
                    border: selected ? "2px solid #1976d2" : `1px solid ${t.border}`,
                    cursor: "pointer",
                    textAlign: "center",
                    boxShadow: selected ? "0 0 0 2px rgba(25, 118, 210, 0.2)" : "none",
                    transition: "transform 0.15s",
                    "&:hover": { transform: "scale(1.04)" },
                  }}
                >
                  <Typography variant="caption" sx={{ fontWeight: 700, fontSize: "0.7rem" }}>
                    {t.name}
                  </Typography>
                </Box>
              );
            })}
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
              Cỡ chữ
            </Typography>
            <Typography variant="body2" color="primary" sx={{ fontWeight: 700 }}>
              {settings.fontSize}px
            </Typography>
          </Box>

          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <ButtonGroup size="small" variant="outlined" sx={{ borderRadius: 2 }}>
              <Button onClick={() => onUpdateSettings({ fontSize: Math.max(14, settings.fontSize - 1) })} disabled={settings.fontSize <= 14} sx={{ fontWeight: 700 }}>
                A-
              </Button>
              <Button onClick={() => onUpdateSettings({ fontSize: Math.min(28, settings.fontSize + 1) })} disabled={settings.fontSize >= 28} sx={{ fontWeight: 700 }}>
                A+
              </Button>
            </ButtonGroup>

            <Slider value={settings.fontSize} min={14} max={28} step={1} onChange={(_, val) => onUpdateSettings({ fontSize: val })} sx={{ flex: 1 }} />
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
            Phông chữ
          </Typography>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            {fontFamilies.map((f) => {
              const selected = settings.fontFamily === f.font;
              return (
                <Box
                  key={f.font}
                  onClick={() => onUpdateSettings({ fontFamily: f.font })}
                  sx={{ p: 1.2, borderRadius: 2, border: selected ? "2px solid #1976d2" : "1px solid rgba(0,0,0,0.1)", bgcolor: selected ? "primary.50" : "transparent", cursor: "pointer", fontFamily: f.font, transition: "all 0.15s" }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, fontFamily: f.font }}>
                    {f.label}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ fontFamily: f.font }}>
                    {f.sample}
                  </Typography>
                </Box>
              );
            })}
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
          <Box>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
              Giãn dòng
            </Typography>
            <ToggleButtonGroup size="small" exclusive value={settings.lineHeight} onChange={(_, val) => val && onUpdateSettings({ lineHeight: val })} fullWidth>
              <ToggleButton value={1.5}>1.5</ToggleButton>
              <ToggleButton value={1.8}>1.8</ToggleButton>
              <ToggleButton value={2.2}>2.2</ToggleButton>
            </ToggleButtonGroup>
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
              Độ rộng lề
            </Typography>
            <ToggleButtonGroup size="small" exclusive value={settings.contentWidth} onChange={(_, val) => val && onUpdateSettings({ contentWidth: val })} fullWidth>
              <ToggleButton value="narrow">
                <ViewStreamIcon fontSize="small" />
              </ToggleButton>
              <ToggleButton value="medium">
                <ViewHeadlineIcon fontSize="small" />
              </ToggleButton>
              <ToggleButton value="wide">
                <ViewDayIcon fontSize="small" />
              </ToggleButton>
            </ToggleButtonGroup>
          </Box>
        </Box>

        <Box sx={{ mt: 2.5 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
            Căn lề đoạn văn
          </Typography>
          <ToggleButtonGroup size="small" exclusive value={settings.textAlign} onChange={(_, val) => val && onUpdateSettings({ textAlign: val })} fullWidth>
            <ToggleButton value="left" sx={{ gap: 1 }}>
              <FormatAlignLeftIcon fontSize="small" /> Canh trái
            </ToggleButton>
            <ToggleButton value="justify" sx={{ gap: 1 }}>
              <FormatAlignJustifyIcon fontSize="small" /> Canh đều hai bên
            </ToggleButton>
          </ToggleButtonGroup>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default ReaderSettingsDialog;
