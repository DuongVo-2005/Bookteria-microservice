import React, { useState } from "react";
import { Button, Menu, MenuItem, ListItemIcon, ListItemText, Divider } from "@mui/material";
import BookmarkAddIcon from "@mui/icons-material/BookmarkAdd";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import AutoStoriesIcon from "@mui/icons-material/AutoStories";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import DeleteIcon from "@mui/icons-material/Delete";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import { addToShelf, updateReadingProgress, removeFromShelf } from "../../services/readingListService";
import { getBookErrorMessage } from "../../services/errorMessages";
import { useToast } from "../../context/ToastContext";

const getStatusLabel = (status) => {
  switch (status) {
    case "WANT_TO_READ":
      return "Muốn đọc";
    case "READING":
      return "Đang đọc";
    case "COMPLETED":
      return "Đã đọc";
    default:
      return status;
  }
};

export const ShelfActionMenu = ({ book, shelfEntry, onShelfChange, fullWidth = true, size = "large" }) => {
  const { showSuccess, showError } = useToast();
  const currentStatus = shelfEntry?.status || null;

  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSelectStatus = async (status) => {
    handleClose();
    try {
      if (shelfEntry) {
        const response = await updateReadingProgress(shelfEntry.id, { status });
        onShelfChange(response.data.result);
      } else {
        const response = await addToShelf(book.id, status);
        onShelfChange(response.data.result);
      }
      showSuccess(`Đã chuyển trạng thái sang "${getStatusLabel(status)}"`);
    } catch (err) {
      showError(getBookErrorMessage(err));
    }
  };

  const handleRemove = async () => {
    handleClose();
    if (!shelfEntry) return;
    try {
      await removeFromShelf(shelfEntry.id);
      onShelfChange(null);
      showSuccess("Đã xoá khỏi kệ sách");
    } catch (err) {
      showError(getBookErrorMessage(err));
    }
  };

  return (
    <>
      {!currentStatus ? (
        <Button
          variant="contained"
          color="primary"
          size={size}
          fullWidth={fullWidth}
          startIcon={<BookmarkAddIcon />}
          endIcon={<ArrowDropDownIcon />}
          onClick={handleClick}
          sx={{ py: 1.4, fontWeight: 700 }}
        >
          + Thêm vào kệ sách
        </Button>
      ) : (
        <Button
          variant="outlined"
          color="primary"
          size={size}
          fullWidth={fullWidth}
          startIcon={<CheckCircleIcon />}
          endIcon={<ArrowDropDownIcon />}
          onClick={handleClick}
          sx={{ py: 1.4, fontWeight: 700, borderWidth: 2, "&:hover": { borderWidth: 2 } }}
        >
          {getStatusLabel(currentStatus)}
        </Button>
      )}

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        transformOrigin={{ horizontal: "left", vertical: "top" }}
        anchorOrigin={{ horizontal: "left", vertical: "bottom" }}
        slotProps={{
          paper: {
            sx: { minWidth: 200, borderRadius: 2, boxShadow: 4 },
          },
        }}
      >
        <MenuItem
          onClick={() => handleSelectStatus("WANT_TO_READ")}
          selected={currentStatus === "WANT_TO_READ"}
        >
          <ListItemIcon>
            <BookmarkBorderIcon fontSize="small" color={currentStatus === "WANT_TO_READ" ? "primary" : "inherit"} />
          </ListItemIcon>
          <ListItemText primary="Muốn đọc" />
        </MenuItem>

        <MenuItem
          onClick={() => handleSelectStatus("READING")}
          selected={currentStatus === "READING"}
        >
          <ListItemIcon>
            <AutoStoriesIcon fontSize="small" color={currentStatus === "READING" ? "primary" : "inherit"} />
          </ListItemIcon>
          <ListItemText primary="Đang đọc" />
        </MenuItem>

        <MenuItem
          onClick={() => handleSelectStatus("COMPLETED")}
          selected={currentStatus === "COMPLETED"}
        >
          <ListItemIcon>
            <CheckCircleIcon fontSize="small" color={currentStatus === "COMPLETED" ? "primary" : "inherit"} />
          </ListItemIcon>
          <ListItemText primary="Đã đọc" />
        </MenuItem>

        {currentStatus && [
          <Divider key="divider" sx={{ my: 0.5 }} />,
          <MenuItem key="remove" onClick={handleRemove} sx={{ color: "error.main" }}>
            <ListItemIcon>
              <DeleteIcon fontSize="small" color="error" />
            </ListItemIcon>
            <ListItemText primary="Xoá khỏi kệ sách" />
          </MenuItem>,
        ]}
      </Menu>
    </>
  );
};

export default ShelfActionMenu;
