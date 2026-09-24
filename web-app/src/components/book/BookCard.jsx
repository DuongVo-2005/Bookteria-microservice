import React, { useState } from "react";
import { Card, CardActionArea, CardContent, Typography, Box, Rating } from "@mui/material";
import { useNavigate } from "react-router-dom";
import { PlaceholderCover } from "./PlaceholderCover";

export const BookCard = ({ book }) => {
  const navigate = useNavigate();
  const [imgError, setImgError] = useState(false);

  const mainAuthor =
    book.authors.find((a) => a.role === "MAIN_AUTHOR")?.name ||
    book.authors[0]?.name ||
    "Tác giả chưa cập nhật";

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
        transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 6px 16px rgba(0, 0, 0, 0.12)",
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
        <Box sx={{ width: "100%", paddingTop: "150%", position: "relative", bgcolor: "#f0f0f0" }}>
          {book.metadata?.coverImage && !imgError ? (
            <Box
              component="img"
              src={book.metadata.coverImage}
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

        <CardContent sx={{ p: 1.5, flexGrow: 1, display: "flex", flexDirection: "column", width: "100%", boxSizing: "border-box" }}>
          <Typography
            variant="subtitle2"
            sx={{
              fontWeight: 600,
              lineHeight: 1.3,
              height: "2.6em",
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
          >
            {mainAuthor}
          </Typography>

          <Box sx={{ mt: "auto", display: "flex", alignItems: "center", flexWrap: "wrap", gap: 0.5 }}>
            <Rating
              value={book.stats?.ratingAverage || 0}
              precision={0.5}
              size="small"
              readOnly
              sx={{ fontSize: "0.95rem" }}
            />
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 500 }}>
              ({book.stats?.reviewCount || 0})
            </Typography>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
};

export default BookCard;
