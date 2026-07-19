package com.ekruminis.txanalytics.indexer.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheInvalidationTopicConfig {

    @Bean
    public NewTopic cacheInvalidationsTopic() {
        return new NewTopic("cache.invalidations", 1, (short) 1);
    }
}
