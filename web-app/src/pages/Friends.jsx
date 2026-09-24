import React, { useState, useEffect } from "react";
import { Box, Card, Typography, Tabs, Tab, Button, Badge, Container } from "@mui/material";
import PeopleAltIcon from "@mui/icons-material/PeopleAlt";
import MarkEmailUnreadIcon from "@mui/icons-material/MarkEmailUnread";
import OutboxIcon from "@mui/icons-material/Outbox";
import BlockOutlinedIcon from "@mui/icons-material/BlockOutlined";
import PersonAddAlt1Icon from "@mui/icons-material/PersonAddAlt1";
import { useNavigate } from "react-router-dom";
import Scene from "./Scene";
import { isAuthenticated } from "../services/authenticationService";
import { useFriend } from "../context/FriendContext";
import { FriendCard } from "../components/friend/FriendCard";
import { FriendRequestItem } from "../components/friend/FriendRequestItem";
import { BlockedUserCard } from "../components/friend/BlockedUserCard";
import { SendFriendRequestDialog } from "../components/friend/SendFriendRequestDialog";
import { UnfriendConfirmDialog } from "../components/friend/UnfriendConfirmDialog";
import { BlockConfirmDialog } from "../components/friend/BlockConfirmDialog";
import { EmptyState } from "../components/book/EmptyState";

