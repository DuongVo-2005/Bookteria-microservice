package com.devteria.search.configuration;

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
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public KafkaTemplate<Object, Object> dltKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key thật của mọi message trong dự án là String — DeadLetterPublishingRecoverer truyền
        // thẳng key đã deserialize (String) cho template này, ByteArraySerializer trước đây ném
        // ClassCastException ngay khi có message lỗi cần dead-letter, khiến DLT không bao giờ
        // publish được và consumer kẹt vô hạn ở đúng offset đó. Value vẫn ByteArraySerializer vì
        // recoverer truyền nguyên byte[] gốc chưa parse được cho value.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
