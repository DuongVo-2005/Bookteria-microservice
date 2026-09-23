package com.devteria.reading.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

// Chặn poison-pill message (JSON hỏng/sai shape) làm nghẽn consumer vô hạn: retry 2 lần cách 1s,
// hết thì đẩy sang topic "<topic>.DLT". Copy đúng pattern book-service (đã fix bug key-serializer
// hôm nay): key StringSerializer (key thật luôn là String trong dự án), value ByteArraySerializer
// (recoverer truyền nguyên byte[] gốc khi lỗi là do deserialize).
//
// KHÔNG khai KafkaTemplate cho DLT như 1 @Bean riêng — reading-service đã có sẵn
// KafkaTemplate<String, OutboxEventMessage> (OutboxPublisher, produce "reading-progress-events");
// thêm 1 bean KafkaTemplate nữa sẽ phá unqualified autowire của bean đó (đã gặp thật, xem
// book-service/be-report.md). Giữ làm biến local trong bean method.
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaTemplate<String, byte[]> dltKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
