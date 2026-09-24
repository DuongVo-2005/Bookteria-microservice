import React, { useState, useEffect } from "react";
import { Container, Box, Paper, Avatar, Typography, Button, Divider, Breadcrumbs, Link, Card, CardContent, CircularProgress } from "@mui/material";
import { useParams, useNavigate } from "react-router-dom";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import PersonIcon from "@mui/icons-material/Person";
import ChatBubbleOutlineOutlinedIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import LocationOnOutlinedIcon from "@mui/icons-material/LocationOnOutlined";
import CakeOutlinedIcon from "@mui/icons-material/CakeOutlined";
import BadgeOutlinedIcon from "@mui/icons-material/BadgeOutlined";
import FlagOutlinedIcon from "@mui/icons-material/FlagOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import KeyOutlinedIcon from "@mui/icons-material/KeyOutlined";
import PersonOffOutlinedIcon from "@mui/icons-material/PersonOffOutlined";
import Scene from "./Scene";
import { isAuthenticated, getCurrentUserId, isAdmin } from "../services/authenticationService";
import { getUserProfile } from "../services/userService";
import { unlockUser } from "../services/adminUserService";
import { getIdentityErrorMessage } from "../services/identityErrorMessages";
import { FriendStatusButton } from "../components/friend/FriendStatusButton";
import { useFriend } from "../context/FriendContext";
import { useToast } from "../context/ToastContext";
import { EmptyState } from "../components/book/EmptyState";
import { ReportDialog } from "../components/report/ReportDialog";
import { LockUserDialog } from "../components/admin/LockUserDialog";
import { ManagePermissionsDialog } from "../components/admin/ManagePermissionsDialog";
import { ResetPasswordDialog } from "../components/admin/ResetPasswordDialog";
import { DeactivateAccountDialog } from "../components/admin/DeactivateAccountDialog";
import { UserShelfSection } from "../components/book/UserShelfSection";

