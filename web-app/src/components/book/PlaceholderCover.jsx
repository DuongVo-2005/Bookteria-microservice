import React from "react";
import { Box, Typography } from "@mui/material";
import MenuBookIcon from "@mui/icons-material/MenuBook";

export const PlaceholderCover = ({ title = "", height = "100%", width = "100%" }) => {
  return (
    <Box
      sx={{
        width,
        height,
        backgroundColor: "#e0e0e0",
        backgroundImage: "linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: 2,
        textAlign: "center",
        color: "#1565c0",
        borderRadius: 1,
        border: "1px solid rgba(25, 118, 210, 0.12)",
        boxSizing: "border-box",
        overflow: "hidden",
      }}
    >
      <MenuBookIcon sx={{ fontSize: 42, mb: 1, opacity: 0.8 }} />
      {title && (
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            fontSize: "0.75rem",
            lineHeight: 1.2,
            maxHeight: "3.6em",
            overflow: "hidden",
            textOverflow: "ellipsis",
            display: "-webkit-box",
            WebkitLineClamp: 3,
            WebkitBoxOrient: "vertical",
            color: "#0d47a1",
          }}
        >
          {title}
        </Typography>
      )}
    </Box>
  );
};

export default PlaceholderCover;
