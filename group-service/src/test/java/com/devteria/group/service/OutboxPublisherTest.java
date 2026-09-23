package com.devteria.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.devteria.group.dto.OutboxEventMessage;
import com.devteria.group.repository.OutboxEventRepository;

// idea-spec Phase 2 - "4.1 Outbox Cleanup Job": verify cleanupPublishedEvents() gọi đúng
// repository với ngưỡng ~7 ngày trước (không đợi cron thật chạy 1 lần/ngày để test).
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;

    @Test
    void cleanupPublishedEvents_deletesPublishedEventsOlderThanSevenDays() {
        OutboxPublisher outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);

        outboxPublisher.cleanupPublishedEvents();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(outboxEventRepository).deleteAllByPublishedTrueAndPublishedAtBefore(captor.capture());

        Instant threshold = captor.getValue();
        Instant expected = Instant.now().minus(7, ChronoUnit.DAYS);
        assertThat(threshold).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }
}
