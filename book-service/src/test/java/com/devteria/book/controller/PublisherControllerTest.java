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

import com.devteria.book.dto.request.PublisherUpdateRequest;
import com.devteria.book.entity.Publisher;
import com.devteria.book.exception.AppException;
import com.devteria.book.exception.ErrorCode;
import com.devteria.book.repository.BookRepository;
import com.devteria.book.repository.PublisherRepository;

@ExtendWith(MockitoExtension.class)
class PublisherControllerTest {

    private static final String PUBLISHER_ID = "publisher-1";

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private BookRepository bookRepository;

    private PublisherController controller;

    @BeforeEach
    void setUp() {
        controller = new PublisherController(publisherRepository, bookRepository);
    }

    @Test
    void updatePublisher_nameAlreadyUsedByAnotherPublisher_throws() {
        when(publisherRepository.findById(PUBLISHER_ID))
                .thenReturn(Optional.of(
                        Publisher.builder().id(PUBLISHER_ID).name("Old").build()));
        when(publisherRepository.findByNameIgnoreCase("Penguin"))
                .thenReturn(Optional.of(
                        Publisher.builder().id("publisher-2").name("Penguin").build()));

        assertThatThrownBy(() -> controller.updatePublisher(
                        PUBLISHER_ID,
                        PublisherUpdateRequest.builder().name("Penguin").build()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PUBLISHER_NAME_ALREADY_EXISTS);
    }

    @Test
    void updatePublisher_sameNameAsItself_isAllowed() {
        when(publisherRepository.findById(PUBLISHER_ID))
                .thenReturn(Optional.of(
                        Publisher.builder().id(PUBLISHER_ID).name("Penguin").build()));
        when(publisherRepository.findByNameIgnoreCase("Penguin"))
                .thenReturn(Optional.of(
                        Publisher.builder().id(PUBLISHER_ID).name("Penguin").build()));
        when(publisherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.updatePublisher(
                PUBLISHER_ID, PublisherUpdateRequest.builder().name("Penguin").build());
    }

    @Test
    void deletePublisher_stillReferencedByABook_throws() {
        when(publisherRepository.existsById(PUBLISHER_ID)).thenReturn(true);
        when(bookRepository.existsByPublisher_PublisherId(PUBLISHER_ID)).thenReturn(true);

        assertThatThrownBy(() -> controller.deletePublisher(PUBLISHER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PUBLISHER_IN_USE);
    }
}
