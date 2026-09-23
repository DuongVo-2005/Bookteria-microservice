package com.devteria.file.repository;

import com.devteria.file.dto.FileInfo;
import com.devteria.file.entity.FileMgmt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Repository
public class FileInfoRepository {

    @Value("${app.file.storage-dir}")
    String storageDir;

    @Value("${app.file.download-prefix}")
    String urlPrefix;
    public FileInfo store(MultipartFile file) throws IOException {
        Path folder = Paths.get(storageDir);
        Files.createDirectories(folder);
        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName= Objects.isNull(fileExtension)
                ? UUID.randomUUID().toString()
                : UUID.randomUUID().toString() + "." + fileExtension;

        Path filePath=folder.resolve(fileName).normalize().toAbsolutePath();
        Files.copy(file.getInputStream(),filePath, StandardCopyOption.REPLACE_EXISTING);

        return FileInfo.builder()
                .name(fileName)
                .size(file.getSize())
                .contentType(file.getContentType())
                .md5Checksum(DigestUtils.md5DigestAsHex(file.getInputStream()))
                .path(filePath.toString())
                .url(urlPrefix + fileName)
                .build();
    }

    public Resource read(FileMgmt fileMgmt) throws IOException {
        var data = Files.readAllBytes(Path.of(fileMgmt.getPath()));

        return new ByteArrayResource(data);

    }

    // idea-spec BA GAP-05: xoá file vật lý trên đĩa khi dọn rác - best-effort, không ném exception
    // nếu file đã không còn tồn tại (đã bị xoá tay/lần dọn trước), chỉ log lại cho các lỗi khác
    // (permission, đang bị lock...) để job dọn rác không chết giữa chừng vì 1 file lỗi.
    public boolean delete(FileMgmt fileMgmt) {
        try {
            return Files.deleteIfExists(Path.of(fileMgmt.getPath()));
        } catch (IOException e) {
            return false;
        }
    }
}