export default function Friends() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
    }
  }, [navigate]);

  const {
    friends,
    incomingRequests,
    outgoingRequests,
    blockedUsers,
    acceptFriendRequest,
    rejectFriendRequest,
    cancelFriendRequest,
    unfriend,
    blockUser,
    unblockUser,
  } = useFriend();

  const [currentTab, setCurrentTab] = useState("FRIENDS");
  const [sendRequestOpen, setSendRequestOpen] = useState(false);
  const [unfriendTarget, setUnfriendTarget] = useState(null);
  const [blockTarget, setBlockTarget] = useState(null);

  const handleTabChange = (_, newValue) => {
    setCurrentTab(newValue);
  };

  const handleOpenUnfriendDialog = (friend) => {
    setUnfriendTarget(friend);
  };

  const handleConfirmUnfriend = (friendUserId) => {
    unfriend(friendUserId);
  };

  const handleConfirmBlock = (userId) => {
    blockUser(userId);
  };

  const getEmptyContent = () => {
    switch (currentTab) {
      case "FRIENDS":
        return {
          title: "Chưa có bạn bè nào",
          description: "Danh sách bạn bè của bạn hiện đang trống. Hãy gửi lời mời kết bạn qua User ID!",
          actionText: "Gửi lời mời kết bạn",
          onAction: () => setSendRequestOpen(true),
        };
      case "INCOMING":
        return {
          title: "Không có lời mời kết bạn nào",
          description: "Khi có người khác gửi lời mời kết bạn đến bạn, yêu cầu sẽ xuất hiện tại đây.",
          actionText: "Gửi lời mời kết bạn",
          onAction: () => setSendRequestOpen(true),
        };
      case "OUTGOING":
        return {
          title: "Bạn chưa gửi lời mời kết bạn nào",
          description: "Các lời mời kết bạn bạn đã gửi đi và đang chờ phản hồi sẽ xuất hiện tại đây.",
          actionText: "Gửi lời mời ngay",
          onAction: () => setSendRequestOpen(true),
        };
      case "BLOCKED":
        return {
          title: "Bạn chưa chặn ai",
          description: "Danh sách những người dùng bạn đã chặn sẽ xuất hiện tại đây.",
          actionText: undefined,
          onAction: undefined,
        };
      default:
        return { title: "", description: "" };
    }
  };

  const emptyContent = getEmptyContent();

  return (
    <Scene>
      <Container maxWidth="lg" sx={{ px: { xs: 0, sm: 2 }, py: 1, width: "100%" }}>
      <Box sx={{ width: "100%", pb: 4 }}>
        <Box sx={{ mb: 3, display: "flex", flexDirection: { xs: "column", sm: "row" }, alignItems: { xs: "flex-start", sm: "center" }, justifyContent: "space-between", gap: 2 }}>
          <Box>
            <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5, fontSize: "1.05rem" }}>
              Quản lý danh sách bạn bè, lời mời đã nhận, lời mời đã gửi và danh sách người đã chặn
            </Typography>
          </Box>

          <Button
            variant="contained"
            color="primary"
            size="medium"
            startIcon={<PersonAddAlt1Icon />}
            onClick={() => setSendRequestOpen(true)}
            sx={{ fontWeight: 700, px: 2.5, py: 1.1, borderRadius: 2.5, textTransform: "none", flexShrink: 0 }}
          >
            + Gửi lời mời kết bạn
          </Button>
        </Box>

        <Card sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3, mb: 3 }}>
          <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
            <Tabs
              value={currentTab}
              onChange={handleTabChange}
              variant="scrollable"
              scrollButtons="auto"
              sx={{ "& .MuiTab-root": { textTransform: "none", fontWeight: 600, fontSize: "0.95rem", minHeight: 48 } }}
            >
              <Tab icon={<PeopleAltIcon fontSize="small" />} iconPosition="start" label={`Bạn bè (${friends.length})`} value="FRIENDS" />
              <Tab
                icon={
                  <Badge badgeContent={incomingRequests.length} color="error">
                    <MarkEmailUnreadIcon fontSize="small" />
                  </Badge>
                }
                iconPosition="start"
                label={`Lời mời đã nhận (${incomingRequests.length})`}
                value="INCOMING"
              />
              <Tab icon={<OutboxIcon fontSize="small" />} iconPosition="start" label={`Lời mời đã gửi (${outgoingRequests.length})`} value="OUTGOING" />
              <Tab icon={<BlockOutlinedIcon fontSize="small" />} iconPosition="start" label={`Người đã chặn (${blockedUsers.length})`} value="BLOCKED" />
            </Tabs>
          </Box>

          {currentTab === "FRIENDS" && (
            <>
              {friends.length > 0 ? (
                <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", md: "repeat(3, 1fr)" }, gap: 2.5 }}>
                  {friends.map((friend) => (
                    <FriendCard key={friend.userId} friend={friend} onOpenUnfriend={handleOpenUnfriendDialog} onOpenBlock={(user) => setBlockTarget(user)} />
                  ))}
                </Box>
              ) : (
                <EmptyState title={emptyContent.title} description={emptyContent.description} actionText={emptyContent.actionText} onAction={emptyContent.onAction} />
              )}
            </>
          )}

          {currentTab === "INCOMING" && (
            <>
              {incomingRequests.length > 0 ? (
                <Box>
                  {incomingRequests.map((req) => (
                    <FriendRequestItem key={req.id} request={req} isIncoming={true} onAccept={acceptFriendRequest} onReject={rejectFriendRequest} />
                  ))}
                </Box>
              ) : (
                <EmptyState title={emptyContent.title} description={emptyContent.description} actionText={emptyContent.actionText} onAction={emptyContent.onAction} />
              )}
            </>
          )}

          {currentTab === "OUTGOING" && (
            <>
              {outgoingRequests.length > 0 ? (
                <Box>
                  {outgoingRequests.map((req) => (
                    <FriendRequestItem key={req.id} request={req} isIncoming={false} onCancel={cancelFriendRequest} />
                  ))}
                </Box>
              ) : (
                <EmptyState title={emptyContent.title} description={emptyContent.description} actionText={emptyContent.actionText} onAction={emptyContent.onAction} />
              )}
            </>
          )}

          {currentTab === "BLOCKED" && (
            <>
              {blockedUsers.length > 0 ? (
                <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", md: "repeat(3, 1fr)" }, gap: 2.5 }}>
                  {blockedUsers.map((block) => (
                    <BlockedUserCard key={block.userId} block={block} onUnblock={unblockUser} />
                  ))}
                </Box>
              ) : (
                <EmptyState title={emptyContent.title} description={emptyContent.description} actionText={emptyContent.actionText} onAction={emptyContent.onAction} />
              )}
            </>
          )}
        </Card>

        <SendFriendRequestDialog open={sendRequestOpen} onClose={() => setSendRequestOpen(false)} />

        <UnfriendConfirmDialog open={Boolean(unfriendTarget)} friend={unfriendTarget} onClose={() => setUnfriendTarget(null)} onConfirm={handleConfirmUnfriend} />

        <BlockConfirmDialog open={Boolean(blockTarget)} user={blockTarget} onClose={() => setBlockTarget(null)} onConfirm={handleConfirmBlock} />
      </Box>
      </Container>
    </Scene>
  );
}
