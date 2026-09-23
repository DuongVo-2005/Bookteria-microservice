package com.devteria.book.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_VALUE(1009, "Uncategorized error", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND(1010, "Chat conversation not found ", HttpStatus.NOT_FOUND),
    BOOK_NOT_FOUND(1011, "Book not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1012, "Category not found", HttpStatus.NOT_FOUND),
    BOOK_TITLE_REQUIRED(1013, "Book title must not be blank", HttpStatus.BAD_REQUEST),
    BOOK_ISBN13_INVALID(1014, "ISBN-13 must be exactly 13 digits", HttpStatus.BAD_REQUEST),
    BOOK_AUTHORS_REQUIRED(1015, "Book must have at least {min} author", HttpStatus.BAD_REQUEST),
    BOOK_AUTHOR_ID_REQUIRED(1016, "Author id must not be blank", HttpStatus.BAD_REQUEST),
    BOOK_CATEGORY_REQUIRED(1017, "Book must have at least {min} category", HttpStatus.BAD_REQUEST),
    BOOK_CATEGORY_ID_REQUIRED(1018, "Category id must not be blank", HttpStatus.BAD_REQUEST),
    BOOK_PUBLISHER_REQUIRED(1019, "Book publisher is required", HttpStatus.BAD_REQUEST),
    BOOK_PUBLISHER_ID_REQUIRED(1020, "Publisher id must not be blank", HttpStatus.BAD_REQUEST),
    BOOK_PAGE_COUNT_INVALID(1021, "Page count must be a positive number", HttpStatus.BAD_REQUEST),
    BOOK_SEARCH_KEYWORD_REQUIRED(1022, "Search keyword must not be blank", HttpStatus.BAD_REQUEST),
    AUTHOR_NOT_FOUND(1023, "Author not found", HttpStatus.NOT_FOUND),
    PUBLISHER_NOT_FOUND(1024, "Publisher not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1025, "Book list must not be empty", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(1026, "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(1027, "User has already reviewed this book", HttpStatus.BAD_REQUEST),
    READING_LIST_NOT_FOUND(1028, "Reading not found", HttpStatus.NOT_FOUND),
    READING_LIST_ALREADY_EXISTS(1029, "Reading list is already exists", HttpStatus.BAD_REQUEST),
    READING_LIST_PAGE_INVALID(1030, "Reading list page invalid", HttpStatus.BAD_REQUEST),
    REVIEW_RATING_REQUIRED(1031, "Rating is required", HttpStatus.BAD_REQUEST),
    REVIEW_RATING_INVALID(1032, "Rating must be between 1 and 5", HttpStatus.BAD_REQUEST),
    BOOK_ISBN13_ALREADY_EXISTS(1033, "A book with this ISBN-13 already exists", HttpStatus.BAD_REQUEST),
    BOOK_SLUG_ALREADY_EXISTS(1034, "A book with this slug already exists", HttpStatus.BAD_REQUEST),
    GOOGLE_BOOK_NOT_FOUND(1035, "Google Books volume not found", HttpStatus.NOT_FOUND),
    GOOGLE_BOOKS_API_ERROR(1036, "Google Books API is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    READING_LIST_PROGRESS_PERCENT_INVALID(1037, "Progress percent must be between 0 and 100", HttpStatus.BAD_REQUEST),
    CANNOT_MERGE_BOOK_WITH_ITSELF(1038, "Cannot merge a book with itself", HttpStatus.BAD_REQUEST),
    BOOK_MERGE_TARGET_REQUIRED(1039, "duplicateBookId must not be blank", HttpStatus.BAD_REQUEST),
    ONBOARDING_CATEGORIES_MIN(1040, "Select at least {min} favorite categories", HttpStatus.BAD_REQUEST),
    ONBOARDING_AUTHORS_MIN(1041, "Select at least {min} favorite authors", HttpStatus.BAD_REQUEST),
    READING_CHALLENGE_YEAR_INVALID(1042, "Year must be a positive number", HttpStatus.BAD_REQUEST),
    READING_CHALLENGE_TARGET_INVALID(1043, "Target books must be at least 1", HttpStatus.BAD_REQUEST),
    REVIEWS_LOCKED_FOR_BOOK(1044, "Reviews are currently locked for this book", HttpStatus.FORBIDDEN),
    AUTHOR_NAME_REQUIRED(1045, "Author name must not be blank", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_REQUIRED(1046, "Category name must not be blank", HttpStatus.BAD_REQUEST),
    PUBLISHER_NAME_REQUIRED(1047, "Publisher name must not be blank", HttpStatus.BAD_REQUEST),
    SHELF_ACCESS_DENIED(1048, "This user's reading shelf is private", HttpStatus.FORBIDDEN),
    AUTHOR_NAME_ALREADY_EXISTS(1049, "An author with this name already exists", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_ALREADY_EXISTS(1050, "A category with this name already exists", HttpStatus.BAD_REQUEST),
    PUBLISHER_NAME_ALREADY_EXISTS(1051, "A publisher with this name already exists", HttpStatus.BAD_REQUEST),
    AUTHOR_IN_USE(1052, "Cannot delete an author that still has books", HttpStatus.BAD_REQUEST),
    CATEGORY_IN_USE(1053, "Cannot delete a category that still has books", HttpStatus.BAD_REQUEST),
    PUBLISHER_IN_USE(1054, "Cannot delete a publisher that still has books", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
