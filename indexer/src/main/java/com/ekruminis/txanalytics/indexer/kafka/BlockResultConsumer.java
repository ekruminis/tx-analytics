package com.ekruminis.txanalytics.indexer.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.mongo.BlockResultDoc;
import com.ekruminis.txanalytics.indexer.mongo.BlockResultRepository;
import com.ekruminis.txanalytics.wire.BlockResult;

@Component
public class BlockResultConsumer {

    private final BlockResultRepository repository;
    private final CacheInvalidationPublisher invalidation;

    public BlockResultConsumer(BlockResultRepository repository, CacheInvalidationPublisher invalidation) {
        this.repository = repository;
        this.invalidation = invalidation;
    }

    @KafkaListener(topics = "blocks.results", containerFactory = "blockResultListenerFactory")
    public void onBlockResult(BlockResult result) {
        repository.save(BlockResultDoc.from(result));
        invalidation.announce(result.runId());
    }
}
