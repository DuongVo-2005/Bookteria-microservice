import React, { useState, useEffect, useCallback } from "react";
import { Box, Typography, Button, CircularProgress } from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import LockOpenOutlinedIcon from "@mui/icons-material/LockOpenOutlined";
import { getReviews, createReview, updateReview, deleteReview } from "../../services/reviewService";
import { setReviewsLock } from "../../services/bookService";
import { getBookErrorMessage } from "../../services/errorMessages";
import { getUserProfile } from "../../services/userService";
import { getCurrentUserId, isAdmin, hasPermission } from "../../services/authenticationService";
import { useToast } from "../../context/ToastContext";
import { ReviewForm } from "./ReviewForm";
import { ReviewItem } from "./ReviewItem";
import { ErrorBanner } from "./ErrorBanner";
import { ReportDialog } from "../report/ReportDialog";

export const ReviewList = ({ book }) => {
  const { showSuccess, showError } = useToast();
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditingUserReview, setIsEditingUserReview] = useState(false);
  const [visibleCount, setVisibleCount] = useState(3);
  const [reportReviewTarget, setReportReviewTarget] = useState(null);
  const [reviewsLocked, setReviewsLocked] = useState(Boolean(book.reviewsLocked));
  const [lockToggling, setLockToggling] = useState(false);
  const canManageLock = isAdmin() || hasPermission("review:lock");

  const loadReviews = useCallback(() => {
    setLoading(true);
    setError(null);
    const currentUserId = getCurrentUserId();

    getReviews(book.id, 0, 10)
      .then((response) => {
        const data = response.data.result.data;
        // ReviewResponse (book-service) chỉ có userId, không có username —
        // tự resolve qua profile-service (giống pattern PublicProfile.jsx),
        // song song theo từng userId khác nhau (không lặp lại lookup trùng).
        const otherUserIds = [...new Set(data.map((r) => r.userId).filter((id) => id && id !== currentUserId))];
        return Promise.all(
          otherUserIds.map((id) =>
            getUserProfile(id)
              .then((res) => [id, res?.data?.result || null])
              .catch(() => [id, null])
          )
        ).then((entries) => {
          const profileMap = Object.fromEntries(entries);
          const list = data.map((review) => ({
            ...review,
            isCurrentUser: review.userId === currentUserId,
            username: profileMap[review.userId]?.username,
            avatar: profileMap[review.userId]?.avatar,
          }));
          setReviews(list);
        });
      })
      .catch((err) => {
        setError(getBookErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [book.id]);

  useEffect(() => {
    loadReviews();
  }, [loadReviews]);

  useEffect(() => {
    setReviewsLocked(Boolean(book.reviewsLocked));
  }, [book.id, book.reviewsLocked]);

  const currentUserReview = reviews.find((r) => r.isCurrentUser);
  const otherReviews = reviews.filter((r) => !r.isCurrentUser);

  const totalReviewsCount = book.stats?.reviewCount || 0;

  const handleAddReview = (rating, content, hasSpoiler) => {
    createReview(book.id, rating, content, hasSpoiler)
      .then(() => {
        showSuccess("Đã gửi đánh giá thành công");
        loadReviews();
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleUpdateReview = (rating, content, hasSpoiler) => {
    if (!currentUserReview) return;
    updateReview(currentUserReview.id, rating, content, hasSpoiler)
      .then(() => {
        showSuccess("Đã cập nhật đánh giá thành công");
        setIsEditingUserReview(false);
        loadReviews();
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleDeleteReview = (reviewId) => {
    // Phase 4 (be-report.md, 2026-09-18): platform ADMIN giờ cũng xoá được
    // đánh giá của người khác (trước đó chỉ tác giả) — dùng chung 1 API,
    // chỉ khác câu thông báo.
    const isOwn = reviewId === currentUserReview?.id;
    deleteReview(reviewId)
      .then(() => {
        showSuccess(isOwn ? "Đã xoá đánh giá của bạn" : "Đã xoá đánh giá (quyền Admin)");
        setIsEditingUserReview(false);
        loadReviews();
      })
      .catch((err) => showError(getBookErrorMessage(err)));
  };

  const handleLoadMore = () => {
    setVisibleCount((prev) => prev + 3);
  };

  const handleToggleLock = () => {
    setLockToggling(true);
    const nextLocked = !reviewsLocked;
    setReviewsLock(book.id, nextLocked)
      .then(() => {
        setReviewsLocked(nextLocked);
        showSuccess(nextLocked ? "Đã khoá đánh giá cho sách này." : "Đã mở khoá đánh giá.");
      })
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setLockToggling(false));
  };

  return (
    <Box sx={{ mt: 4 }}>
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 1, mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          Đánh giá ({totalReviewsCount})
        </Typography>

        {canManageLock && (
          <Button
            variant="outlined"
            size="small"
            color={reviewsLocked ? "success" : "warning"}
            startIcon={lockToggling ? <CircularProgress size={14} color="inherit" /> : reviewsLocked ? <LockOpenOutlinedIcon /> : <LockOutlinedIcon />}
            onClick={handleToggleLock}
            disabled={lockToggling}
            sx={{ textTransform: "none", fontWeight: 600 }}
          >
            {reviewsLocked ? "Mở khoá đánh giá" : "Khoá đánh giá"}
          </Button>
        )}
      </Box>

      {error && <ErrorBanner message={error} onRetry={loadReviews} />}

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
          <CircularProgress size={28} />
        </Box>
      ) : (
        <>
          {!currentUserReview ? (
            reviewsLocked ? (
              <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: "center", fontStyle: "italic" }}>
                Đánh giá cho sách này đang tạm khoá.
              </Typography>
            ) : (
              <ReviewForm onSubmit={handleAddReview} />
            )
          ) : isEditingUserReview ? (
            <ReviewForm
              initialRating={currentUserReview.rating}
              initialContent={currentUserReview.content}
              initialHasSpoiler={currentUserReview.hasSpoiler}
              isEditing={true}
              onSubmit={handleUpdateReview}
              onCancel={() => setIsEditingUserReview(false)}
            />
          ) : (
            <Box sx={{ mb: 3 }}>
              <ReviewItem
                review={currentUserReview}
                onEdit={() => setIsEditingUserReview(true)}
                onDelete={handleDeleteReview}
              />
            </Box>
          )}

          {otherReviews.length > 0 ? (
            <Box sx={{ mt: 2 }}>
              {otherReviews.slice(0, visibleCount).map((review) => (
                <ReviewItem
                  key={review.id}
                  review={review}
                  onDelete={isAdmin() ? handleDeleteReview : undefined}
                  onReport={(r) => setReportReviewTarget(r)}
                />
              ))}

              {visibleCount < otherReviews.length && (
                <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
                  <Button variant="outlined" color="primary" endIcon={<ExpandMoreIcon />} onClick={handleLoadMore}>
                    Xem thêm đánh giá
                  </Button>
                </Box>
              )}
            </Box>
          ) : (
            !currentUserReview &&
            !reviewsLocked && (
              <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: "center" }}>
                Chưa có đánh giá nào cho cuốn sách này. Hãy là người đầu tiên chia sẻ cảm nhận!
              </Typography>
            )
          )}
        </>
      )}

      <ReportDialog
        open={Boolean(reportReviewTarget)}
        onClose={() => setReportReviewTarget(null)}
        targetType="REVIEW"
        targetId={reportReviewTarget?.id}
        targetLabel={reportReviewTarget ? `Đánh giá của ${reportReviewTarget.username || "người dùng này"}` : undefined}
      />
    </Box>
  );
};

export default ReviewList;
