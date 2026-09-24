import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Drawer from "@mui/material/Drawer";
import Toolbar from "@mui/material/Toolbar";
import { useTheme } from "@mui/material";
import Header from "../components/Header";
import SideMenu from "../components/SideMenu";
import HorizontalNav from "../components/HorizontalNav";

const drawerWidth = 300;
const SIDEBAR_OPEN_KEY = "sidebar_open";

function Scene({ children }) {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [isClosing, setIsClosing] = React.useState(false);
  const [sidebarOpen, setSidebarOpen] = React.useState(() => {
    try {
      const saved = localStorage.getItem(SIDEBAR_OPEN_KEY);
      return saved !== null ? saved === "true" : true;
    } catch {
      return true;
    }
  });

  const theme = useTheme();

  const handleDrawerClose = () => {
    setIsClosing(true);
    setMobileOpen(false);
  };

  const handleDrawerTransitionEnd = () => {
    setIsClosing(false);
  };

  const handleDrawerToggle = () => {
    if (!isClosing) {
      setMobileOpen(!mobileOpen);
    }
  };

  const handleSidebarToggle = () => {
    setSidebarOpen((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(SIDEBAR_OPEN_KEY, String(next));
      } catch {
        // Ignore storage errors if disabled
      }
      return next;
    });
  };

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
      }}
    >
      <AppBar
        position="fixed"
        sx={{
          ml: { sm: sidebarOpen ? `${drawerWidth}px` : 0 },
          zIndex: theme.zIndex.drawer + 1,
          transition: "margin-left 250ms cubic-bezier(0.4, 0, 0.2, 1)",
        }}
      >
        <Toolbar>
          <Header onToggleMobileDrawer={handleDrawerToggle} onToggleSidebar={handleSidebarToggle} sidebarOpen={sidebarOpen} />
        </Toolbar>
      </AppBar>
      <Box
        sx={{
          display: "flex",
          flexDirection: "row",
        }}
      >
        <Box
          component="nav"
          sx={{ width: { sm: sidebarOpen ? drawerWidth : 0 }, flexShrink: { sm: 0 }, transition: "width 250ms cubic-bezier(0.4, 0, 0.2, 1)" }}
          aria-label="mailbox folders"
        >
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onTransitionEnd={handleDrawerTransitionEnd}
            onClose={handleDrawerClose}
            ModalProps={{
              keepMounted: true, // Better open performance on mobile.
            }}
            sx={{
              display: { xs: "block", sm: "none" },
              "& .MuiDrawer-paper": {
                boxSizing: "border-box",
                width: drawerWidth,
              },
            }}
          >
            <SideMenu />
          </Drawer>
          <Drawer
            variant="permanent"
            sx={{
              display: { xs: "none", sm: "block" },
              "& .MuiDrawer-paper": {
                boxSizing: "border-box",
                width: drawerWidth,
                transform: sidebarOpen ? "none" : `translateX(-${drawerWidth}px)`,
                visibility: sidebarOpen ? "visible" : "hidden",
                transition: "transform 250ms cubic-bezier(0.4, 0, 0.2, 1), visibility 250ms cubic-bezier(0.4, 0, 0.2, 1)",
                overflowX: "hidden",
              },
            }}
            open
          >
            <SideMenu />
          </Drawer>
        </Box>
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            width: { sm: sidebarOpen ? `calc(100% - ${drawerWidth}px)` : "100%" },
            transition: "width 250ms cubic-bezier(0.4, 0, 0.2, 1)",
          }}
        >
          <Toolbar />
          <HorizontalNav visible={!sidebarOpen} />
          <Box
            sx={{
              display: "flex",
              justifyContent: "center",
              width: "100%",
              height: "100%",
            }}
          >
            {children}
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

export default Scene;
