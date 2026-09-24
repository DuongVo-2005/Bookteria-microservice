import React, { useState, useEffect, useRef, useCallback } from "react";
import { Box, Typography, Paper, IconButton, Tooltip, Divider } from "@mui/material";
import ShareIcon from "@mui/icons-material/Share";

const highlightColors = [
  { color: "yellow", hex: "#fef08a", name: "Vàng" },
  { color: "green", hex: "#bbf7d0", name: "Xanh lá" },
  { color: "pink", hex: "#fbcfe8", name: "Hồng" },
  { color: "blue", hex: "#bfdbfe", name: "Xanh dương" },
];

export const ReaderContent = ({ chapter, settings, onAddHighlight, onOpenShareQuote, onScrollProgress, contentBg, textColor }) => {
  const contentRef = useRef(null);
  const [selectedText, setSelectedText] = useState("");
  const [toolbarPos, setToolbarPos] = useState(null);

  const handleMouseUp = () => {
    const selection = window.getSelection();
    if (!selection || selection.isCollapsed) {
      setToolbarPos(null);
      setSelectedText("");
      return;
    }

    const text = selection.toString().trim();
    if (text.length > 2) {
      const range = selection.getRangeAt(0);
      const rect = range.getBoundingClientRect();
      setSelectedText(text);

      setToolbarPos({
        top: Math.max(70, rect.top - 50 + window.scrollY),
        left: Math.max(20, rect.left + rect.width / 2),
      });
    } else {
      setToolbarPos(null);
      setSelectedText("");
    }
  };

  const handleDocumentClick = useCallback((e) => {
    const target = e.target;
    if (!target.closest(".selection-toolbar") && !window.getSelection()?.toString()) {
      setToolbarPos(null);
    }
  }, []);

  useEffect(() => {
    document.addEventListener("mousedown", handleDocumentClick);
    return () => document.removeEventListener("mousedown", handleDocumentClick);
  }, [handleDocumentClick]);

  useEffect(() => {
    const handleScroll = () => {
      const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
      if (totalHeight > 0) {
        const currentProgress = (window.scrollY / totalHeight) * 100;
        onScrollProgress?.(currentProgress);
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, [onScrollProgress]);

  const handleApplyHighlight = (color) => {
    if (selectedText) {
      onAddHighlight(selectedText, color);
      window.getSelection()?.removeAllRanges();
      setToolbarPos(null);
      setSelectedText("");
    }
  };

  const handleShare = () => {
    if (selectedText) {
      onOpenShareQuote(selectedText);
      window.getSelection()?.removeAllRanges();
      setToolbarPos(null);
      setSelectedText("");
    }
  };

  const getMaxWidth = () => {
    switch (settings.contentWidth) {
      case "narrow":
        return 680;
      case "medium":
        return 820;
      case "wide":
        return 1000;
      default:
        return 820;
    }
  };

  return (
    <Box sx={{ width: "100%", minHeight: "calc(100vh - 120px)", bgcolor: contentBg, color: textColor, py: { xs: 3, sm: 6 }, px: { xs: 2, sm: 4 }, transition: "background-color 0.3s, color 0.3s" }} onMouseUp={handleMouseUp}>
      {toolbarPos && selectedText && (
        <Paper
          className="selection-toolbar"
          elevation={4}
          sx={{ position: "absolute", top: toolbarPos.top, left: toolbarPos.left, transform: "translateX(-50%)", zIndex: 1200, display: "flex", alignItems: "center", gap: 0.8, p: 0.8, borderRadius: 3, bgcolor: "#1f2937", color: "#ffffff", boxShadow: "0 8px 24px rgba(0,0,0,0.25)" }}
        >
          {highlightColors.map((hc) => (
            <Tooltip key={hc.color} title={`Tô màu ${hc.name}`}>
              <Box
                onClick={() => handleApplyHighlight(hc.color)}
                sx={{ width: 22, height: 22, borderRadius: "50%", bgcolor: hc.hex, cursor: "pointer", border: "2px solid rgba(255,255,255,0.8)", transition: "transform 0.15s", "&:hover": { transform: "scale(1.2)" } }}
              />
            </Tooltip>
          ))}

          <Divider orientation="vertical" flexItem sx={{ borderColor: "rgba(255,255,255,0.2)", mx: 0.5 }} />

          <Tooltip title="Chia sẻ trích dẫn này">
            <IconButton size="small" onClick={handleShare} sx={{ color: "#ffffff", p: 0.5 }}>
              <ShareIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Paper>
      )}

      <Box
        ref={contentRef}
        sx={{
          maxWidth: getMaxWidth(),
          mx: "auto",
          fontFamily: `${settings.fontFamily}, serif`,
          fontSize: `${settings.fontSize}px`,
          lineHeight: settings.lineHeight,
          textAlign: settings.textAlign,
          "& h1, & h2, & h3, & h4": { fontFamily: `${settings.fontFamily}, serif`, fontWeight: 700, my: 2.5, color: "inherit" },
          "& p": { mb: 2.5, color: "inherit" },
          "& blockquote": { borderLeft: "4px solid #1976d2", my: 3, pl: 2.5, py: 1, fontStyle: "italic", opacity: 0.9, bgcolor: "rgba(25, 118, 210, 0.05)", borderRadius: "0 8px 8px 0" },
          "& pre, & code": { fontFamily: "Fira Code, monospace", bgcolor: "rgba(0,0,0,0.06)", p: 1.5, borderRadius: 2, overflowX: "auto", display: "block", fontSize: "0.9em", my: 2 },
        }}
      >
        <Box sx={{ mb: 4, pb: 2, borderBottom: "1px solid rgba(0,0,0,0.1)" }}>
          <Typography variant="h4" component="h1" sx={{ fontWeight: 800, fontFamily: `${settings.fontFamily}, serif`, lineHeight: 1.3, mb: 1 }}>
            {chapter.title}
          </Typography>
          {chapter.subtitle && (
            <Typography variant="subtitle1" sx={{ opacity: 0.8, fontFamily: `${settings.fontFamily}, serif`, fontStyle: "italic" }}>
              {chapter.subtitle}
            </Typography>
          )}
        </Box>

        {chapter.content.split("\n\n").map((paragraph, idx) => {
          if (paragraph.startsWith("### ")) {
            return (
              <Typography key={idx} variant="h6" component="h3" sx={{ fontWeight: 700, fontFamily: `${settings.fontFamily}, serif`, mt: 3.5, mb: 1.5 }}>
                {paragraph.replace("### ", "")}
              </Typography>
            );
          }

          if (paragraph.startsWith("> ")) {
            return (
              <Box key={idx} component="blockquote" sx={{ borderLeft: "4px solid #1976d2", pl: 2.5, py: 1, my: 2.5, fontStyle: "italic", opacity: 0.9, bgcolor: "rgba(25, 118, 210, 0.06)", borderRadius: "0 8px 8px 0" }}>
                <Typography sx={{ fontStyle: "italic", fontFamily: "inherit", fontSize: "inherit" }}>{paragraph.replace("> ", "")}</Typography>
              </Box>
            );
          }

          if (paragraph.startsWith("```")) {
            const cleanCode = paragraph.replace(/```[a-z]*\n?/g, "");
            return (
              <Box key={idx} component="pre" sx={{ bgcolor: "rgba(0,0,0,0.06)", p: 2, borderRadius: 2, overflowX: "auto", fontFamily: "Fira Code, monospace", fontSize: "0.88em", my: 2.5 }}>
                <code>{cleanCode}</code>
              </Box>
            );
          }

          return (
            <Typography key={idx} component="p" sx={{ fontFamily: "inherit", fontSize: "inherit", lineHeight: "inherit", mb: 2.5 }}>
              {paragraph}
            </Typography>
          );
        })}
      </Box>
    </Box>
  );
};

export default ReaderContent;