// Trang hồ sơ công khai của người dùng khác — theo đúng spec B.1
// (fe-roadmap-gap-analysis-and-new-ui-spec.md): backend sẵn sàng thật
// (GET /profile/users/{userId}), chỉ hiện đúng field profile-service thật có
// (username/avatar/email/firstName/lastName/dob/city) — không có
// bio/mutualFriendsCount/currentReadingBook như bản UI mẫu bên thứ 3.
export default function PublicProfile() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const { getFriendshipStatus } = useFriend();
  const { showSuccess, showError } = useToast();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
    }
  }, [navigate]);

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [reportUserOpen, setReportUserOpen] = useState(false);
  const [lockDialogOpen, setLockDialogOpen] = useState(false);
  const [unlocking, setUnlocking] = useState(false);
  const [permissionsDialogOpen, setPermissionsDialogOpen] = useState(false);
  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);
  const [deactivateDialogOpen, setDeactivateDialogOpen] = useState(false);

  const handleUnlock = () => {
    setUnlocking(true);
    unlockUser(userId)
      .then(() => showSuccess("Đã mở khoá tài khoản."))
      .catch((err) => showError(getIdentityErrorMessage(err)))
      .finally(() => setUnlocking(false));
  };

  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    setNotFound(false);
    getUserProfile(userId)
      .then((response) => {
        setProfile(response?.data?.result || response?.data || null);
      })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [userId]);

  const myId = getCurrentUserId();
  const isSelf = userId === myId;
  const relationshipStatus = !isSelf ? getFriendshipStatus(userId) : "NONE";
  const isBlockedOrBlocking = relationshipStatus === "BLOCKED" || relationshipStatus === "BLOCKED_BY";

  if (loading) {
    return (
      <Scene>
        <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
          <CircularProgress />
        </Box>
      </Scene>
    );
  }

  if (notFound || !profile) {
    return (
      <Scene>
        <Container maxWidth="md" sx={{ py: 4 }}>
          <EmptyState title="Không tìm thấy người dùng này" description="Người dùng không tồn tại hoặc đã bị xoá." actionText="Quay lại" onAction={() => navigate(-1)} />
        </Container>
      </Scene>
    );
  }

  const fullName = `${profile.lastName || ""} ${profile.firstName || ""}`.trim() || profile.username;

  return (
    <Scene>
      <Container maxWidth="md" sx={{ py: 3 }}>
        <Box sx={{ mb: 2.5, display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 1 }}>
          <Breadcrumbs aria-label="breadcrumb">
            <Link underline="hover" color="inherit" sx={{ cursor: "pointer", display: "flex", alignItems: "center" }} onClick={() => navigate("/")}>
              Trang chủ
            </Link>
            <Link underline="hover" color="inherit" sx={{ cursor: "pointer" }} onClick={() => navigate("/friends")}>
              Bạn bè
            </Link>
            <Typography color="text.primary" sx={{ fontWeight: 600 }}>
              @{profile.username}
            </Typography>
          </Breadcrumbs>

          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)} size="small" variant="outlined" sx={{ borderRadius: 2, textTransform: "none" }}>
            Quay lại
          </Button>
        </Box>

        <Paper elevation={0} sx={{ borderRadius: 4, overflow: "hidden", border: "1px solid rgba(0, 0, 0, 0.08)", boxShadow: "0 4px 20px rgba(0, 0, 0, 0.05)", mb: 3 }}>
          <Box sx={{ height: 140, background: "linear-gradient(135deg, #1976d2 0%, #42a5f5 50%, #7e57c2 100%)" }} />

          <Box sx={{ px: { xs: 2.5, sm: 4 }, pb: 3.5, pt: 0, position: "relative" }}>
            <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: { xs: "center", sm: "flex-end" }, justifyContent: "space-between", gap: 2, mt: -6, mb: 2 }}>
              <Box sx={{ display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: { xs: "center", sm: "flex-end" }, gap: 2.5 }}>
                <Avatar
                  src={profile.avatar || undefined}
                  alt={fullName}
                  sx={{ width: { xs: 96, sm: 110 }, height: { xs: 96, sm: 110 }, border: "4px solid #ffffff", boxShadow: "0 4px 14px rgba(0,0,0,0.15)", bgcolor: "primary.main" }}
                >
                  <PersonIcon sx={{ fontSize: 56 }} />
                </Avatar>

                <Box sx={{ textAlign: { xs: "center", sm: "left" } }}>
                  <Typography variant="h5" sx={{ fontWeight: 800, color: "text.primary", lineHeight: 1.2 }}>
                    {fullName}
                  </Typography>
                  <Typography variant="body1" color="text.secondary" sx={{ fontWeight: 500, mt: 0.3 }}>
                    @{profile.username}
                  </Typography>
                </Box>
              </Box>

              {!isSelf && (
                <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, flexWrap: "wrap", justifyContent: "center" }}>
                  <FriendStatusButton otherUserId={profile.userId} otherUsername={profile.username} otherAvatar={profile.avatar} size="medium" />

                  {!isBlockedOrBlocking && (
                    <Button
                      variant="contained"
                      color="secondary"
                      size="medium"
                      startIcon={<ChatBubbleOutlineOutlinedIcon />}
                      onClick={() => navigate("/chat", { state: { startChatWithUserId: profile.userId } })}
                      sx={{ fontWeight: 700, borderRadius: 2, textTransform: "none", px: 2.5, boxShadow: "0 3px 10px rgba(156, 39, 176, 0.25)" }}
                    >
                      Nhắn tin
                    </Button>
                  )}
                  <Button
                    variant="outlined"
                    color="error"
                    size="medium"
                    startIcon={<FlagOutlinedIcon />}
                    onClick={() => setReportUserOpen(true)}
                    sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                  >
                    Báo cáo
                  </Button>

                  {/* BA Backlog OPS-03 (be-report.md, bổ sung 2026-09-19) —
                      không có API nào cho FE biết user này đang bị khoá hay
                      không, nên luôn hiện cả 2 nút, Admin tự biết cần thao
                      tác nào dựa trên thông tin đã có từ nơi khác (VD hàng
                      chờ báo cáo). */}
                  {isAdmin() && (
                    <>
                      <Button
                        variant="outlined"
                        color="warning"
                        size="medium"
                        startIcon={<LockOutlinedIcon />}
                        onClick={() => setLockDialogOpen(true)}
                        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                      >
                        Khoá tài khoản
                      </Button>
                      <Button
                        variant="outlined"
                        color="success"
                        size="medium"
                        startIcon={<LockOpenOutlinedIcon />}
                        onClick={handleUnlock}
                        disabled={unlocking}
                        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                      >
                        Mở khoá
                      </Button>
                      <Button
                        variant="outlined"
                        color="secondary"
                        size="medium"
                        startIcon={<AdminPanelSettingsOutlinedIcon />}
                        onClick={() => setPermissionsDialogOpen(true)}
                        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                      >
                        Quản lý quyền
                      </Button>
                      <Button
                        variant="outlined"
                        color="info"
                        size="medium"
                        startIcon={<KeyOutlinedIcon />}
                        onClick={() => setResetPasswordDialogOpen(true)}
                        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                      >
                        Đặt lại mật khẩu
                      </Button>
                      <Button
                        variant="outlined"
                        color="error"
                        size="medium"
                        startIcon={<PersonOffOutlinedIcon />}
                        onClick={() => setDeactivateDialogOpen(true)}
                        sx={{ fontWeight: 600, borderRadius: 2, textTransform: "none", px: 2.5 }}
                      >
                        Vô hiệu hoá tài khoản
                      </Button>
                    </>
                  )}
                </Box>
              )}
            </Box>

            <Divider sx={{ my: 2.5 }} />

            <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.secondary", textTransform: "uppercase", letterSpacing: 0.8, mb: 2 }}>
              Thông tin cá nhân
            </Typography>

            <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 2 }}>
              <Card variant="outlined" sx={{ borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.015)" }}>
                <CardContent sx={{ p: 2, "&:last-child": { pb: 2 } }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                    <EmailOutlinedIcon color="primary" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                        Email
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {profile.email || "Chưa cập nhật"}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>

              <Card variant="outlined" sx={{ borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.015)" }}>
                <CardContent sx={{ p: 2, "&:last-child": { pb: 2 } }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                    <LocationOnOutlinedIcon color="primary" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                        Thành phố / Tỉnh
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {profile.city || "Chưa cập nhật"}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>

              {profile.dob && (
                <Card variant="outlined" sx={{ borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.015)" }}>
                  <CardContent sx={{ p: 2, "&:last-child": { pb: 2 } }}>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                      <CakeOutlinedIcon color="primary" />
                      <Box>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                          Ngày sinh
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {profile.dob}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              )}

              <Card variant="outlined" sx={{ borderRadius: 2.5, bgcolor: "rgba(0,0,0,0.015)" }}>
                <CardContent sx={{ p: 2, "&:last-child": { pb: 2 } }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                    <BadgeOutlinedIcon color="primary" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                        Mã người dùng (User ID)
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {profile.userId}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Box>
          </Box>
        </Paper>

        <UserShelfSection userId={profile.userId} />

        <ReportDialog open={reportUserOpen} onClose={() => setReportUserOpen(false)} targetType="USER" targetId={profile.userId} targetLabel={`Người dùng @${profile.username}`} />
        <LockUserDialog open={lockDialogOpen} targetUserId={profile.userId} targetUsername={profile.username} onClose={() => setLockDialogOpen(false)} />
        <ManagePermissionsDialog open={permissionsDialogOpen} targetUserId={profile.userId} targetUsername={profile.username} onClose={() => setPermissionsDialogOpen(false)} />
        <ResetPasswordDialog open={resetPasswordDialogOpen} targetUserId={profile.userId} targetUsername={profile.username} onClose={() => setResetPasswordDialogOpen(false)} />
        <DeactivateAccountDialog open={deactivateDialogOpen} targetUserId={profile.userId} targetUsername={profile.username} onClose={() => setDeactivateDialogOpen(false)} />
      </Container>
    </Scene>
  );
}
