import React from "react";
import { Outlet } from "react-router-dom";
import { ToastProvider } from "../../context/ToastContext";
import { GroupProvider } from "../../context/GroupContext";

// Layout route riêng cho khu vực Groups (/groups, /groups/:groupId), theo
// đúng pattern BookProviders — không cần global vì chỉ trang trong khu vực
// này dùng useGroup().
export default function GroupProviders() {
  return (
    <ToastProvider>
      <GroupProvider>
        <Outlet />
      </GroupProvider>
    </ToastProvider>
  );
}
