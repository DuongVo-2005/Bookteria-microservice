import React from "react";
import { Box, ButtonBase, Typography, Chip, Collapse } from "@mui/material";
import { useLocation, useNavigate } from "react-router-dom";
import { NAV_ITEMS, isNavPathActive, isNavItemVisible } from "./navConfig";
import { useFriend } from "../context/FriendContext";

// Thanh điều hướng ngang, dạng pill — hiện thay cho sidebar dọc khi sidebar
// bị thu gọn (Scene.jsx điều khiển qua prop `visible`).
export default function HorizontalNav({ visible }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { incomingRequests } = useFriend();
  const items = NAV_ITEMS.filter(isNavItemVisible);

  const getBadgeCount = (item) => (item.badgeType === "friend" ? incomingRequests.length : 0);

  return (
    <Collapse in={visible} timeout={250} unmountOnExit={false}>
      <Box
        component="nav"
        aria-label="horizontal navigation"
        sx={{
          bgcolor: "background.paper",
          borderBottom: "1px solid rgba(0, 0, 0, 0.08)",
          boxShadow: "0 2px 6px rgba(0, 0, 0, 0.04)",
          position: "sticky",
          top: 64,
          zIndex: (t) => t.zIndex.appBar - 1,
          px: { xs: 1.5, sm: 3, md: 4 },
          py: 0.75,
          display: "flex",
          alignItems: "center",
          minHeight: 48,
          width: "100%",
          boxSizing: "border-box",
          transition: "all 250ms cubic-bezier(0.4, 0, 0.2, 1)",
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: { xs: 1, sm: 1.5 },
            width: "100%",
            overflowX: "auto",
            flexWrap: "nowrap",
            py: 0.5,
            scrollbarWidth: "none",
            "&::-webkit-scrollbar": { display: "none" },
            WebkitOverflowScrolling: "touch",
          }}
        >
          {items.map((item) => {
            const active = isNavPathActive(location.pathname, item.path);
            const badgeCount = getBadgeCount(item);
            const Icon = item.Icon;

            return (
              <ButtonBase
                key={item.key}
                onClick={() => navigate(item.path)}
                sx={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 1,
                  px: { xs: 1.5, sm: 2 },
                  py: 0.85,
                  borderRadius: "24px",
                  whiteSpace: "nowrap",
                  flexShrink: 0,
                  transition: "all 200ms cubic-bezier(0.4, 0, 0.2, 1)",
                  bgcolor: active ? "primary.main" : "transparent",
                  color: active ? "#ffffff" : "text.primary",
                  boxShadow: active ? "0 2px 6px rgba(25, 118, 210, 0.3)" : "none",
                  "&:hover": {
                    bgcolor: active ? "primary.dark" : "rgba(25, 118, 210, 0.08)",
                    color: active ? "#ffffff" : "primary.main",
                  },
                  "&:focus-visible": {
                    outline: "2px solid #1976d2",
                    outlineOffset: "2px",
                  },
                }}
              >
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: active ? "#ffffff" : "primary.main",
                    "& .MuiSvgIcon-root": { fontSize: "1.25rem" },
                  }}
                >
                  <Icon />
                </Box>

                <Typography variant="body2" sx={{ fontWeight: active ? 700 : 500, fontSize: { xs: "0.85rem", sm: "0.9rem" }, letterSpacing: 0.2 }}>
                  {item.label}
                </Typography>

                {badgeCount > 0 && (
                  <Chip
                    label={badgeCount}
                    size="small"
                    color="error"
                    sx={{ height: 18, minWidth: 18, fontSize: "0.65rem", fontWeight: 700, px: 0.5, "& .MuiChip-label": { px: 0.4 } }}
                  />
                )}
              </ButtonBase>
            );
          })}
        </Box>
      </Box>
    </Collapse>
  );
}
