package com.ekruminis.txanalytics.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestTopicConfig {

    @Bean
    public NewTopic ingestCommandsTopic() {
        return new NewTopic("ingest.commands", 1, (short) 1);
    }
}
