import React, { useState, useEffect } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, Box, Typography, Chip, Button, CircularProgress, Stepper, Step, StepLabel, Avatar, List, ListItem } from "@mui/material";
import { getCategories, getAuthors } from "../../services/bookService";
import { setOnboardingPreferences, getActiveReaders } from "../../services/onboardingService";
import { getBookErrorMessage } from "../../services/errorMessages";
import { useToast } from "../../context/ToastContext";
import { FriendStatusButton } from "../friend/FriendStatusButton";

const STEPS = ["Chọn thể loại", "Chọn tác giả", "Gợi ý kết bạn"];
const MIN_SELECTED = 3;

// BA Backlog FEAT-01 (be-report.md, bổ sung 2026-09-19): Onboarding Wizard 3
// bước, làm tín hiệu Cold-Start cho recommendation. Bước 3 "gợi ý follow"
// không có API follow riêng — dùng lại cơ chế kết bạn đã có trong app.
export const OnboardingWizardDialog = ({ open, onClose, onCompleted }) => {
  const { showError } = useToast();
  const [step, setStep] = useState(0);

  const [categories, setCategories] = useState([]);
  const [authors, setAuthors] = useState([]);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
  const [selectedAuthorIds, setSelectedAuthorIds] = useState([]);
  const [loadingOptions, setLoadingOptions] = useState(true);

  const [activeReaders, setActiveReaders] = useState([]);
  const [readersLoading, setReadersLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setStep(0);
    setSelectedCategoryIds([]);
    setSelectedAuthorIds([]);
    setLoadingOptions(true);
    Promise.all([getCategories(), getAuthors()])
      .then(([catRes, authorRes]) => {
        setCategories(catRes.data.result?.data || catRes.data.result || []);
        setAuthors(authorRes.data.result?.data || authorRes.data.result || []);
      })
      .catch(() => {})
      .finally(() => setLoadingOptions(false));
  }, [open]);

  const toggleCategory = (id) => {
    setSelectedCategoryIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };
  const toggleAuthor = (id) => {
    setSelectedAuthorIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const handleNextFromStep1 = () => setStep(1);

  const handleNextFromStep2 = () => {
    setSubmitting(true);
    setOnboardingPreferences(selectedCategoryIds, selectedAuthorIds)
      .then(() => {
        setStep(2);
        setReadersLoading(true);
        return getActiveReaders(5)
          .then((res) => setActiveReaders(res?.data?.result || []))
          .catch(() => setActiveReaders([]))
          .finally(() => setReadersLoading(false));
      })
      .catch((err) => showError(getBookErrorMessage(err)))
      .finally(() => setSubmitting(false));
  };

  const handleFinish = () => {
    onCompleted?.();
    onClose();
  };

  return (
    <Dialog open={open} onClose={step === 2 ? handleFinish : undefined} fullWidth maxWidth="sm" slotProps={{ paper: { sx: { borderRadius: 3 } } }}>
      <DialogTitle sx={{ fontWeight: 700 }}>Hoàn thiện hồ sơ đọc sách</DialogTitle>
      <DialogContent>
        <Stepper activeStep={step} sx={{ mb: 3 }}>
          {STEPS.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {step === 0 && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Chọn ít nhất {MIN_SELECTED} thể loại bạn quan tâm ({selectedCategoryIds.length}/{MIN_SELECTED}).
            </Typography>
            {loadingOptions ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : (
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                {categories.map((c) => (
                  <Chip
                    key={c.id}
                    label={c.name}
                    clickable
                    color={selectedCategoryIds.includes(c.id) ? "primary" : "default"}
                    variant={selectedCategoryIds.includes(c.id) ? "filled" : "outlined"}
                    onClick={() => toggleCategory(c.id)}
                  />
                ))}
              </Box>
            )}
          </Box>
        )}

        {step === 1 && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Chọn ít nhất {MIN_SELECTED} tác giả yêu thích ({selectedAuthorIds.length}/{MIN_SELECTED}).
            </Typography>
            {loadingOptions ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : (
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                {authors.map((a) => (
                  <Chip
                    key={a.id}
                    label={a.name}
                    clickable
                    color={selectedAuthorIds.includes(a.id) ? "primary" : "default"}
                    variant={selectedAuthorIds.includes(a.id) ? "filled" : "outlined"}
                    onClick={() => toggleAuthor(a.id)}
                  />
                ))}
              </Box>
            )}
          </Box>
        )}

        {step === 2 && (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Kết bạn với những độc giả tích cực để theo dõi cảm nhận sách của họ:
            </Typography>
            {readersLoading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : activeReaders.length > 0 ? (
              <List>
                {activeReaders.map((r) => {
                  const userId = r.userId || r.id;
                  const username = r.username || r.name;
                  return (
                    <ListItem key={userId} sx={{ display: "flex", alignItems: "center", gap: 1.5, px: 0 }}>
                      <Avatar src={r.avatar || undefined} sx={{ width: 40, height: 40 }} />
                      <Typography variant="body2" sx={{ fontWeight: 600, flexGrow: 1 }}>
                        {username}
                      </Typography>
                      <FriendStatusButton otherUserId={userId} otherUsername={username} otherAvatar={r.avatar} size="small" />
                    </ListItem>
                  );
                })}
              </List>
            ) : (
              <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 2 }}>
                Chưa có gợi ý nào lúc này.
              </Typography>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5 }}>
        {step === 0 && (
          <Button variant="contained" onClick={handleNextFromStep1} disabled={selectedCategoryIds.length < MIN_SELECTED} sx={{ textTransform: "none", fontWeight: 700, ml: "auto" }}>
            Tiếp theo
          </Button>
        )}
        {step === 1 && (
          <>
            <Button onClick={() => setStep(0)} sx={{ textTransform: "none" }}>
              Quay lại
            </Button>
            <Button variant="contained" onClick={handleNextFromStep2} disabled={selectedAuthorIds.length < MIN_SELECTED || submitting} sx={{ textTransform: "none", fontWeight: 700 }}>
              {submitting ? <CircularProgress size={18} color="inherit" /> : "Tiếp theo"}
            </Button>
          </>
        )}
        {step === 2 && (
          <Button variant="contained" onClick={handleFinish} sx={{ textTransform: "none", fontWeight: 700, ml: "auto" }}>
            Hoàn tất
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default OnboardingWizardDialog;
