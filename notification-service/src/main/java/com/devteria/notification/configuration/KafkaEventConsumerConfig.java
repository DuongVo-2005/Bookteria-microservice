package com.devteria.notification.configuration;

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

import com.devteria.notification.dto.event.FriendEventMessage;

// Factory riêng cho listener "friend-events"/"group-events" — KHÔNG dùng factory mặc định
// (auto-config từ spring.kafka.consumer.* trong application.yaml, đang phục vụ listener
// "notification-delivery" với type NotificationEvent). spring.json.value.default.type là
// property toàn cục chỉ nhận 1 giá trị (đúng constraint đã ghi ở chat-service's
// GroupEventConsumer) — 2 factory khác type buộc phải khai riêng bằng code.
//
// ErrorHandlingDeserializer + DLT recoverer BẮT BUỘC phải có ngay từ Phase 1 (không đợi Phase 2
// "Notification DLT" như idea-spec dự định) — verify thật lúc test: consumer group mới
// ("notification-group") lần đầu đọc "friend-events" từ "earliest" gặp NGAY 1 record rác cũ
// (còn sót từ lúc test khác trong project, không phải do code này tạo ra) khiến
// JsonDeserializer ném SerializationException làm nghẽn partition vô hạn nếu không có
// ErrorHandlingDeserializer — đúng cảnh báo trong log Spring Kafka: "please consider
// configuring an ErrorHandlingDeserializer". Copy đúng pattern đã proven ở
// book-service's KafkaErrorHandlerConfig (DLT + FixedBackOff), không tạo pattern mới.
@Configuration
public class KafkaEventConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FriendEventMessage>
            eventEnvelopeKafkaListenerContainerFactory(
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
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FriendEventMessage.class.getName());

        ConsumerFactory<String, FriendEventMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

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

        ConcurrentKafkaListenerContainerFactory<String, FriendEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
