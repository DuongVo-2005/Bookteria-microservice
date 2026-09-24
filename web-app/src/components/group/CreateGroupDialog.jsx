import React, { useState } from "react";
import { Dialog, DialogTitle, DialogContent, DialogActions, IconButton, TextField, FormControl, FormLabel, RadioGroup, FormControlLabel, Radio, MenuItem, Button, Box, Typography, Avatar, CircularProgress } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import PublicIcon from "@mui/icons-material/Public";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import AddPhotoAlternateIcon from "@mui/icons-material/AddPhotoAlternate";
import { useNavigate } from "react-router-dom";
import { useGroup } from "../../context/GroupContext";

const CATEGORIES = ["Công nghệ & Lập trình", "Tâm lý & Kỹ năng sống", "Văn học & Tiểu thuyết", "Lịch sử & Triết học", "Kinh doanh & Khởi nghiệp", "Thiết kế & Nghệ thuật", "Khoa học & Vũ trụ"];

const PRESET_AVATARS = [
  "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=300&q=80",
  "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=300&q=80",
  "https://images.unsplash.com/photo-1461360370896-922624d12aa1?auto=format&fit=crop&w=300&q=80",
  "https://images.unsplash.com/photo-1474932430478-367dbb6832c1?auto=format&fit=crop&w=300&q=80",
  "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=300&q=80",
];

const PRESET_COVERS = [
  "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=1200&q=80",
];

export const CreateGroupDialog = ({ open, onClose }) => {
  const { createGroup } = useGroup();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [visibility, setVisibility] = useState("PUBLIC");
  const [selectedAvatar, setSelectedAvatar] = useState(PRESET_AVATARS[0]);
  const [selectedCover, setSelectedCover] = useState(PRESET_COVERS[0]);
  const [errorName, setErrorName] = useState(false);

  const handleClose = () => {
    setName("");
    setDescription("");
    setErrorName(false);
    onClose();
  };

  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorName(true);
      return;
    }

    setSubmitting(true);
    createGroup({
      name: name.trim(),
      description: description.trim() || "Cộng đồng giao lưu và thảo luận sách.",
      category,
      visibility,
      avatar: selectedAvatar,
      coverImage: selectedCover,
    })
      .then((created) => {
        handleClose();
        navigate(`/groups/${created.id}`);
      })
      .catch(() => {})
      .finally(() => setSubmitting(false));
  };

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm" slotProps={{ paper: { sx: { borderRadius: 3, maxHeight: "90vh" } } }}>
      <form onSubmit={handleSubmit}>
        <DialogTitle sx={{ fontWeight: 700, display: "flex", justifyContent: "space-between", alignItems: "center", pb: 1 }}>
          <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
            Tạo hội nhóm đọc sách mới
          </Typography>
          <IconButton onClick={handleClose} size="small" aria-label="Đóng">
            <CloseIcon />
          </IconButton>
        </DialogTitle>

        <DialogContent dividers sx={{ p: 3, display: "flex", flexDirection: "column", gap: 2.5 }}>
          <TextField
            label="Tên hội nhóm"
            required
            fullWidth
            placeholder="Ví dụ: CLB Sách Công Nghệ & Khởi Nghiệp"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (errorName) setErrorName(false);
            }}
            error={errorName}
            helperText={errorName ? "Vui lòng nhập tên hội nhóm" : ""}
          />

          <TextField select label="Thể loại / Chủ đề chính" fullWidth value={category} onChange={(e) => setCategory(e.target.value)}>
            {CATEGORIES.map((cat) => (
              <MenuItem key={cat} value={cat}>
                {cat}
              </MenuItem>
            ))}
          </TextField>

          <TextField label="Mô tả nhóm" multiline rows={3} fullWidth placeholder="Giới thiệu mục tiêu, đối tượng tham gia và định hướng sinh hoạt của nhóm…" value={description} onChange={(e) => setDescription(e.target.value)} />

          <FormControl component="fieldset">
            <FormLabel component="legend" sx={{ fontWeight: 600, fontSize: "0.9rem", mb: 1 }}>
              Quyền riêng tư
            </FormLabel>
            <RadioGroup value={visibility} onChange={(e) => setVisibility(e.target.value)}>
              <FormControlLabel
                value="PUBLIC"
                control={<Radio size="small" />}
                label={
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <PublicIcon fontSize="small" color="primary" />
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        Công khai (Public)
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Bất kỳ ai cũng có thể tìm thấy nhóm, xem bài đăng và tham gia tự do.
                      </Typography>
                    </Box>
                  </Box>
                }
                sx={{ mb: 1, alignItems: "flex-start" }}
              />
              <FormControlLabel
                value="PRIVATE"
                control={<Radio size="small" />}
                label={
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <LockOutlinedIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        Riêng tư (Private)
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Chỉ thành viên mới có thể xem các bài đăng thảo luận và tài liệu trong nhóm.
                      </Typography>
                    </Box>
                  </Box>
                }
                sx={{ alignItems: "flex-start" }}
              />
            </RadioGroup>
          </FormControl>

          <Box>
            <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
              <AddPhotoAlternateIcon fontSize="small" color="action" />
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                Chọn hình đại diện mẫu cho nhóm
              </Typography>
            </Box>
            <Box sx={{ display: "flex", gap: 1.5 }}>
              {PRESET_AVATARS.map((url, idx) => (
                <Avatar
                  key={idx}
                  src={url}
                  variant="rounded"
                  onClick={() => setSelectedAvatar(url)}
                  sx={{ width: 50, height: 50, cursor: "pointer", borderRadius: 2, border: selectedAvatar === url ? "3px solid #1976d2" : "2px solid transparent", boxShadow: selectedAvatar === url ? "0 0 8px rgba(25, 118, 210, 0.5)" : "none", transition: "all 0.2s" }}
                />
              ))}
            </Box>
          </Box>

          <Box>
            <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
              Chọn ảnh bìa nhóm
            </Typography>
            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 1 }}>
              {PRESET_COVERS.map((url, idx) => (
                <Box
                  key={idx}
                  component="img"
                  src={url}
                  alt={`Cover ${idx}`}
                  onClick={() => setSelectedCover(url)}
                  sx={{ width: "100%", height: 48, objectFit: "cover", borderRadius: 1.5, cursor: "pointer", border: selectedCover === url ? "3px solid #1976d2" : "2px solid transparent", boxShadow: selectedCover === url ? "0 0 8px rgba(25, 118, 210, 0.4)" : "none", transition: "all 0.2s" }}
                />
              ))}
            </Box>
          </Box>
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={handleClose} color="inherit" sx={{ fontWeight: 600 }}>
            Huỷ bỏ
          </Button>
          <Button type="submit" variant="contained" color="primary" disabled={submitting} sx={{ fontWeight: 700, px: 3, borderRadius: 2 }}>
            {submitting ? <CircularProgress size={20} color="inherit" /> : "Tạo nhóm"}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default CreateGroupDialog;
