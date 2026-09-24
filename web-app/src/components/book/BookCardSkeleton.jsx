import React from "react";
import { Card, CardContent, Skeleton, Box } from "@mui/material";

export const BookCardSkeleton = () => {
  return (
    <Card
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        borderRadius: 2,
        overflow: "hidden",
      }}
    >
      <Box sx={{ width: "100%", paddingTop: "150%", position: "relative" }}>
        <Skeleton
          variant="rectangular"
          animation="wave"
          sx={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            height: "100%",
          }}
        />
      </Box>
      <CardContent sx={{ p: 1.5, flexGrow: 1, display: "flex", flexDirection: "column" }}>
        <Skeleton variant="text" width="90%" height={20} animation="wave" />
        <Skeleton variant="text" width="60%" height={20} animation="wave" sx={{ mb: 1 }} />
        <Skeleton variant="text" width="50%" height={16} animation="wave" sx={{ mb: 1 }} />
        <Box sx={{ mt: "auto", display: "flex", alignItems: "center" }}>
          <Skeleton variant="rectangular" width={80} height={16} animation="wave" sx={{ borderRadius: 0.5, mr: 1 }} />
          <Skeleton variant="text" width={30} height={16} animation="wave" />
        </Box>
      </CardContent>
    </Card>
  );
};

export default BookCardSkeleton;
