import * as React from "react";
import Divider from "@mui/material/Divider";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import Toolbar from "@mui/material/Toolbar";
import Chip from "@mui/material/Chip";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import { useLocation, useNavigate } from "react-router-dom";
import { useFriend } from "../context/FriendContext";
import { NAV_ITEMS, isNavPathActive, isNavItemVisible } from "./navConfig";

function SideMenu() {
  const location = useLocation();
  const navigate = useNavigate();
  const { incomingRequests } = useFriend();
  const items = NAV_ITEMS.filter(isNavItemVisible);

  const getBadgeCount = (item) => (item.badgeType === "friend" ? incomingRequests.length : 0);

  return (
    <Box sx={{ overflow: "auto" }}>
      <Toolbar />
      <List disablePadding sx={{ py: 1 }}>
        {items.map((item) => {
          const active = isNavPathActive(location.pathname, item.path);
          const badgeCount = getBadgeCount(item);
          const Icon = item.Icon;

          return (
            <ListItem key={item.key} disablePadding sx={{ px: 1.5, py: 0.3 }}>
              <ListItemButton
                onClick={() => navigate(item.path)}
                selected={active}
                sx={{
                  borderRadius: 2,
                  py: 1.2,
                  px: 2,
                  "&.Mui-selected": {
                    bgcolor: "primary.main",
                    color: "#ffffff",
                    "& .MuiListItemIcon-root": { color: "#ffffff" },
                    "&:hover": { bgcolor: "primary.dark" },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: active ? "#ffffff" : "text.secondary" }}>
                  <Icon />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="body2" sx={{ fontWeight: active ? 700 : 500, fontSize: "0.95rem" }}>
                      {item.label}
                    </Typography>
                  }
                />
                {badgeCount > 0 && (
                  <Chip label={badgeCount} size="small" color="error" sx={{ height: 20, minWidth: 20, fontSize: "0.7rem", fontWeight: 700 }} />
                )}
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
      <Divider />
    </Box>
  );
}

export default SideMenu;
