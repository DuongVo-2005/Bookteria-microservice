package com.devteria.file.controller;

import com.devteria.file.dto.ApiResponse;
import com.devteria.file.dto.response.FileData;
import com.devteria.file.dto.response.FileResponse;
import com.devteria.file.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {

    FileService fileService;

    @PostMapping("/media/upload")
    ApiResponse<FileResponse> uploadMedia(@RequestParam("file") MultipartFile file) throws IOException {

        return ApiResponse.<FileResponse>builder()
                .result(fileService.uploadFile(file))
                .build();
    }

    @GetMapping("/media/download/{fileName}")
    ResponseEntity<Resource> downloadMedia(@PathVariable String fileName) throws IOException {
        var fileData = fileService.downloadFile(fileName);
        return ResponseEntity.<Resource>ok()
                .header(HttpHeaders.CONTENT_TYPE, fileData.contentType())
                .body(fileData.resource());
    }

    // idea-spec BA GAP-05: gọi ngay sau khi post/profile đính kèm fileId trả về từ /media/upload
    // thành công - tránh bị FileCleanupJob dọn nhầm file đang thật sự được dùng.
    @PatchMapping("/media/{fileId}/confirm-attach")
    ApiResponse<Void> confirmAttach(@PathVariable String fileId) {
        fileService.confirmAttach(fileId);
        return ApiResponse.<Void>builder().message("File attached").build();
    }
}
