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

import com.devteria.book.dto.request.CategoryUpdateRequest;
import com.devteria.book.entity.Category;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private static final String CATEGORY_ID = "category-1";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookRepository bookRepository;

    private CategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoryController(categoryRepository, bookRepository);
    }

    @Test
    void updateCategory_nameAlreadyUsedByAnotherCategory_throws() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(
                        Category.builder().id(CATEGORY_ID).name("Old").build()));
        when(categoryRepository.findByNameIgnoreCase("Fiction"))
                .thenReturn(Optional.of(
                        Category.builder().id("category-2").name("Fiction").build()));

        assertThatThrownBy(() -> controller.updateCategory(
                        CATEGORY_ID,
                        CategoryUpdateRequest.builder().name("Fiction").build()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
    }

    @Test
    void updateCategory_sameNameAsItself_isAllowed() {
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(
                        Category.builder().id(CATEGORY_ID).name("Fiction").build()));
        when(categoryRepository.findByNameIgnoreCase("Fiction"))
                .thenReturn(Optional.of(
                        Category.builder().id(CATEGORY_ID).name("Fiction").build()));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.updateCategory(
                CATEGORY_ID, CategoryUpdateRequest.builder().name("Fiction").build());
    }

    @Test
    void deleteCategory_stillReferencedByABook_throws() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(bookRepository.existsByCategories_CategoryId(CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> controller.deleteCategory(CATEGORY_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_IN_USE);
    }
}
