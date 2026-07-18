package com.ekruminis.txanalytics.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionsRawTopic(@Value("${producer.partitions}") int partitions) {
        return new NewTopic("transactions.raw", partitions, (short) 1);
    }
}
