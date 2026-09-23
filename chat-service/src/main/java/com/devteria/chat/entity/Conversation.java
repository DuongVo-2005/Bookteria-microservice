package com.devteria.chat.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.devteria.chat.dto.ConversationStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversation")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Conversation {
    @MongoId
    String id;

    String type; // GROUP, DIRECT

    @Indexed(unique = true)
    String participantsHash;

    List<ParticipantInfo> participants;

    Instant createdDate;

    Instant modifiedDate;

    // NORMAL = chat bình thường (bạn bè). PENDING = tin nhắn chờ (người lạ vừa
    // nhắn lần đầu). REJECTED = đã từ chối, không nhắn lại được nữa.
    @Builder.Default
    ConversationStatus status = ConversationStatus.NORMAL;

    // userId của người đã tạo Message Request (chỉ có giá trị khi status khác
    // NORMAL) — dùng để biết ai là "sender" cần chờ Accept, ai là "receiver"
    // được quyền Accept/Reject.
    String requestedBy;

    String lastMessage;

    Instant lastMessageAt;

    // userId của các participant đã đọc tới thời điểm nào — dùng tính unreadCount.
    @Builder.Default
    Map<String, Instant> lastReadAt = new HashMap<>();

    // userId của các participant đã ẩn hội thoại này khỏi danh sách của họ.
    @Builder.Default
    Set<String> hiddenBy = new HashSet<>();
}
