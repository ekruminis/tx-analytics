package com.ekruminis.txanalytics.indexer.kafka;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationPublisher {

    private static final String TOPIC = "cache.invalidations";
    private static final long WINDOW_MS = 10_000;

    private final KafkaTemplate<String, String> kafka;
    private final Map<String, Long> lastAnnounced = new ConcurrentHashMap<>();

    public CacheInvalidationPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public void announce(String runId) {
        long now = System.currentTimeMillis();
        Long last = lastAnnounced.get(runId);
        if (last == null || now - last > WINDOW_MS) {
            lastAnnounced.put(runId, now);
            kafka.send(TOPIC, runId, runId);
        }
    }
}
