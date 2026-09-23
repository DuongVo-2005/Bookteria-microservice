package com.devteria.friend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.devteria.friend.dto.FriendRequestStatus;
import com.devteria.friend.entity.FriendRequest;

public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
    Optional<FriendRequest> findBySenderIdAndReceiverId(String senderId, String receiverId);

    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(
            String senderId, String receiverId, FriendRequestStatus status);

    List<FriendRequest> findByReceiverIdAndStatus(String receiverId, FriendRequestStatus status);

    List<FriendRequest> findBySenderIdAndStatus(String senderId, FriendRequestStatus status);
}
