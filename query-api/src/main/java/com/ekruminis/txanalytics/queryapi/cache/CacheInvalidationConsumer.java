package com.ekruminis.txanalytics.queryapi.cache;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.queryapi.postgres.SimulationRunRepository;

@Component
public class CacheInvalidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationConsumer.class);

    private static final List<String> PER_RUN_CACHES = List.of("runSummary", "feeDistribution", "runMiners");

    private final CacheManager cacheManager;
    private final StringRedisTemplate redis;
    private final SimulationRunRepository runRepo;

    public CacheInvalidationConsumer(CacheManager cacheManager,
                                     StringRedisTemplate redis,
                                     SimulationRunRepository runRepo) {
        this.cacheManager = cacheManager;
        this.redis = redis;
        this.runRepo = runRepo;
    }

    @KafkaListener(topics = "cache.invalidations")
    public void onInvalidation(String runId) {
        for (String name : PER_RUN_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.evict(runId);
            }
        }

        deleteByPattern("runTimeseries::" + runId + "_*");

        deleteByPattern("comparison::*" + runId + "*");
        runRepo.findById(UUID.fromString(runId)).ifPresent(run ->
                deleteByPattern("comparison::" + run.getExperimentId() + "*"));

        log.info("evicted caches for run {}", runId);
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
