package com.devteria.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import com.devteria.profile.entity.UserProfile;
import com.devteria.profile.exception.AppException;
import com.devteria.profile.exception.ErrorCode;
import com.devteria.profile.mapper.UserProfileMapper;
import com.devteria.profile.query.UserProfileQuery;
import com.devteria.profile.repository.UserProfileRepository;
import com.devteria.profile.repository.httpclient.FileClient;

// Unit test thuần Mockito cho UserProfileService.anonymize() (BA v2 §2.2, code mới) -
// UserProfileService trước đó 0 test riêng (chỉ có ProfileServiceApplicationTests context-load).
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private UserProfileQuery userProfileQuery;

    @Mock
    private FileClient fileClient;

    @Mock
    private RedisTemplate<String, com.devteria.profile.dto.response.UserProfileResponse> userProfileRedisTemplate;

    private UserProfileService userProfileService;

    private UserProfileService newService() {
        return new UserProfileService(
                userProfileRepository, userProfileMapper, userProfileQuery, fileClient, userProfileRedisTemplate);
    }

    @Test
    void anonymize_profileNotFound_throws() {
        userProfileService = newService();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> userProfileService.anonymize(USER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    @Test
    void anonymize_clearsPersonalFieldsAndEvictsCache() {
        userProfileService = newService();
        UserProfile profile = UserProfile.builder()
                .userId(USER_ID)
                .firstName("Duong")
                .lastName("Vo")
                .avatar("http://avatar.jpg")
                .city("HCM")
                .build();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        userProfileService.anonymize(USER_ID);

        assertThat(profile.getFirstName()).isEqualTo("Người dùng");
        assertThat(profile.getLastName()).isEqualTo("đã xoá");
        assertThat(profile.getAvatar()).isNull();
        assertThat(profile.getCity()).isNull();
        assertThat(profile.getDob()).isNull();
        verify(userProfileRepository).save(profile);
        verify(userProfileRedisTemplate).delete("profile:userId:" + USER_ID);
    }
}
