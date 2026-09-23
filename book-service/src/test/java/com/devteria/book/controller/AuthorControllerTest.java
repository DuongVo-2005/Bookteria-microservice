package com.devteria.book.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devteria.book.dto.request.AuthorUpdateRequest;
import com.devteria.book.entity.Author;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;

// idea-spec BA v2 P1-02 follow-up (Phase 1 report Known Issue #2/#3): trước đây updateAuthor()
// cho phép trùng tên, deleteAuthor() không kiểm tra sách đang tham chiếu.
@ExtendWith(MockitoExtension.class)
class AuthorControllerTest {

    private static final String AUTHOR_ID = "author-1";

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    private AuthorController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthorController(authorRepository, bookRepository);
    }

    @Test
    void updateAuthor_nameAlreadyUsedByAnotherAuthor_throws() {
        when(authorRepository.findById(AUTHOR_ID))
                .thenReturn(Optional.of(
                        Author.builder().id(AUTHOR_ID).name("Old Name").build()));
        when(authorRepository.findByNameIgnoreCase("Existing"))
                .thenReturn(Optional.of(
                        Author.builder().id("author-2").name("Existing").build()));

        assertThatThrownBy(() -> controller.updateAuthor(
                        AUTHOR_ID,
                        AuthorUpdateRequest.builder().name("Existing").build()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHOR_NAME_ALREADY_EXISTS);
    }

    @Test
    void updateAuthor_sameNameAsItself_isAllowed() {
        when(authorRepository.findById(AUTHOR_ID))
                .thenReturn(Optional.of(
                        Author.builder().id(AUTHOR_ID).name("Same Name").build()));
        when(authorRepository.findByNameIgnoreCase("Same Name"))
                .thenReturn(Optional.of(
                        Author.builder().id(AUTHOR_ID).name("Same Name").build()));
        when(authorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.updateAuthor(
                AUTHOR_ID, AuthorUpdateRequest.builder().name("Same Name").build());
        // không throw là đủ - giữ nguyên tên chính nó không bị coi là trùng.
    }

    @Test
    void deleteAuthor_stillReferencedByABook_throws() {
        when(authorRepository.existsById(AUTHOR_ID)).thenReturn(true);
        when(bookRepository.existsByAuthors_AuthorId(AUTHOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> controller.deleteAuthor(AUTHOR_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHOR_IN_USE);
    }
}
