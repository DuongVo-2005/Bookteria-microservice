package com.devteria.profile.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypherdsl.core.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.devteria.profile.dto.request.ProfileCreationRequest;
import com.devteria.profile.dto.request.SearchUserRequest;
import com.devteria.profile.dto.response.PageResponse;
import com.devteria.profile.dto.response.UserProfileResponse;
import com.devteria.profile.entity.UserProfile;
import com.devteria.profile.exception.AppException;
import com.devteria.profile.exception.ErrorCode;
import com.devteria.profile.mapper.UserProfileMapper;
import com.devteria.profile.query.UserProfileQuery;
import com.devteria.profile.repository.UserProfileRepository;
import com.devteria.profile.repository.httpclient.FileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileService {
    UserProfileRepository userProfileRepository;

    UserProfileMapper userProfileMapper;

    UserProfileQuery userProfileQuery;

    FileClient fileClient;

    RedisTemplate<String, UserProfileResponse> userProfileRedisTemplate;

    private static final String PROFILE_CACHE_PREFIX = "profile:userId:";
    private static final Duration PROFILE_CACHE_TTL = Duration.ofMinutes(30);

    public UserProfileResponse createProfile(ProfileCreationRequest request) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile = userProfileRepository.save(userProfile);

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfileResponse getProfile(String id) {
        UserProfile userProfile =
                userProfileRepository.findByUserId(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    // Batch lookup dùng Redis cache-aside — cho các service gọi nhiều userId 1 lúc
    // (vd. group-service liệt kê N thành viên/bình luận) tránh phải bắn N request riêng lẻ
    // hoặc N lần round-trip Neo4j khi cache miss.
    public List<UserProfileResponse> getByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueIds = new HashSet<>(userIds);
        Map<String, UserProfileResponse> result = new HashMap<>();
        Set<String> missedIds = new HashSet<>();

        for (String userId : uniqueIds) {
            UserProfileResponse cached = userProfileRedisTemplate.opsForValue().get(PROFILE_CACHE_PREFIX + userId);
            if (cached != null) {
                result.put(userId, cached);
            } else {
                missedIds.add(userId);
            }
        }

        if (!missedIds.isEmpty()) {
            List<UserProfile> fetched = userProfileRepository.findAllByUserIdIn(List.copyOf(missedIds));
            for (UserProfile profile : fetched) {
                UserProfileResponse response = userProfileMapper.toUserProfileResponse(profile);
                result.put(profile.getUserId(), response);
                userProfileRedisTemplate
                        .opsForValue()
                        .set(PROFILE_CACHE_PREFIX + profile.getUserId(), response, PROFILE_CACHE_TTL);
            }
        }

        return List.copyOf(result.values());
    }

    public UserProfileResponse getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserProfile userProfile = userProfileRepository
                .findByUserId(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfileResponse getByUserId(String userId) {
        String cacheKey = PROFILE_CACHE_PREFIX + userId;
        UserProfileResponse cached = userProfileRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        UserProfile userProfiles = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        UserProfileResponse response = userProfileMapper.toUserProfileResponse(userProfiles);

        userProfileRedisTemplate.opsForValue().set(cacheKey, response, PROFILE_CACHE_TTL);

        return response;
    }

    public void deleteProfile(String id) {
        UserProfile deleteProfile =
                userProfileRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userProfileRepository.delete(deleteProfile);
    }

    public Page<UserProfile> searchGet(Condition condition, Pageable pageable) {
        Page<UserProfile> page = userProfileRepository.findAll(condition, pageable);
        return page;
    }

    public PageResponse<UserProfileResponse> search(int size, int page, SearchUserRequest request) {

        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Sort sort = Sort.by("username").descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<UserProfile> userProfiles =
                userProfileRepository.findAllByUsernameContainingIgnoreCase(request.getKeyword(), pageable);

        List<UserProfileResponse> pageData = userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();

        return PageResponse.<UserProfileResponse>builder()
                .currentPage(page)
                .pageSize(userProfiles.getSize())
                .totalPages(userProfiles.getTotalPages())
                .totalElements(userProfiles.getTotalElements())
                .data(pageData)
                .build();
    }

    public UserProfileResponse updateMyProfile(ProfileCreationRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        UserProfile existingProfile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        existingProfile.setUsername(request.getUsername());
        existingProfile.setFirstName(request.getFirstName());
        existingProfile.setLastName(request.getLastName());
        existingProfile.setEmail(request.getEmail());
        existingProfile.setDob(request.getDob());
        existingProfile.setCity(request.getCity());

        UserProfile updatedProfile = userProfileRepository.save(existingProfile);
        userProfileRedisTemplate.delete(PROFILE_CACHE_PREFIX + userId);
        return userProfileMapper.toUserProfileResponse(updatedProfile);
    }

    // idea-spec BA v2 §2.2: "Soft Delete & Anonymize" — identity-service gọi khi Admin deactivate
    // 1 tài khoản. Mọi nơi khác (post/group/book review...) đều resolve tên tác giả qua
    // profile-service tại thời điểm đọc (không cache tên riêng ở service khác), nên chỉ cần
    // anonymize đúng 1 chỗ này là toàn hệ thống tự động hiển thị "Người dùng đã xoá" ở mọi nơi.
    public void anonymize(String userId) {
        UserProfile profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        profile.setFirstName("Người dùng");
        profile.setLastName("đã xoá");
        profile.setAvatar(null);
        profile.setDob(null);
        profile.setCity(null);
        userProfileRepository.save(profile);
        userProfileRedisTemplate.delete(PROFILE_CACHE_PREFIX + userId);
    }

    public UserProfileResponse updateAvatar(MultipartFile multipartFile) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Upload file
        var response = fileClient.uploadMedia(multipartFile);

        profile.setAvatar(response.getResult().getUrl());
        UserProfileResponse updated = userProfileMapper.toUserProfileResponse(userProfileRepository.save(profile));
        userProfileRedisTemplate.delete(PROFILE_CACHE_PREFIX + userId);
        return updated;
    }

    public UserProfileResponse getByUsername(String username) {
        UserProfile profile = userProfileRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userProfileMapper.toUserProfileResponse(profile);
    }
}
