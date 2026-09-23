package com.devteria.file.service;

import com.devteria.file.dto.response.FileData;
import com.devteria.file.dto.response.FileResponse;
import com.devteria.file.exception.AppException;
import com.devteria.file.exception.ErrorCode;
import com.devteria.file.mapper.FileMgmtMapper;
import com.devteria.file.repository.FileInfoRepository;
import com.devteria.file.repository.FileMgmtRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileService {
    // idea-spec Phase 7 - "26.2 ... File upload security": trước đây không có validate gì cả
    // (bất kỳ loại file/kích thước nào cũng upload được, chỉ giới hạn ngầm bởi Spring's default
    // multipart size). Toàn bộ use case media hiện tại trong project (post/group/profile image)
    // chỉ cần ảnh - whitelist theo đúng nhu cầu thật, không đoán rộng ra.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    // idea-spec BA GAP-05: file chưa được confirm-attach quá thời hạn này coi là rác.
    private static final long UNATTACHED_TTL_HOURS = 24;

    FileInfoRepository fileInfoRepository;
    FileMgmtRepository fileMgmtRepository;
    FileMgmtMapper fileMgmtMapper;;

    public FileResponse uploadFile(MultipartFile file) throws IOException {
        validateFile(file);

        var fileInfo=fileInfoRepository.store(file);

        var fileMgmt=fileMgmtMapper.toFileMgmt(fileInfo);

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        fileMgmt.setOwnerId(userId);
        fileMgmt.setAttached(false);
        fileMgmt.setCreatedAt(Instant.now());
        fileMgmt = fileMgmtRepository.save(fileMgmt);

        return FileResponse.builder()
                .fileId(fileMgmt.getId())
                .originalFileName(file.getOriginalFilename())
                .url(fileInfo.getUrl())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    public FileData downloadFile(String fileName) throws IOException {
        var fileMgmt = fileMgmtRepository.findById(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        var resource = fileInfoRepository.read(fileMgmt);

        return new FileData(fileMgmt.getContentType(),resource);
    }

    // idea-spec BA GAP-05: gọi bởi chính user đã upload, ngay sau khi post/profile đính kèm file
    // thành công (FE gọi, hoặc BE-to-BE nếu post-service/profile-service tự confirm hộ). Idempotent
    // - confirm lại file đã attached=true không lỗi, không phải trường hợp cần chặn.
    public void confirmAttach(String fileId) {
        var fileMgmt = fileMgmtRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!userId.equals(fileMgmt.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!fileMgmt.isAttached()) {
            fileMgmt.setAttached(true);
            fileMgmtRepository.save(fileMgmt);
        }
    }

    // idea-spec BA GAP-05: Storage Garbage Collection - quét mỗi giờ, xoá file chưa confirm-attach
    // quá 24h (user bỏ dở form, không submit). Best-effort: 1 file lỗi xoá đĩa không chặn các file
    // còn lại trong cùng batch.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupOrphanFiles() {
        Instant threshold = Instant.now().minus(UNATTACHED_TTL_HOURS, ChronoUnit.HOURS);
        List<com.devteria.file.entity.FileMgmt> orphans =
                fileMgmtRepository.findAllByAttachedFalseAndCreatedAtBefore(threshold);

        for (var orphan : orphans) {
            boolean deletedFromDisk = fileInfoRepository.delete(orphan);
            if (!deletedFromDisk) {
                log.warn("Could not delete orphan file from disk, removing DB record anyway. fileId={}, path={}",
                        orphan.getId(), orphan.getPath());
            }
            fileMgmtRepository.delete(orphan);
        }

        if (!orphans.isEmpty()) {
            log.info("Cleaned up {} orphan file(s) unattached for more than {}h.", orphans.size(), UNATTACHED_TTL_HOURS);
        }
    }
}