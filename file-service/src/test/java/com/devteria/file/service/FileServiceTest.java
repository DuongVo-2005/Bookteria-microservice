package com.devteria.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.devteria.file.entity.FileMgmt;
import com.devteria.file.exception.AppException;
import com.devteria.file.exception.ErrorCode;
import com.devteria.file.mapper.FileMgmtMapper;
import com.devteria.file.repository.FileInfoRepository;
import com.devteria.file.repository.FileMgmtRepository;

// Unit test thuần Mockito cho FileService - chưa từng có test nào trong file-service trước đây.
// Tập trung vào confirmAttach()/cleanupOrphanFiles() (GAP-05 Storage Garbage Collection, code mới).
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final String USER_ID = "user-1";
    private static final String FILE_ID = "file-1.png";

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private FileMgmtRepository fileMgmtRepository;

    @Mock
    private FileMgmtMapper fileMgmtMapper;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileInfoRepository, fileMgmtRepository, fileMgmtMapper);

        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        // lenient: cleanupOrphanFiles() không chạm SecurityContext (chạy từ @Scheduled, không có
        // request/JWT nào cả), chỉ confirmAttach() cần.
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn(USER_ID);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private FileMgmt fileMgmt(boolean attached, String ownerId) {
        return FileMgmt.builder()
                .id(FILE_ID)
                .ownerId(ownerId)
                .attached(attached)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void confirmAttach_fileNotFound_throws() {
        when(fileMgmtRepository.findById(FILE_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> fileService.confirmAttach(FILE_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void confirmAttach_notOwner_throwsUnauthorized() {
        when(fileMgmtRepository.findById(FILE_ID)).thenReturn(Optional.of(fileMgmt(false, "another-user")));

        var exception = assertThrows(AppException.class, () -> fileService.confirmAttach(FILE_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(fileMgmtRepository, never()).save(any());
    }

    @Test
    void confirmAttach_owner_marksAttachedTrue() {
        FileMgmt mgmt = fileMgmt(false, USER_ID);
        when(fileMgmtRepository.findById(FILE_ID)).thenReturn(Optional.of(mgmt));

        fileService.confirmAttach(FILE_ID);

        assertThat(mgmt.isAttached()).isTrue();
        verify(fileMgmtRepository).save(mgmt);
    }

    @Test
    void confirmAttach_alreadyAttached_idempotentNoExtraSave() {
        FileMgmt mgmt = fileMgmt(true, USER_ID);
        when(fileMgmtRepository.findById(FILE_ID)).thenReturn(Optional.of(mgmt));

        fileService.confirmAttach(FILE_ID);

        verify(fileMgmtRepository, never()).save(any());
    }

    @Test
    void cleanupOrphanFiles_deletesDiskAndDbRecordForEachOrphan() {
        FileMgmt orphan1 = fileMgmt(false, USER_ID);
        FileMgmt orphan2 = FileMgmt.builder()
                .id("file-2.png")
                .ownerId(USER_ID)
                .attached(false)
                .createdAt(Instant.now())
                .build();
        when(fileMgmtRepository.findAllByAttachedFalseAndCreatedAtBefore(any()))
                .thenReturn(List.of(orphan1, orphan2));
        when(fileInfoRepository.delete(any())).thenReturn(true);

        fileService.cleanupOrphanFiles();

        verify(fileInfoRepository, times(2)).delete(any());
        verify(fileMgmtRepository).delete(orphan1);
        verify(fileMgmtRepository).delete(orphan2);
    }

    @Test
    void cleanupOrphanFiles_diskDeleteFails_stillRemovesDbRecord() {
        FileMgmt orphan = fileMgmt(false, USER_ID);
        when(fileMgmtRepository.findAllByAttachedFalseAndCreatedAtBefore(any())).thenReturn(List.of(orphan));
        when(fileInfoRepository.delete(orphan)).thenReturn(false);

        fileService.cleanupOrphanFiles();

        verify(fileMgmtRepository).delete(orphan);
    }

    @Test
    void cleanupOrphanFiles_noOrphans_doesNothing() {
        when(fileMgmtRepository.findAllByAttachedFalseAndCreatedAtBefore(any())).thenReturn(List.of());

        fileService.cleanupOrphanFiles();

        verify(fileMgmtRepository, never()).delete(any(FileMgmt.class));
        verify(fileInfoRepository, never()).delete(any());
    }
}
