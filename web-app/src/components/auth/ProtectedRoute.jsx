import React from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isAuthenticated } from "../../services/authenticationService";

// Layout route bảo vệ 1 nhóm route con — theo đúng pattern BookProviders/
// FriendProviders/GroupProviders đã có (element={<ProtectedRoute/>} bọc
// quanh các <Route> con, dùng <Outlet/>).
//
// isAuthenticated() đọc thẳng localStorage (đồng bộ, không có round-trip
// mạng nào để "xác định" trạng thái đăng nhập trong app này) nên không có
// bước loading riêng: ngay lượt render đầu tiên đã biết chắc render trang
// riêng tư hay redirect — không có tình trạng render trang riêng tư trước
// rồi mới redirect sau.
export default function ProtectedRoute() {
  const location = useLocation();

  if (!isAuthenticated()) {
    const returnUrl = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?returnUrl=${returnUrl}&reason=required`} replace />;
  }

  return <Outlet />;
}
