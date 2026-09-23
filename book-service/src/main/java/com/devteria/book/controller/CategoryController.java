package com.devteria.book.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.CategoryUpdateRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.CategoryResponse;
import com.devteria.book.entity.Category;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {
    CategoryRepository categoryRepository;
    BookRepository bookRepository;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        var categories =
                categoryRepository.findAll().stream().map(this::toResponse).toList();
        return ApiResponse.<List<CategoryResponse>>builder().result(categories).build();
    }

    // idea-spec BA v2 P1-02: category:manage
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('category:manage')")
    @PutMapping("/categories/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String categoryId, @RequestBody @Valid CategoryUpdateRequest request) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
            }
        });
        category.setName(request.getName());
        category = categoryRepository.save(category);
        return ApiResponse.<CategoryResponse>builder()
                .result(toResponse(category))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('category:manage')")
    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (bookRepository.existsByCategories_CategoryId(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryRepository.deleteById(categoryId);
        return ApiResponse.<Void>builder().message("Category has been deleted").build();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}
