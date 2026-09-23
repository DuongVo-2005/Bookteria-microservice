package com.devteria.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.devteria.post.dto.ApiResponse;
import com.devteria.post.dto.PostVisibility;
import com.devteria.post.entity.Post;
import com.devteria.post.exception.AppException;
import com.devteria.post.exception.ErrorCode;
import com.devteria.post.mapper.PostMapper;
import com.devteria.post.repository.PostCommentRepository;
import com.devteria.post.repository.PostLikeRepository;
import com.devteria.post.repository.PostRepository;
import com.devteria.post.repository.httpclient.BookClient;
import com.devteria.post.repository.httpclient.FriendClient;
import com.devteria.post.repository.httpclient.ProfileClient;

// Unit test thuần Mockito cho PostService (idea-spec Phase 3) — tập trung vào logic dễ vỡ
// nhất: visibility access control (canView qua getPost/toggleLike) và toggle like đếm đúng.
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final String ALICE = "alice-id";
    private static final String BOB = "bob-id";
    private static final String CHARLIE = "charlie-id";

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private DateTimeFormatter dateTimeFormatter;

    @Mock
    private ProfileClient profileClient;

    @Mock
    private FriendClient friendClient;

    @Mock
    private BookClient bookClient;

    @Mock
    private OutboxEventService outboxEventService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                postLikeRepository,
                postCommentRepository,
                postMapper,
                dateTimeFormatter,
                profileClient,
                friendClient,
                bookClient,
                outboxEventService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser(String userId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Post postWith(PostVisibility visibility, String ownerId) {
        return Post.builder()
                .id("post-1")
                .userId(ownerId)
                .content("hello")
                .visibility(visibility)
                .likesCount(0)
                .commentsCount(0)
                .createdDate(Instant.now())
                .build();
    }

    @Test
    void getPost_publicPost_viewableByAnyone() {
        mockCurrentUser(CHARLIE);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(postWith(PostVisibility.PUBLIC, ALICE)));
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        var response = postService.getPost("post-1");

        assertThat(response).isNotNull();
    }

    @Test
    void getPost_privatePost_deniedForNonOwner() {
        mockCurrentUser(CHARLIE);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(postWith(PostVisibility.PRIVATE, ALICE)));

        var exception = assertThrows(AppException.class, () -> postService.getPost("post-1"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED);
    }

    @Test
    void getPost_privatePost_allowedForOwner() {
        mockCurrentUser(ALICE);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(postWith(PostVisibility.PRIVATE, ALICE)));
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        var response = postService.getPost("post-1");

        assertThat(response).isNotNull();
    }

    @Test
    void getPost_friendsOnlyPost_allowedForFriend() {
        mockCurrentUser(BOB);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(postWith(PostVisibility.FRIENDS, ALICE)));
        when(friendClient.getFriendUserIds(ALICE))
                .thenReturn(
                        ApiResponse.<List<String>>builder().result(List.of(BOB)).build());
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        var response = postService.getPost("post-1");

        assertThat(response).isNotNull();
    }

    @Test
    void getPost_friendsOnlyPost_deniedForNonFriend() {
        mockCurrentUser(CHARLIE);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(postWith(PostVisibility.FRIENDS, ALICE)));
        when(friendClient.getFriendUserIds(ALICE))
                .thenReturn(
                        ApiResponse.<List<String>>builder().result(List.of(BOB)).build());

        var exception = assertThrows(AppException.class, () -> postService.getPost("post-1"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED);
    }

    @Test
    void toggleLike_notLikedYet_likesAndIncrementsCount() {
        mockCurrentUser(BOB);
        Post post = postWith(PostVisibility.PUBLIC, ALICE);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndUserId("post-1", BOB)).thenReturn(false);
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        postService.toggleLike("post-1");

        assertThat(post.getLikesCount()).isEqualTo(1);
        verify(postLikeRepository).save(any());
    }

    @Test
    void toggleLike_alreadyLiked_unlikesAndDecrementsCount() {
        mockCurrentUser(BOB);
        Post post = postWith(PostVisibility.PUBLIC, ALICE);
        post.setLikesCount(1);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndUserId("post-1", BOB)).thenReturn(true);
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        postService.toggleLike("post-1");

        assertThat(post.getLikesCount()).isEqualTo(0);
        verify(postLikeRepository).deleteByPostIdAndUserId("post-1", BOB);
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void toggleLike_likesCountNeverGoesNegative() {
        mockCurrentUser(BOB);
        Post post = postWith(PostVisibility.PUBLIC, ALICE);
        post.setLikesCount(0);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByPostIdAndUserId("post-1", BOB)).thenReturn(true);
        when(postMapper.toPostResponse(any()))
                .thenReturn(
                        com.devteria.post.dto.response.PostResponse.builder().build());
        when(dateTimeFormatter.format(any())).thenReturn("now");

        postService.toggleLike("post-1");

        assertThat(post.getLikesCount()).isEqualTo(0);
    }
}
