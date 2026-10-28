package com.humberto.b2b.ordermanagement.notification.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "notification.publisher", havingValue = "kafka")
class KafkaConfiguration {

    @Bean
    NewTopic orderStatusChangedTopic(@Value("${notification.kafka.topic}") String topic,
                                     @Value("${notification.kafka.partitions}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }

    @Bean
    NewTopic orderStatusChangedDeadLetterTopic(@Value("${notification.kafka.topic}") String topic,
                                               @Value("${notification.kafka.partitions}") int partitions) {
        return TopicBuilder.name(topic + "-dlt").partitions(partitions).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
                                          @Value("${notification.kafka.topic}") String topic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(topic + "-dlt", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 3));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
