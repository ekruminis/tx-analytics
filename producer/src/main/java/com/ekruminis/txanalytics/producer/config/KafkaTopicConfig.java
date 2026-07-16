package com.ekruminis.txanalytics.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    // 3 partitions, 1 replicator
    @Bean
    public NewTopic transactionsRawTopic() {
        return new NewTopic("transactions.raw", 3, (short) 1);
    }
}
