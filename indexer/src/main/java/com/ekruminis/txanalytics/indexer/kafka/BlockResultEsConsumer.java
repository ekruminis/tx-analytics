package com.ekruminis.txanalytics.indexer.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.elastic.BlockResultEsDoc;
import com.ekruminis.txanalytics.indexer.elastic.BlockResultEsRepository;
import com.ekruminis.txanalytics.indexer.elastic.BlockTime;
import com.ekruminis.txanalytics.wire.BlockResult;

@Component
public class BlockResultEsConsumer {

    private final BlockResultEsRepository repository;
    private final BlockTime blockTime;

    public BlockResultEsConsumer(BlockResultEsRepository repository, BlockTime blockTime) {
        this.repository = repository;
        this.blockTime = blockTime;
    }

    @KafkaListener(topics = "blocks.results", containerFactory = "esBlockResultListenerFactory")
    public void onBlockResult(BlockResult result) {
        repository.save(BlockResultEsDoc.from(result, blockTime.at(result.height())));
    }
}
