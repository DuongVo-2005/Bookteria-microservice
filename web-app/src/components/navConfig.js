import HomeIcon from "@mui/icons-material/Home";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import BookmarksIcon from "@mui/icons-material/Bookmarks";
import PeopleAltOutlinedIcon from "@mui/icons-material/PeopleAltOutlined";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import ChatBubbleOutlineOutlinedIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import FlagIcon from "@mui/icons-material/Flag";
import SupervisorAccountIcon from "@mui/icons-material/SupervisorAccount";
import { isAdmin, hasPermission } from "../services/authenticationService";

// BA v2 Phase 1 P1-02 (be-report.md, bổ sung 2026-09-19) — LIBRARIAN giờ cũng
// quản lý được catalog sách, phải thấy được link "Quản trị sách" dù không
// phải ADMIN. "Báo cáo vi phạm"/"Quản lý nhóm" vẫn ADMIN-only đúng theo API
// contract (`hasRole('ADMIN')`, không có nhánh permission nào khác).
const CATALOG_PERMISSIONS = [
  "book:create",
  "book:update",
  "book:delete",
  "book:import",
  "reading:import_content",
  "review:lock",
  "author:manage",
  "category:manage",
  "publisher:manage",
];
const canAccessCatalogAdmin = () => isAdmin() || CATALOG_PERMISSIONS.some((p) => hasPermission(p));

// Nguồn dùng chung cho SideMenu (dọc) + HorizontalNav (ngang, hiện khi thu
// gọn sidebar) — đổi/thêm 1 mục điều hướng chỉ cần sửa đúng 1 chỗ ở đây.
// `visible` (nếu có) được gọi lại mỗi lần render để luôn phản ánh đúng
// quyền hiện tại của user đăng nhập.
export const NAV_ITEMS = [
  { key: "home", label: "Home", path: "/", Icon: HomeIcon },
  { key: "books", label: "Books", path: "/books", Icon: MenuBookIcon },
  { key: "my-books", label: "My Books", path: "/me/reading-list", Icon: BookmarksIcon },
  { key: "friends", label: "Friends", path: "/friends", Icon: PeopleAltOutlinedIcon, badgeType: "friend" },
  { key: "groups", label: "Groups", path: "/groups", Icon: GroupsOutlinedIcon },
  { key: "chat", label: "Chat", path: "/chat", Icon: ChatBubbleOutlineOutlinedIcon },
  { key: "admin-books", label: "Quản trị sách", path: "/admin/books", Icon: AdminPanelSettingsIcon, visible: canAccessCatalogAdmin },
  { key: "admin-reports", label: "Báo cáo vi phạm", path: "/admin/reports", Icon: FlagIcon, visible: isAdmin },
  { key: "admin-groups", label: "Quản lý nhóm", path: "/admin/groups", Icon: SupervisorAccountIcon, visible: isAdmin },
];

export const isNavItemVisible = (item) => !item.visible || item.visible();

export const isNavPathActive = (currentPath, targetPath) => {
  if (targetPath === "/") return currentPath === "/";
  return currentPath.startsWith(targetPath);
};
