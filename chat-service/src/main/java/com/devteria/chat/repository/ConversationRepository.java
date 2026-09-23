package com.devteria.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.devteria.chat.entity.Conversation;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByParticipantsHash(String hash);

    @Query("{'participants.userId' : ?0}")
    List<Conversation> findAllByParticipantIdsContains(String userId);

    @Query("{'participants.userId': ?0, 'hiddenBy': {'$ne': ?0}}")
    Page<Conversation> findAllByParticipantIdsContainsAndNotHidden(String userId, Pageable pageable);

    // Message Request đang chờ MÌNH phản hồi: mình là participant, trạng thái
    // PENDING, và mình không phải người đã tạo request đó (loại request mình
    // tự gửi ra khỏi kết quả).
    @Query("{'participants.userId': ?0, 'status': 'PENDING', 'requestedBy': {'$ne': ?0}}")
    List<Conversation> findPendingRequestsReceivedBy(String userId);
}
