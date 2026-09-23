package com.devteria.file.entity;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_mgmt")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMgmt {
    @MongoId
    String id;
    String contentType;
    long size;
    String path;
    String md5Checksum;
    String ownerId;

    // idea-spec BA GAP-05: Storage Garbage Collection. File mới upload luôn attached=false; caller
    // (post-service/profile-service...) gọi confirm-attach sau khi đính kèm thành công vào
    // post/profile. File còn attached=false quá 24h coi là rác (user bỏ dở, không submit form) -
    // FileCleanupJob tự dọn định kỳ.
    @Builder.Default
    boolean attached = false;

    Instant createdAt;
}
