package com.devteria.book.configuration;

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
// hết thì đẩy sang topic "<topic>.DLT" thay vì crash/lặp mãi. Chỉ bắt lỗi tầng deserialize
// (ErrorHandlingDeserializer, xem application.yaml) — lỗi nghiệp vụ trong consume() vẫn tự
// try/catch + log như cũ, không đổi hành vi đã verify.
//
// KHÔNG khai KafkaTemplate cho DLT như 1 @Bean riêng (khác pattern chat-service/search-service
// — nơi đó không có bean KafkaTemplate nào khác nên không xung đột). book-service đã có sẵn
// KafkaTemplate<String, OutboxEventMessage> (OutboxPublisher, produce "book-events") — nếu thêm
// 1 bean KafkaTemplate nữa vào context, Spring không còn tự resolve rõ ràng cho OutboxPublisher
// được nữa (đã verify thật: kể cả gắn @Qualifier/@Resource theo tên bean cũng không cứu được,
// generic-type matching của Spring loại cả 2 candidate). Giữ KafkaTemplate cho DLT như 1 field
// nội bộ, tạo 1 lần lúc khởi tạo bean — không bao giờ là ứng viên autowire cho nơi khác.
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key thật của mọi message trong dự án là String — DeadLetterPublishingRecoverer truyền
        // thẳng key đã deserialize (String) cho template này, ByteArraySerializer trước đây ném
        // ClassCastException ngay khi có message lỗi cần dead-letter, khiến DLT không bao giờ
        // publish được và consumer kẹt vô hạn ở đúng offset đó. Value vẫn ByteArraySerializer vì
        // recoverer truyền nguyên byte[] gốc chưa parse được cho value.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        KafkaTemplate<String, byte[]> dltKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
