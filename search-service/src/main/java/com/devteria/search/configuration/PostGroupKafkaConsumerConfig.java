package com.devteria.search.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.devteria.search.dto.KafkaEventMessage;

// Factory riêng cho listener "post-events"/"group-events" - KHÔNG dùng factory mặc định (auto-
// config từ application.yaml đang phục vụ "book-events" với type KafkaBookEvent).
// spring.json.value.default.type là property toàn cục chỉ nhận 1 giá trị (đúng constraint đã
// documented nhiều lần trong project - notification-service/chat-service) - 2 factory khác type
// buộc phải khai riêng bằng code, không thể chỉ sửa application.yaml.
//
// DLT KafkaTemplate xây dựng làm local variable BÊN TRONG bean method, KHÔNG khai riêng thành
// @Bean thứ 2 - tránh đúng bẫy erasure đã gặp ở book-service (thêm KafkaTemplate bean thứ 2 phá
// unqualified injection của cái đầu, xem docs/be-next-initiatives-tasks.md). search-service's
// KafkaErrorHandlerConfig đã có 1 @Bean KafkaTemplate<Object,Object> riêng cho book-events rồi.
@Configuration
public class PostGroupKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KafkaEventMessage> postGroupKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, KafkaEventMessage.class.getName());

        ConsumerFactory<String, KafkaEventMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key thật của mọi message trong dự án là String — DeadLetterPublishingRecoverer truyền
        // thẳng key đã deserialize (String) cho template này, ByteArraySerializer trước đây ném
        // ClassCastException ngay khi có message lỗi cần dead-letter, khiến DLT không bao giờ
        // publish được và consumer kẹt vô hạn ở đúng offset đó. Value vẫn ByteArraySerializer vì
        // recoverer truyền nguyên byte[] gốc chưa parse được cho value.
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaTemplate<String, byte[]> dltKafkaTemplate =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));

        ConcurrentKafkaListenerContainerFactory<String, KafkaEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
