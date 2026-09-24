import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import Login from "../pages/Login";
import OAuth2Redirect from "../pages/OAuth2Redirect";
import Home from "../pages/Home";
import Profile from "../pages/Profile";
import Chat from "../pages/Chat";
import Books from "../pages/Books";
import BookDetail from "../pages/BookDetail";
import ReadingList from "../pages/ReadingList";
import SearchResults from "../pages/SearchResults";
import AdminBooks from "../pages/AdminBooks";
import AdminReports from "../pages/AdminReports";
import AdminGroups from "../pages/AdminGroups";
import Reader from "../pages/Reader";
import BookReaderPage from "../pages/BookReaderPage";
import Friends from "../pages/Friends";
import PublicProfile from "../pages/PublicProfile";
import Groups from "../pages/Groups";
import GroupDetail from "../pages/GroupDetail";
import BookProviders from "../components/book/BookProviders";
import FriendProviders from "../components/friend/FriendProviders";
import GroupProviders from "../components/group/GroupProviders";
import ProtectedRoute from "../components/auth/ProtectedRoute";

// Phân loại route — theo đúng yêu cầu auth (không tự đổi URL nào đang có):
// PUBLIC:    /login, /oauth2/redirect, /books, /books/:bookId, /search
// PROTECTED: /, /profile, /chat, /friends, /users/:userId, /admin/reports,
//            /me/reading-list, /admin/books, /reader/:bookId,
//            /read/books/:bookId, /groups, /groups/:groupId
// (Lưu ý: book-service/search-service hiện yêu cầu JWT cho MỌI endpoint kể
// cả GET — /books và /search "public" ở tầng route FE nghĩa là không bị
// ProtectedRoute chặn điều hướng, không có nghĩa dữ liệu tải được khi chưa
// đăng nhập; xem báo cáo cuối cùng, mục "vấn đề còn tồn tại".)
const AppRoutes = () => {
  return (
    <Router>
      <FriendProviders>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Home />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/chat" element={<Chat />} />
            <Route path="/friends" element={<Friends />} />
            <Route path="/users/:userId" element={<PublicProfile />} />
            <Route path="/admin/reports" element={<AdminReports />} />
            <Route path="/admin/groups" element={<AdminGroups />} />
          </Route>

          <Route element={<BookProviders />}>
            <Route path="/books" element={<Books />} />
            <Route path="/books/:bookId" element={<BookDetail />} />
            <Route path="/search" element={<SearchResults />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/me/reading-list" element={<ReadingList />} />
              <Route path="/admin/books" element={<AdminBooks />} />
              <Route path="/reader/:bookId" element={<Reader />} />
              <Route path="/read/books/:bookId" element={<BookReaderPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route element={<GroupProviders />}>
              <Route path="/groups" element={<Groups />} />
              <Route path="/groups/:groupId" element={<GroupDetail />} />
            </Route>
          </Route>
        </Routes>
      </FriendProviders>
    </Router>
  );
};

export default AppRoutes;
