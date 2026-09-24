import React, { useState } from "react";
import { Card, CardActionArea, CardContent, Typography, Box, Rating, Chip } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { PlaceholderCover } from "./PlaceholderCover";

export const SearchResultCard = ({ book }) => {
  const navigate = useNavigate();
  const [imgError, setImgError] = useState(false);

  const authorsText =
    book.authorNames && book.authorNames.length > 0 ? book.authorNames.join(", ") : "Tác giả chưa cập nhật";

  const handleClick = () => {
    navigate(`/books/${book.id}`);
  };

  return (
    <Card
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        borderRadius: 2,
        overflow: "hidden",
        border: "1px solid rgba(0, 0, 0, 0.08)",
        boxShadow: "0 1px 4px rgba(0, 0, 0, 0.04)",
        transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 8px 20px rgba(0, 0, 0, 0.12)",
        },
      }}
    >
      <CardActionArea
        onClick={handleClick}
        sx={{
          flexGrow: 1,
          display: "flex",
          flexDirection: "column",
          alignItems: "stretch",
          justifyContent: "flex-start",
          height: "100%",
        }}
      >
        <Box sx={{ width: "100%", paddingTop: "150%", position: "relative", bgcolor: "#f5f5f5" }}>
          {book.coverImage && !imgError ? (
            <Box
              component="img"
              src={book.coverImage}
              alt={book.title}
              onError={() => setImgError(true)}
              sx={{
                position: "absolute",
                top: 0,
                left: 0,
                width: "100%",
                height: "100%",
                objectFit: "cover",
              }}
              loading="lazy"
            />
          ) : (
            <Box sx={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%" }}>
              <PlaceholderCover title={book.title} />
            </Box>
          )}
        </Box>

        <CardContent
          sx={{
            p: 1.5,
            flexGrow: 1,
            display: "flex",
            flexDirection: "column",
            width: "100%",
            boxSizing: "border-box",
          }}
        >
          {book.categoryNames && book.categoryNames.length > 0 && (
            <Box sx={{ mb: 0.8, display: "flex", flexWrap: "wrap", gap: 0.5 }}>
              {book.categoryNames.slice(0, 2).map((catName) => (
                <Chip
                  key={catName}
                  label={catName}
                  size="small"
                  sx={{
                    height: 20,
                    fontSize: "0.68rem",
                    bgcolor: "action.hover",
                    color: "text.secondary",
                    pointerEvents: "none",
                  }}
                />
              ))}
            </Box>
          )}

          <Typography
            variant="subtitle2"
            sx={{
              fontWeight: 600,
              lineHeight: 1.3,
              minHeight: "2.6em",
              maxHeight: "2.6em",
              overflow: "hidden",
              textOverflow: "ellipsis",
              display: "-webkit-box",
              WebkitLineClamp: 2,
              WebkitBoxOrient: "vertical",
              color: "text.primary",
              mb: 0.5,
            }}
            title={book.title}
          >
            {book.title}
          </Typography>

          <Typography
            variant="caption"
            color="text.secondary"
            sx={{
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              display: "block",
              mb: 1,
            }}
            title={authorsText}
          >
            {authorsText}
          </Typography>

          <Box sx={{ mt: "auto", display: "flex", alignItems: "center", flexWrap: "wrap", gap: 0.5 }}>
            <Rating value={book.ratingAverage} precision={0.5} size="small" readOnly sx={{ fontSize: "0.95rem" }} />
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 500 }}>
              ({book.ratingCount})
            </Typography>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
};

export default SearchResultCard;
