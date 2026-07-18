package com.ekruminis.txanalytics.indexer.kafka;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ekruminis.txanalytics.indexer.elastic.BlockTime;
import com.ekruminis.txanalytics.indexer.elastic.TxResultEsDoc;
import com.ekruminis.txanalytics.indexer.elastic.TxResultEsRepository;
import com.ekruminis.txanalytics.wire.TxResult;

@Component
public class TxResultEsConsumer {

    private final TxResultEsRepository repository;
    private final BlockTime blockTime;

    public TxResultEsConsumer(TxResultEsRepository repository, BlockTime blockTime) {
        this.repository = repository;
        this.blockTime = blockTime;
    }

    @KafkaListener(topics = "tx.results", containerFactory = "esTxResultListenerFactory")
    public void onTxResults(List<TxResult> results) {
        List<TxResultEsDoc> docs = results.stream()
                .map(r -> TxResultEsDoc.from(r, blockTime.at(r.height())))
                .toList();
        repository.saveAll(docs);
    }
}
