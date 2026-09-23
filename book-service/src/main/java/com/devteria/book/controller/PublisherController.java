package com.devteria.book.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.devteria.book.dto.request.PublisherUpdateRequest;
import com.devteria.book.dto.response.ApiResponse;
import com.devteria.book.dto.response.PublisherResponse;
import com.devteria.book.entity.Publisher;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.PublisherRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublisherController {
    PublisherRepository publisherRepository;
    BookRepository bookRepository;

    @GetMapping("/publishers")
    public ApiResponse<List<PublisherResponse>> getPublishers() {
        var publishers =
                publisherRepository.findAll().stream().map(this::toResponse).toList();
        return ApiResponse.<List<PublisherResponse>>builder().result(publishers).build();
    }

    // idea-spec BA v2 P1-02: publisher:manage
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('publisher:manage')")
    @PutMapping("/publishers/{publisherId}")
    public ApiResponse<PublisherResponse> updatePublisher(
            @PathVariable String publisherId, @RequestBody @Valid PublisherUpdateRequest request) {
        Publisher publisher = publisherRepository
                .findById(publisherId)
                .orElseThrow(() -> new AppException(ErrorCode.PUBLISHER_NOT_FOUND));
        publisherRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(publisherId)) {
                throw new AppException(ErrorCode.PUBLISHER_NAME_ALREADY_EXISTS);
            }
        });
        publisher.setName(request.getName());
        publisher = publisherRepository.save(publisher);
        return ApiResponse.<PublisherResponse>builder()
                .result(toResponse(publisher))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasAuthority('publisher:manage')")
    @DeleteMapping("/publishers/{publisherId}")
    public ApiResponse<Void> deletePublisher(@PathVariable String publisherId) {
        if (!publisherRepository.existsById(publisherId)) {
            throw new AppException(ErrorCode.PUBLISHER_NOT_FOUND);
        }
        if (bookRepository.existsByPublisher_PublisherId(publisherId)) {
            throw new AppException(ErrorCode.PUBLISHER_IN_USE);
        }
        publisherRepository.deleteById(publisherId);
        return ApiResponse.<Void>builder().message("Publisher has been deleted").build();
    }

    private PublisherResponse toResponse(Publisher publisher) {
        return PublisherResponse.builder()
                .publisherId(publisher.getId())
                .name(publisher.getName())
                .build();
    }
}
