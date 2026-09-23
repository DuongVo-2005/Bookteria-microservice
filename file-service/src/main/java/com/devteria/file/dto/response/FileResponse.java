package com.devteria.file.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileResponse {
    // idea-spec BA GAP-05: caller (post-service/profile-service...) cần fileId (không suy ra được
    // từ url) để gọi confirm-attach sau khi đính kèm vào post/profile thành công.
    String fileId;
    String originalFileName;
    String url;
}
