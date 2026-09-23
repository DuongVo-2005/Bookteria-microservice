package com.devteria.identity.configuration;

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

// Chặn poison-pill message làm nghẽn consumer vô hạn — copy đúng pattern đã proven (và đã fix bug
// key-serializer hôm nay) ở book/reading/group/post-service. Consumer Kafka ĐẦU TIÊN của
// identity-service (trước đó chỉ publish notification-delivery/user-events).
//
// KHÔNG khai KafkaTemplate cho DLT như 1 @Bean riêng — identity-service đã có 1 KafkaTemplate
// auto-config (UserService/OutboxPublisher đều tự resolve đúng bean đó qua các field generic khác
// nhau); thêm 1 bean KafkaTemplate tường minh sẽ tạo ứng viên thứ 2 gây ambiguous autowire.
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
