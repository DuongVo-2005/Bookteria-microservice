package com.devteria.book.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.AuthorUpdateRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.AuthorResponse;
import com.devteria.book.entity.Author;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.AuthorRepository;
import com.devteria.book.repository.BookRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthorController {
    AuthorRepository authorRepository;
    BookRepository bookRepository;

    @GetMapping("/authors")
    public ApiResponse<List<AuthorResponse>> getAuthors() {
        var authors = authorRepository.findAll().stream().map(this::toResponse).toList();
        return ApiResponse.<List<AuthorResponse>>builder().result(authors).build();
    }

    // idea-spec BA v2 P1-02: author:manage — sửa master data (find-or-create hiện có chỉ tạo mới,
    // chưa có cách sửa tên/bio/avatar sau khi đã tạo).
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('author:manage')")
    @PutMapping("/authors/{authorId}")
    public ApiResponse<AuthorResponse> updateAuthor(
            @PathVariable String authorId, @RequestBody @Valid AuthorUpdateRequest request) {
        Author author =
                authorRepository.findById(authorId).orElseThrow(() -> new AppException(ErrorCode.AUTHOR_NOT_FOUND));
        authorRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(authorId)) {
                throw new AppException(ErrorCode.AUTHOR_NAME_ALREADY_EXISTS);
            }
        });
        author.setName(request.getName());
        author.setBio(request.getBio());
        author.setAvatarUrl(request.getAvatarUrl());
        author = authorRepository.save(author);
        return ApiResponse.<AuthorResponse>builder().result(toResponse(author)).build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('author:manage')")
    @DeleteMapping("/authors/{authorId}")
    public ApiResponse<Void> deleteAuthor(@PathVariable String authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new AppException(ErrorCode.AUTHOR_NOT_FOUND);
        }
        if (bookRepository.existsByAuthors_AuthorId(authorId)) {
            throw new AppException(ErrorCode.AUTHOR_IN_USE);
        }
        authorRepository.deleteById(authorId);
        return ApiResponse.<Void>builder().message("Author has been deleted").build();
    }

    private AuthorResponse toResponse(Author author) {
        return AuthorResponse.builder()
                .authorId(author.getId())
                .name(author.getName())
                .bio(author.getBio())
                .avatarUrl(author.getAvatarUrl())
                .build();
    }
}
